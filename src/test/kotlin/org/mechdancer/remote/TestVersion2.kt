package org.mechdancer.remote

import org.mechdancer.version2.RemoteHub
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.remote.functions.address.AddressMonitor
import org.mechdancer.version2.remote.resources.Addresses

object TestVersion2 {
    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val remote = RemoteHub(
            name = "version2",
            newMemberDetected = ::println
        )

        // 接收
        launch { remote() }

        // 询问
        val address = remote.hub.must<Addresses>()
        val synchronizer = remote.hub.must<AddressMonitor>()

        while (address["BB"] == null) {
            synchronizer ask "BB"
            Thread.sleep(1000)
        }

        println(address["BB"])
    }
}
