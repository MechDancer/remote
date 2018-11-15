package org.mechdancer.remote.core

inline fun <reified T : RemotePlugin> RemoteHub.getPlugin() =
    pluginView.find { it is T }?.let { it as T }

inline fun <reified T : RemotePlugin> RemoteHub.broadcastBy(msg: ByteArray) =
    getPlugin<T>()?.let { broadcast(it.id, msg) }

inline fun <reified T : RemotePlugin> RemoteHub.teardown() =
    getPlugin<T>()
        ?.also(this::teardown)
        ?: throw IllegalArgumentException(
            "no ${T::class.java.simpleName} setup to this hub"
        )
