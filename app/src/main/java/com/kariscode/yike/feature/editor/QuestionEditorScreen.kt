package com.kariscode.yike.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kariscode.yike.ui.component.NavigationAction
import com.kariscode.yike.ui.component.YikeTopAppBar

/**
 * 编辑页将来会承载“卡片信息 + 多问题编辑”的复杂表单状态；
 * 先建立页面壳可以让后续的表单状态机与保存用例有固定承载点，避免把校验规则散落到 Composable。
 */
@Composable
fun QuestionEditorScreen(
    cardId: String,
    deckId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            YikeTopAppBar(
                title = "问题编辑",
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
            Text("cardId: $cardId")
            Text("deckId: ${deckId ?: "(null)"}")
            Text("问题编辑（占位）：后续接入字段校验、空答案保存与删除。")
        }
    }
}
