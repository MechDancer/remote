package org.mechdancer.remote

import org.mechdancer.framework.RemoteHub
import org.mechdancer.framework.dependency.must
import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.remote.modules.address.AddressMonitor
import org.mechdancer.framework.remote.modules.tcpconnection.CommonShortConnection
import org.mechdancer.framework.remote.modules.tcpconnection.ShortConnectionClient
import org.mechdancer.framework.remote.modules.tcpconnection.listen
import org.mechdancer.framework.remote.modules.tcpconnection.say
import org.mechdancer.framework.remote.resources.Addresses
import org.mechdancer.framework.remote.resources.TcpCmd.COMMON
import org.mechdancer.remote.Dispatcher.launch

private object TestTcp {
    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val remote = RemoteHub(
            name = "framework",
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
        launch { remote.accept() }

        with(RemoteHub("client")) {
            launch { invoke() }

            // 询问
            val address = hub.must<Addresses>()
            val synchronizer = hub.must<AddressMonitor>()
            val connector = hub.must<ShortConnectionClient>()

            while (address["framework"] == null) {
                synchronizer ask "framework"
                Thread.sleep(1000)
            }

            (connector connect "framework")!!.use { I ->
                println("connected: ${I.remoteSocketAddress}")

                I say COMMON

                while (true) {
                    readLine()!!
                        .takeUnless { it == "over" }
                        ?.toByteArray()
                        ?.also(I::say)
                        ?: break
                    String(I.listen())
                        .also(::println)
                }

                I say "over".toByteArray()
            }
        }
    }
}
