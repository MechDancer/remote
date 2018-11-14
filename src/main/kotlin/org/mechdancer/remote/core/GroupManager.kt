package org.mechdancer.remote.core

/**
 * 组管理器
 */
class GroupManager {
	// 组存储
	private val group = mutableMapOf<String, Long>()

	/**
	 * 加入新成员或更新沉默时间
	 * @param name 名字
	 * @return 名字是否是首次出现
	 */
	infix fun detect(name: String) =
		(name !in group).also { group[name] = System.currentTimeMillis() }

	/**
	 * 获取成员的沉默时间
	 * @param name 要获取的成员名字
	 * @return 若成员存在，最后一次出现时间到当前的毫秒数
	 *         若成员不存在，返回当前时间的总毫秒数
	 */
	fun silenceTime(name: String) =
		System.currentTimeMillis() - (group[name] ?: 0)

	/**
	 * 获取成员的沉默时间
	 * @param names 感兴趣的成员名录，不写表示获取所有成员
	 * @return 所有感兴趣成员的沉默时间
	 */
	fun silenceTime(vararg names: String): Map<String, Long> {
		val now = System.currentTimeMillis()
		val interest =
			if (names.isEmpty()) group
			else group.filterKeys { it in names }
		return interest.mapValues { now - it.value }
	}

	/**
	 * 按沉默时间筛选
	 * @param time 最大沉默时间
	 * @return 沉默时间短于[time]的组成员
	 */
	infix fun filterByTime(time: Long) =
		silenceTime()
			.filterValues { it < time }
			.keys
}
