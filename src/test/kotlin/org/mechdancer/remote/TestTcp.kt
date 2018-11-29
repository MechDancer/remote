package org.mechdancer.remote

import org.mechdancer.framework.dependency.plusAssign
import org.mechdancer.framework.remote.RemoteHub
import org.mechdancer.framework.remote.modules.tcpconnection.CommonShortConnection
import org.mechdancer.framework.remote.modules.tcpconnection.listen
import org.mechdancer.framework.remote.modules.tcpconnection.say
import org.mechdancer.framework.remote.resources.TcpCmd.COMMON
import org.mechdancer.remote.Dispatcher.launch
import java.net.Socket

private object TestTcp {
    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val remote = RemoteHub(
            name = "kotlin echo server",
            newMemberDetected = ::println
        ).apply {
            hub += CommonShortConnection {
                println("accepted: $this")

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

        with(RemoteHub("kotlin")) {
            openAllNetwork()
            launch { invoke() }

            tailrec fun connect(): Socket {
                connect("kotlin echo server", COMMON)?.let { return it }
                Thread.sleep(1000)
                return connect()
            }

            connect().use { I ->
                while (true) {
                    readLine()!!
                        .also { I say it.toByteArray() }
                        .takeUnless { it == "over" }
                        ?: break
                    println(String(I.listen()))
                }
            }
        }
    }
}
