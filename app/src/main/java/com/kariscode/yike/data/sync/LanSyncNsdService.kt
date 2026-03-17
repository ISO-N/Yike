package com.kariscode.yike.data.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * NSD 服务只负责把局域网里的候选地址发现出来，
 * 是为了让设备身份、协议能力和信任状态统一由后续 hello/配对流程决定。
 */
class LanSyncNsdService(
    context: Context
) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val _services = MutableStateFlow<List<DiscoveredLanService>>(emptyList())
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var localServiceName: String? = null

    /**
     * 发现结果以只读 StateFlow 暴露，是为了让上层能消费网络变化但不能绕过发现服务直接篡改缓存。
     */
    val services: StateFlow<List<DiscoveredLanService>> = _services.asStateFlow()

    /**
     * 本机广播名单独注册，是为了把“可被发现”和“主动发现别人”两个生命周期动作解耦。
     */
    fun registerService(serviceName: String, port: Int) {
        if (registrationListener != null) {
            return
        }
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            serviceType = LanSyncConfig.SERVICE_TYPE
            this.port = port
        }
        registrationListener = object : NsdManager.RegistrationListener {
            /**
             * 记录系统最终采用的广播名，是为了在发现回调里准确过滤掉自己，避免把本机当成远端设备。
             */
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                localServiceName = serviceInfo.serviceName
            }

            /**
             * 注册失败要写日志，是为了在端口冲突或系统拒绝时留下真实错误，而不是页面只看到抽象失败提示。
             */
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                localServiceName = serviceName
                LanSyncLogger.e("NSD register failed: $errorCode for ${serviceInfo.serviceName}")
            }

            /**
             * 注销成功后清空本机广播名，是为了让后续重新进入同步页时不会拿着过期名字错误过滤结果。
             */
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                localServiceName = null
            }

            /**
             * 注销失败虽然不影响 stop 调用继续返回，但仍要清空本地缓存，以免会话状态和系统状态进一步漂移。
             */
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                LanSyncLogger.e("NSD unregister failed: $errorCode for ${serviceInfo.serviceName}")
                localServiceName = null
            }
        }.also { listener ->
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    /**
     * 结束广播时统一收口注册监听，是为了让页面退出后不再继续向局域网暴露本机服务。
     */
    fun unregisterService() {
        registrationListener?.let { listener ->
            runCatching { nsdManager.unregisterService(listener) }
                .onFailure { throwable -> LanSyncLogger.e("NSD unregister exception", throwable) }
        }
        registrationListener = null
        localServiceName = null
    }

    /**
     * 发现流程开启时顺带申请 multicast lock，是为了提升局域网 Wi-Fi 场景下的服务可见性。
     */
    fun startDiscovery() {
        if (discoveryListener != null) {
            return
        }
        acquireMulticastLock()
        discoveryListener = object : NsdManager.DiscoveryListener {
            /**
             * 启动发现失败时记录日志并主动停掉当前会话资源，是为了避免 discoveryListener 留在半初始化状态。
             */
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                LanSyncLogger.e("NSD discovery start failed: $errorCode for $serviceType")
                stopDiscovery()
            }

            /**
             * 停止发现失败依然执行本地收口，是为了防止页面退出后继续保留过期的发现缓存。
             */
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                LanSyncLogger.e("NSD discovery stop failed: $errorCode for $serviceType")
                stopDiscovery()
            }

            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onDiscoveryStopped(serviceType: String) = Unit

            /**
             * 发现到候选服务后立即做地址解析，是为了让上层拿到的结果已经具备可连接的 host/port 信息。
             */
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != LanSyncConfig.SERVICE_TYPE) {
                    return
                }
                if (serviceInfo.serviceName == localServiceName) {
                    return
                }
                resolveService(serviceInfo)
            }

            /**
             * 服务丢失时按 serviceName 移除，是为了在对端退出同步页后尽快把设备列表收敛回真实可用状态。
             */
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _services.update { services ->
                    services.filterNot { service -> service.serviceName == serviceInfo.serviceName }
                }
            }
        }.also { listener ->
            nsdManager.discoverServices(LanSyncConfig.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    /**
     * 停止发现时同时释放 multicast lock 并清空缓存，是为了让同步页关闭后立刻回到完全离线的默认状态。
     */
    fun stopDiscovery() {
        discoveryListener?.let { listener ->
            runCatching { nsdManager.stopServiceDiscovery(listener) }
                .onFailure { throwable -> LanSyncLogger.e("NSD stop discovery exception", throwable) }
        }
        discoveryListener = null
        releaseMulticastLock()
        _services.value = emptyList()
    }

    /**
     * 解析成功后只保留连接所需最小字段，是为了让 NSD 层不承担协议能力和可信设备判断职责。
     */
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(
            serviceInfo,
            object : NsdManager.ResolveListener {
                /**
                 * 解析失败必须写日志，是为了保留设备不可见或地址异常时的底层原因。
                 */
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    LanSyncLogger.e("NSD resolve failed: $errorCode for ${serviceInfo.serviceName}")
                }

                /**
                 * 解析到 host/port 后以 serviceName 为键 upsert，可以避免局域网重复广播造成列表不断追加重复项。
                 */
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val hostAddress = serviceInfo.host?.hostAddress ?: return
                    upsertService(
                        DiscoveredLanService(
                            serviceName = serviceInfo.serviceName,
                            hostAddress = hostAddress,
                            port = serviceInfo.port
                        )
                    )
                }
            }
        )
    }

    /**
     * serviceName 作为发现层唯一键足够稳定，是为了把更高层的 deviceId/fingerprint 判断留给 hello 和配对流程。
     */
    private fun upsertService(service: DiscoveredLanService) {
        _services.update { services ->
            (services.filterNot { current -> current.serviceName == service.serviceName } + service)
                .sortedBy { current -> current.serviceName.lowercase() }
        }
    }

    /**
     * Multicast lock 统一维护在单点后，多次打开关闭同步页也不会遗留系统资源。
     */
    private fun acquireMulticastLock() {
        if (multicastLock != null) {
            return
        }
        multicastLock = wifiManager.createMulticastLock("yike-lan-sync").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    /**
     * 发现关闭即释放 lock，是为了避免同步页退出后仍持续占用 Wi-Fi 组播能力。
     */
    private fun releaseMulticastLock() {
        multicastLock?.takeIf { it.isHeld }?.release()
        multicastLock = null
    }

    /**
     * 发现层中间结果只保留 serviceName、host 和 port，
     * 是为了把 NSD 平台对象和上层协议对象彻底隔离开。
     */
    data class DiscoveredLanService(
        val serviceName: String,
        val hostAddress: String,
        val port: Int
    )
}
