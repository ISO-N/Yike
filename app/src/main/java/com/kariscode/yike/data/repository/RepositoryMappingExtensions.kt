package com.kariscode.yike.data.repository

/**
 * 仓储层频繁做 entity/domain 集合映射，把最薄的一层模板收口后，
 * 能减少样板噪音并让每个仓储更聚焦于查询口径而不是容器转换。
 */
inline fun <Input, Output> List<Input>.mapModels(
    transform: (Input) -> Output
): List<Output> = map(transform)

/**
 * 可空单对象映射统一走同一入口，可避免各仓储重复写 `?.let(transform)` 这种边界模板。
 */
inline fun <Input, Output> Input?.mapModel(
    transform: (Input) -> Output
): Output? = this?.let(transform)
