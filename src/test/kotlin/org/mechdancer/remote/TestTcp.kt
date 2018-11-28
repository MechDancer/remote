package org.mechdancer.remote

import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.remote.RemoteHub
import org.mechdancer.framework.remote.modules.tcpconnection.CommonShortConnection
import org.mechdancer.framework.remote.modules.tcpconnection.listen
import org.mechdancer.framework.remote.modules.tcpconnection.say
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
                        .also { println("heard: $it") }
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
        launch(remote::invoke)
        launch(remote::accept)
        launch(remote::accept)

        println("server started")

        with(RemoteHub("client")) {
            openAllNetwork()
            launch { invoke() }

            // 询问
            while (this["framework"] == null) {
                ask("framework")
                Thread.sleep(1000)
            }

            (connect("framework", COMMON))!!.use { I ->
                println("connected: ${I.remoteSocketAddress}")
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
