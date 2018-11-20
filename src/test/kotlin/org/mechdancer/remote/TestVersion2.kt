package org.mechdancer.remote

import org.mechdancer.version2.RemoteHub

object TestVersion2 {
    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val remote = RemoteHub(
            name = "version2",
            newMemberDetected = ::println
        )

        // 接收
        forever { remote() }
    }
}
