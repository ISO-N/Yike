package com.kariscode.yike.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kariscode.yike.ui.component.NavigationAction
import com.kariscode.yike.ui.component.YikeTopAppBar

/**
 * 设置页集中承载提醒与备份入口的原因是这些能力都属于“全局行为”，
 * 把入口固定在此处可以避免在首页或内容页散落高风险操作入口。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            YikeTopAppBar(
                title = "设置",
                navigationAction = NavigationAction(label = "返回", onClick = onBack)
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("设置页（占位）：后续接入提醒开关/时间选择/权限提示与持久化。")
            Button(onClick = onOpenBackupRestore) {
                Text("备份与恢复")
            }
        }
    }
}
