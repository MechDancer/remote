package org.mechdancer.version2.remote.resources

import org.mechdancer.version2.dependency.ResourceFactory
import org.mechdancer.version2.hashOf
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

/**
 * 监听套接字资源
 */
class ServerSockets : ResourceFactory<Int, ServerSocket> {
    private val default by lazy { ServerSocket(0) }
    private val core = ConcurrentHashMap<Int, ServerSocket>()

    /**
     * 获取或构造新的套接字资源
     * @param parameter 端口号
     * @return 监听套接字
     */
    override fun get(parameter: Int) =
        if (parameter == 0) default
        else core
            .runCatching {
                computeIfAbsent(parameter) {
                    ServerSocket(parameter)
                }
            }
            .getOrNull()

    override fun equals(other: Any?) = other is ServerSockets
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<ServerSockets>()
    }
}
