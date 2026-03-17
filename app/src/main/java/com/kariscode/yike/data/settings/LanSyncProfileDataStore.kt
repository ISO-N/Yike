package com.kariscode.yike.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 局域网同步配置与全局设置拆开存储，是为了避免设备身份、配对缓存和提醒主题等业务设置互相污染。
 */
val Context.lanSyncProfileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lan_sync_profile"
)
