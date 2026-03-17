package com.kariscode.yike.domain.model

/**
 * 主题模式作为独立领域枚举存在，是为了让设置存储、备份恢复与 UI 渲染共享同一语义，
 * 避免各层各自维护一套字符串常量后逐渐产生取值漂移。
 */
enum class ThemeMode(
    val storageValue: String,
    val displayLabel: String
) {
    LIGHT(storageValue = "light", displayLabel = "浅色"),
    DARK(storageValue = "dark", displayLabel = "深色"),
    SYSTEM(storageValue = "system", displayLabel = "跟随系统");

    companion object {
        /**
         * 未知存储值统一回退到 LIGHT，是为了兼容旧版本与损坏数据，
         * 同时延续当前应用“默认浅色”的既有视觉预期。
         */
        fun fromStorageValue(value: String?): ThemeMode =
            entries.firstOrNull { mode -> mode.storageValue == value } ?: LIGHT
    }
}
