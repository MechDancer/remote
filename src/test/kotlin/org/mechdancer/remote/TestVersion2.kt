package org.mechdancer.remote

import org.mechdancer.version2.RemoteHub
import org.mechdancer.version2.dependency.must
import org.mechdancer.version2.dependency.plusAssign
import org.mechdancer.version2.remote.functions.address.AddressMonitor
import org.mechdancer.version2.remote.functions.tcpconnection.*
import org.mechdancer.version2.remote.resources.Addresses
import org.mechdancer.version2.remote.resources.TcpCmd.COMMON

object TestVersion2 {
    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val remote = RemoteHub(
            name = "version2",
            newMemberDetected = ::println
        ).apply {
            hub += CommonShortConnection {
                while (!isClosed)
                    String(listen())
                        .takeUnless { it == "over" }
                        ?.let { "you said \"$it\"" }
                        ?.toByteArray()
                        ?.also(this::say)
                        ?: break
                println("bye~")
            }
            hub.sync()
        }

        // 接收
        launch { remote() }
        launch { remote.listen() }

        with(RemoteHub("client")) {
            launch { invoke() }

            // 询问
            val address = hub.must<Addresses>()
            val synchronizer = hub.must<AddressMonitor>()

            while (address["version2"] == null) {
                synchronizer ask "version2"
                Thread.sleep(1000)
            }

            with(hub.must<ShortConnectionClient>().connect("version2")!!) {
                println("connected: $remoteSocketAddress")
                order(COMMON)
                while (true) {
                    readLine()!!
                        .takeUnless { it == "over" }
                        ?.toByteArray()
                        ?.also(this::say)
                        ?: break
                    String(listen())
                        .also(::println)
                }
                say("over".toByteArray())
                close()
            }
        }
    }
}
