package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.AbstractComponent
import org.mechdancer.framework.dependency.buildView
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

/**
 * 监听套接字资源
 */
class ServerSockets(private val port: Int = 0) :
    AbstractComponent<ServerSockets>(ServerSockets::class) {
    private val core = ConcurrentHashMap<Int, ServerSocket>()
    val view = buildView(core)

    val default by lazy { ServerSocket(port) }

    /**
     * 获取或构造新的套接字资源
     * @param parameter 端口号
     * @return 监听套接字
     */
    operator fun get(parameter: Int) =
        if (parameter == 0) default
        else core
            .runCatching {
                computeIfAbsent(parameter) {
                    ServerSocket(parameter)
                }
            }
            .getOrNull()
}
