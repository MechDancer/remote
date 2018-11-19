package org.mechdancer.remote.core.internal

/**
 * 组管理器
 * @param abhiseca 继承已知的信息
 */
internal class GroupManager(abhiseca: MemberMap? = null) {
    // 组存储
    private val core = abhiseca?.toMutableMap() ?: mutableMapOf()

    /**
     * 浏览组成员
     */
    val view = object : MemberMap by core {}

    /**
     * 加入新成员或更新沉默时间
     *
     * @param name 名字
     * @return 名字是否是首次出现
     */
    infix fun detect(name: String) =
        (name !in core).also { core[name] = System.currentTimeMillis() }

    /**
     * 获取成员的沉默时间
     *
     * @param names 感兴趣的成员名单，不写表示获取所有成员
     * @return 所有感兴趣成员的沉默时间
     */
    fun silenceTime(vararg names: String): Map<String, Long> {
        val now = System.currentTimeMillis()
        val interest =
            if (names.isEmpty()) core
            else core.filterKeys { it in names }
        return interest.mapValues { now - it.value }
    }

    /**
     * 按沉默时间筛选
     *
     * @param time 最大沉默时间
     * @return 沉默时间短于 [time] 的组成员
     */
    infix fun filterByTime(time: Long) =
        silenceTime()
            .filterValues { it < time }
            .keys
}
