package org.mechdancer.remote.plugins

import org.mechdancer.remote.core.AddressManager
import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.RemotePlugin
import org.mechdancer.remote.core.protocol.bytes
import org.mechdancer.remote.core.protocol.inetSocketAddress
import org.mechdancer.remote.core.protocol.readWithLength
import org.mechdancer.remote.core.protocol.writeWithLength
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.collections.set
import kotlin.concurrent.thread

/**
 * 长连接服务插件
 * 建立、保持和中断与其他终端之间的长连接
 */
class LongConnection : RemotePlugin('C') {
    // 长连接等待套接字
    private val server = ServerSocket(0)

    // 附加到的终端
    private var host: RemoteHub? = null

    // 本地地址
    private var address: InetSocketAddress? = null

    // 长连接地址管理
    private val endpoints = AddressManager {
        // 发送连接请求
        host?.broadcast(id,
                        ByteArrayOutputStream()
                            .apply {
                                writeWithLength(it.toByteArray())
                                write(address!!.bytes)
                            }
                            .toByteArray()
        ) ?: throw UnsupportedOperationException("plugin must be setup first")
    }

    // 连接管理
    private val connections = mutableMapOf<String, Socket>()

    /**
     * 初始化，保存终端引用
     */
    override fun onSetup(host: RemoteHub) {
        this.host = host
        this.address = InetSocketAddress(host.address.address, server.localPort)
        thread {
            while (true)
                server.accept()
                    ?.takeIf { this.host == host }
                    ?.let {
                        val name = String(it.getInputStream().readWithLength())
                        endpoints[name] = it.remoteSocketAddress as InetSocketAddress
                        connections[name] = it
                    }
                    ?: break
        }
    }

    /**
     * 卸载，终止接收
     */
    override fun onTeardown() {
        for (socket in connections.values) socket.close()
        connections.clear()
        host = null
        address = null
    }

    /**
     * 响应连接请求
     */
    override fun onBroadcast(sender: String, payload: ByteArray) {
        val (name, endpoint) = payload
            .let(::ByteArrayInputStream)
            .use { String(it.readWithLength()) to inetSocketAddress(it.readBytes()) }
        endpoints[sender] = endpoint

        host?.name
            ?.takeIf { it == name }
            ?.toByteArray()
            ?.let {
                connections[sender] = Socket()
                    .apply {
                        connect(endpoint)
                        getOutputStream().writeWithLength(it)
                    }
            }
    }

    private fun connect(name: String): Socket {
        connections[name]?.let { return it }
        endpoints[name]
        connections[name]?.let { return it }
        connections[name] = endpoints.connect(name)!!
        return connections[name]!!
    }

    fun sendTo(name: String, msg: ByteArray) {
        connect(name).getOutputStream().writeWithLength(msg)
    }

    fun receiveFrom(name: String) =
        connect(name).getInputStream().readWithLength()
}
