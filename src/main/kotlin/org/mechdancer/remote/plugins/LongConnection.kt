package org.mechdancer.remote.plugins

import org.mechdancer.remote.core.RemoteHub
import org.mechdancer.remote.core.RemotePlugin
import org.mechdancer.remote.core.internal.AddressManager
import org.mechdancer.remote.core.protocol.bytes
import org.mechdancer.remote.core.protocol.inetSocketAddress
import org.mechdancer.remote.core.protocol.readWithLength
import org.mechdancer.remote.core.protocol.writeWithLength
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * 长连接服务插件
 * 建立、保持和中断与其他终端之间的长连接
 */
class LongConnection : RemotePlugin('C') {
    // 长连接等待套接字
    private val server = ServerSocket(0)

    // 附加到的终端
    private lateinit var host: RemoteHub

    // 本地地址
    private lateinit var address: InetSocketAddress

    // 长连接地址管理
    private val addresses = AddressManager {
        if (!::host.isInitialized)
            throw UnsupportedOperationException("plugin must be setup first")
        host.broadcast(
            id,
            ByteArrayOutputStream()
                .apply {
                    writeWithLength(it.toByteArray())
                    write(address.bytes)
                }
                .toByteArray()
        )
    }

    // 连接管理
    private val connections = mutableMapOf<String, Socket>()

    /**
     * 初始化，保存终端引用
     */
    override fun onSetup(host: RemoteHub) {
        this.host = host
        address = InetSocketAddress(host.address.address, server.localPort)
    }

    /**
     * 接收请求
     */
    override fun onBroadcast(sender: String, payload: ByteArray) {
        val (name, address) = payload
            .let(::ByteArrayInputStream)
            .use { String(it.readWithLength()) to inetSocketAddress(it.readBytes()) }

        addresses[sender] = address

        if (name == host.name)
            connections[sender] = Socket().apply { connect(address) }
    }

    //fun send(info:Pair<ByteArray,String>)
}
