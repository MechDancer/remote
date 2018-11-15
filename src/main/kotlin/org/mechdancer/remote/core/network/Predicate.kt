package org.mechdancer.remote.core.network

/**
 * 从严筛选原则
 * 顺序使用一套从宽到严的规则尽量精细选择
 *
 * @return
 * * 没有候选项或仅剩一个候选项，立即返回
 * * 还有多个候选项但规则已用尽，立即返回
 * * 应用最高级规则筛选并递归
 */
tailrec infix fun <T> Collection<T>.tryFilter(
	filters: Collection<(T) -> Boolean>
): Collection<T> =
	takeIf { it.size < 2 || filters.isEmpty() }
		?: filter(filters.first()) tryFilter filters.drop(1)

/**
 * 从优筛选原则
 * 顺序使用一套从严到宽的规则尽量最优选择
 *
 * @return
 * * 如果一开始就没有或只有一个候选项，立即返回
 * * 只要使用某个规则筛后有选项存在，立即返回
 * * 否则尝试应用下一条规则
 */
infix fun <T> Collection<T>.tryBest(
	filters: Collection<(T) -> Boolean>
): Collection<T> {
	if (size < 2) return this
	for (filter in filters)
		return filter(filter)
			.takeIf(Collection<*>::isNotEmpty)
			?: continue
	return this
}

/**
 * 时变性筛选
 * 反复尝试等待集合中有且仅有一个元素满足谓词
 *
 * @exception AssertionError 传入空的集合引发此异常
 */
tailrec infix fun <T> Collection<T>.waitSingle(
	filter: (T) -> Boolean
): T = also { assert(it.isNotEmpty()) }
	.singleOrNull(filter)
	?: waitSingle(filter)
