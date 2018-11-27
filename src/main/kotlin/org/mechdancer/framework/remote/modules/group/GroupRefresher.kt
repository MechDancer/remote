package org.mechdancer.framework.remote.modules.group

import org.mechdancer.framework.dependency.AbstractModule
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.remote.resources.Group

/**
 * 组刷新器
 */
class GroupRefresher : AbstractModule() {
    private val group by must<Group>(host)
    private val monitor by must<GroupMonitor>(host)

    operator fun invoke(timeout: Int): List<String> {
        monitor.yell()
        Thread.sleep(timeout.toLong())
        return group[timeout + 100]
    }
}
