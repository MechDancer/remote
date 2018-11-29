package org.mechdancer.remote

import org.mechdancer.framework.remote.RemoteDsl.Companion.remoteHub
import org.mechdancer.framework.remote.modules.tcpconnection.CommonTcpServer
import org.mechdancer.framework.remote.modules.tcpconnection.listen
import org.mechdancer.framework.remote.modules.tcpconnection.say
import org.mechdancer.framework.remote.resources.TcpCmd.COMMON
import org.mechdancer.remote.Dispatcher.launch
import java.net.Socket

private object TestTcp {
    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val remote =
            remoteHub("kotlin echo server") {

                newMemberDetected {
                    println("server detect $it")
                }

                inAddition {
                    CommonTcpServer {
                        println("server accepted: $this")

                        while (!isClosed)
                            String(listen())
                                .also { println("server heard: \"$it\"") }
                                .takeUnless { it == "over" }
                                ?.let { "you said \"$it\"" }
                                ?.toByteArray()
                                ?.also(this::say)
                                ?: break

                        println("server separate from $this")
                    }
                }
            }

        // 接收
        launch(remote::invoke)
        launch(remote::accept)
        launch(remote::accept)

        println("server started")

        remoteHub("kotlin").apply {
            openAllNetwork()
            launch { invoke() }

            var server: Socket?

            do {
                server = connect("kotlin echo server", COMMON)
                Thread.sleep(200)
            } while (server == null)

            server.use { I ->
                do {
                    readLine()!!
                        .also { I say it.toByteArray() }
                        .takeUnless { it == "over" }
                        ?: break
                    println(String(I.listen()))
                } while (true)
            }
        }
    }
}
