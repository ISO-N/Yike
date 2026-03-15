package com.kariscode.yike.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Material3 的 TopAppBar 目前仍处于实验 API，集中封装能把 opt-in 控制在单点，
 * 避免在每个页面到处散落 @OptIn 注解导致后续调整 AppBar 样式时遗漏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YikeTopAppBar(
    title: String,
    subtitle: String? = null,
    navigationAction: NavigationAction? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (navigationAction != null) {
                IconButton(onClick = navigationAction.onClick) {
                    Text(navigationAction.label)
                }
            }
        },
        actions = {
            if (!actionText.isNullOrBlank() && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(actionText)
                }
            }
        }
    )
}

/**
 * 通过显式的导航动作模型，能让“返回/退出”等语义在 UI 层保持一致，
 * 避免以后出现同一页面用图标、另一页面用文字导致交互不统一。
 */
data class NavigationAction(
    val label: String,
    val onClick: () -> Unit
)

/**
 * 统一返回动作的文案与建模方式，是为了让各页面不用重复拼装相同的导航对象，
 * 并把“聚焦流页面默认使用文字返回”这个约定固定在一个位置维护。
 */
fun backNavigationAction(onClick: () -> Unit): NavigationAction =
    NavigationAction(label = "返", onClick = onClick)

