package com.kariscode.yike.data.sync

import android.util.Log

/**
 * 同步日志统一包一层，是为了把面向开发的诊断细节与面向用户的错误文案分离，避免 UI 直接依赖 Log API。
 */
object LanSyncLogger {
    private const val TAG: String = "LanSyncV2"

    /**
     * 调试日志保留在同步模块内集中输出，是为了在发现、配对和传输串联时还能按同一 tag 快速定位问题。
     */
    fun d(message: String) {
        Log.d(TAG, message)
    }

    /**
     * 错误日志始终带可选异常对象，是为了让协议失败时仍能保留堆栈，不必把技术细节暴露给最终用户。
     */
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
