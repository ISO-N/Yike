package com.kariscode.yike.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kariscode.yike.ui.component.YikeTopAppBar

/**
 * 首版首页先作为“总入口”存在，原因是导航与分层落地阶段更需要稳定的路径结构，
 * 而不是立刻在首页堆叠复杂展示；后续再把待复习统计和空/错状态接入这里。
 */
@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    onOpenDeckList: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { YikeTopAppBar(title = "忆刻") },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("首页（占位）：后续接入今日待复习概览/空状态/错误状态。")
            Button(
                onClick = onStartReview,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("开始复习")
            }
            Button(
                onClick = onOpenDeckList,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("卡组管理")
            }
            Button(
                onClick = onOpenSettings,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("设置")
            }
        }
    }
}
