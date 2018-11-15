package org.mechdancer.remote.core

/**
 * 按类型查找一个插件
 * @receiver 加载了插件的远程终端
 * @param T  目标插件类型
 * @return   终端未加载此类插件则返回空
 */
inline fun <reified T : RemotePlugin> RemoteHub.getPlugin() =
    plugins.find { it is T }?.let { it as T }

/**
 * 通过插件广播
 * @receiver 加载了插件的远程终端
 * @param T  目标插件类型
 * @return   终端未加载此类插件则返回空
 */
inline fun <reified T : RemotePlugin> RemoteHub.broadcastBy(msg: ByteArray) =
    getPlugin<T>()?.let { broadcast(it.id, msg) }

/**
 * 按类型卸载插件
 * @receiver 加载了插件的远程终端
 * @param T  目标插件类型
 * @return   终端未加载此类插件则返回空
 */
inline fun <reified T : RemotePlugin> RemoteHub.teardown() =
    getPlugin<T>()?.also(this::teardown)