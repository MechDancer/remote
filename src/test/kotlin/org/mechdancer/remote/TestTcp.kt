package org.mechdancer.remote

import org.mechdancer.framework.RemoteHub
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.remote.functions.address.AddressMonitor
import org.mechdancer.framework.remote.functions.tcpconnection.*
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.TcpCmd.COMMON

object TestTcp {
    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val remote = RemoteHub(
            name = "framework",
            newMemberDetected = ::println
        ).apply {
            hub += CommonShortConnection {
                while (!isClosed)
                    String(invoke())
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
        launch { remote.accept() }

        with(RemoteHub("client")) {
            launch { invoke() }

            // 询问
            val address = hub.must<Addresses>()
            val synchronizer = hub.must<AddressMonitor>()

            while (address["framework"] == null) {
                synchronizer ask "framework"
                Thread.sleep(1000)
            }

            with(hub.must<ShortConnectionClient>().connect("framework")!!) {
                println("connected: $remoteSocketAddress")
                order(COMMON)
                while (true) {
                    readLine()!!
                        .takeUnless { it == "over" }
                        ?.toByteArray()
                        ?.also(this::say)
                        ?: break
                    String(invoke())
                        .also(::println)
                }
                say("over".toByteArray())
                close()
            }
        }
    }
}
