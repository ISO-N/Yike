package com.kariscode.yike.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kariscode.yike.domain.model.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 设置仓储测试用于守住 DataStore 与领域模型之间的映射边界，
 * 避免新增主题字段后出现“界面可选但重启丢失”的静默回归。
 */
class DataStoreAppSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /**
     * 主题模式必须被稳定持久化，
     * 否则用户下次启动应用时会看到和上次不同的界面风格。
     */
    @Test
    fun setThemeMode_persistsSelectedMode() = runTest {
        val repository = DataStoreAppSettingsRepository(
            dataStore = PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { temporaryFolder.newFile("app-settings.preferences_pb") }
            )
        )

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.getSettings().themeMode)
    }
}
