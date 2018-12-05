package org.mechdancer.remote

import org.mechdancer.framework.remote.RemoteDsl.Companion.remoteHub
import org.mechdancer.framework.remote.modules.tcpconnection.CommonTcpServer
import org.mechdancer.framework.remote.modules.tcpconnection.listen
import org.mechdancer.framework.remote.modules.tcpconnection.say
import org.mechdancer.framework.remote.resources.TcpCmd.COMMON
import org.mechdancer.remote.Dispatcher.launch

private object TestTcp {
    const val serverName = "kotlin echo server"
    const val over = "over"
    fun serverPrint(string: String) = println("- server $string")

    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化
        val remote =
            remoteHub(serverName) {

                newMemberDetected {
                    serverPrint("detect $it")
                }

                inAddition {
                    CommonTcpServer { client, I ->
                        serverPrint("accepted: $client")

                        while (!I.isClosed)
                            "\"${String(I.listen())}\""
                                .takeUnless { it == "\"$over\"" }
                                ?.also { serverPrint("heard: $it") }
                                ?.also { I say "you said $it".toByteArray() }
                                ?: break

                        serverPrint("separate from $client")
                    }
                }
            }

        // 接收
        launch(remote::invoke)
        repeat(3) {
            launch(remote::accept)
        }

        serverPrint("started")

        remoteHub("kotlin").apply {
            openOneNetwork()
            launch { invoke() }

            var success: Boolean

            do {
                success = connect(serverName, COMMON) { I ->
                    do {
                        readLine()!!
                            .also { I say it.toByteArray() }
                            .takeUnless { it == over }
                            ?: break
                        println("server heard ${String(I.listen())}")
                    } while (true)
                } != null
                if (!success) Thread.sleep(200)
            } while (!success)
        }
    }
}
