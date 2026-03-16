package com.kariscode.yike.core.message

/**
 * 统一管理应用中的错误消息和提示文本，
 * 避免在多处硬编码导致维护困难和内容不一致。
 */
object ErrorMessages {
    const val LOAD_FAILED = "加载失败"
    const val RETRY_LATER = "请稍后重试"
    const val SAVE_FAILED = "保存失败，请稍后重试"
    const val DELETE_FAILED = "删除失败，请稍后重试"
    const val UPDATE_FAILED = "更新失败，请稍后重试"

    const val NAME_REQUIRED = "名称不能为空"
    const val TITLE_REQUIRED = "标题不能为空"
    const val QUESTION_CONTENT_REQUIRED = "题面不能为空"
    const val ANSWER_CONTENT_REQUIRED = "答案不能为空"
}

object SuccessMessages {
    const val SAVED = "已保存"
    const val DELETED = "已删除"
    const val UPDATED = "已更新"
}
