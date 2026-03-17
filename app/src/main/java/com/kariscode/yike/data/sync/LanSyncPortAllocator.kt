package com.kariscode.yike.data.sync

import java.net.ServerSocket

/**
 * 端口选择抽成独立组件，是为了把“如何找到可用端口”从 HTTP 服务实现里分离出来，便于测试冲突与回退逻辑。
 */
class LanSyncPortAllocator {
    /**
     * 只在约定范围内扫描端口，是为了保证发现广播和排障文档都能围绕稳定端口段工作。
     */
    fun findAvailablePort(): Int {
        for (port in LanSyncConfig.PORT_RANGE_START..LanSyncConfig.PORT_RANGE_END) {
            if (isPortAvailable(port)) {
                return port
            }
        }
        error("未找到可用的局域网同步端口")
    }

    /**
     * 通过短暂占用 ServerSocket 检测可用性，是为了在真正启动 Ktor 服务前尽早发现显式端口冲突。
     */
    private fun isPortAvailable(port: Int): Boolean = runCatching {
        ServerSocket(port).use { socket ->
            socket.reuseAddress = true
        }
    }.isSuccess
}
