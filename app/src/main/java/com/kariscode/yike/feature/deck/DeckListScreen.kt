package com.kariscode.yike.feature.deck

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
 * 卡组列表页在首版先落导航与页面壳，目的是让后续的数据层接入可以沿着固定路由逐步替换占位状态，
 * 避免一边写 Room/Repository，一边临时改路由导致返工。
 */
@Composable
fun DeckListScreen(
    onBack: () -> Unit,
    onOpenDeck: (deckId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            YikeTopAppBar(
                title = "卡组列表",
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
            Text("卡组列表（占位）：后续接入创建/编辑/归档/删除与聚合信息。")
            Button(onClick = { onOpenDeck("deck_demo") }) {
                Text("进入示例卡组")
            }
        }
    }
}
