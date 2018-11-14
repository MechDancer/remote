package org.mechdancer.remote.core.network

/**
 * 逻辑谓词
 */
typealias Predicate<T> = (T) -> Boolean

/**
 * 从严筛选原则
 * 顺序使用一套从宽到严的规则尽量精细选择
 *
 * * 没有候选项或仅剩一个候选项，立即返回
 * * 还有多个候选项但规则已用尽，立即返回
 * * 应用最高级规则筛选并递归
 */
tailrec infix fun <T> Collection<T>.filter(
    filters: Collection<Predicate<T>>
): Collection<T> =
    takeIf { it.size < 2 || filters.isEmpty() }
        ?: filter(filters.first()) filter filters.drop(1)

/**
 * 从优筛选原则
 * 顺序使用一套从严到宽的规则尽量最优选择
 *
 * * 如果一开始就只有一个候选项，立即返回
 * * 只要使用某个规则筛后有选项存在，立即返回其中一个
 * * 否则尝试应用下一条规则
 */
infix fun <T> Collection<T>.first(
    filters: Collection<Predicate<T>>
): T? {
    when (size) {
        0 -> return null
        1 -> return single()
    }
    for (filter in filters)
        return firstOrNull(filter) ?: continue
    return firstOrNull()
}
