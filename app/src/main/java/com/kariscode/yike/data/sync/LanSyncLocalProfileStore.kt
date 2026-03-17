package com.kariscode.yike.data.sync

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kariscode.yike.data.settings.lanSyncProfileDataStore
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 本机设备身份集中存储后，页面、发现广播和协议 hello 就能共享同一份显示名称，不会出现不同入口展示不同设备名。
 */
class LanSyncLocalProfileStore(
    private val context: Context,
    private val crypto: LanSyncCrypto
) {
    private val appContext = context.applicationContext
    private val deviceId: String = Settings.Secure.getString(
        appContext.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "unknown-device"

    /**
     * 读取本机档案时同时生成短 ID 和一次性配对码，是为了让同步页进入后可以立即展示当前身份与配对凭证。
     */
    suspend fun loadProfile(): com.kariscode.yike.domain.model.LanSyncLocalProfile {
        val displayName = appContext.lanSyncProfileDataStore.data.map { prefs ->
            prefs[Keys.displayName] ?: defaultDisplayName()
        }.first()
        return com.kariscode.yike.domain.model.LanSyncLocalProfile(
            deviceId = deviceId,
            displayName = displayName,
            shortDeviceId = deviceId.takeLast(6),
            pairingCode = crypto.createPairingCode()
        )
    }

    /**
     * 设备名更新持久化后，下次进入同步页仍能维持用户选择，而不是回退到系统型号。
     */
    suspend fun updateDisplayName(displayName: String) {
        appContext.lanSyncProfileDataStore.edit { prefs ->
            prefs[Keys.displayName] = displayName.trim().ifBlank { defaultDisplayName() }
        }
    }

    /**
     * 默认设备名仍沿用厂商和型号，是为了在用户未自定义前保持一个大多数场景下可识别的展示口径。
     */
    private fun defaultDisplayName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        return "$manufacturer ${Build.MODEL}"
    }

    private object Keys {
        val displayName = stringPreferencesKey("displayName")
    }
}
