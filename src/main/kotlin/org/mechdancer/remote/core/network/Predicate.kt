package org.mechdancer.remote.core.network

/**
 * 从严筛选原则
 * 顺序使用一套从宽到严的规则尽量精细选择
 *
 * @return 逐个调用规则进行筛选，直到规则用尽或候选项不到两个
 */
infix fun <T> Collection<T>.tryFilter(
	filters: Iterable<(T) -> Boolean>
): Collection<T> {
	if (size < 2)
		return this
	var receiver = this
	for (filter in filters)
		receiver = receiver
			.filter(filter)
			.takeUnless { it.size < 2 }
			?: break

	return receiver
}

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
	filters: Iterable<(T) -> Boolean>
): Collection<T> {
	if (size < 2)
		return this
	for (filter in filters)
		return filter(filter)
			.takeIf(Iterable<*>::any)
			?: continue
	return this
}

/**
 * 时变性筛选
 * 反复尝试等待集合中有且仅有一个元素满足谓词
 *
 * @exception AssertionError 传入空的集合引发此异常
 */
tailrec infix fun <T> Iterable<T>.waitSingle(
	filter: (T) -> Boolean
): T = also { assert(it.any()) }
	.singleOrNull(filter)
	?: waitSingle(filter)
