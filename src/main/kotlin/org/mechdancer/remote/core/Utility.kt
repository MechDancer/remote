package org.mechdancer.remote.core

import org.mechdancer.remote.builder.RemoteCallbackBuilder
import java.rmi.Remote
import kotlin.concurrent.thread

/**
 * 在后台线程中循环执行
 */
fun launch(block: () -> Unit) =
	thread(isDaemon = true) { while (true) block() }

/**
 * 在当前线程循环执行
 */
fun forever(block: () -> Unit) =
	run { while (true) block() }

/**
 * 反射获取接口名字
 */
inline fun <reified T : Remote> nameOf() =
	T::class.simpleName ?: throw IllegalArgumentException("class nameless")

/**
 * 用接口名初始化一个服务
 */
inline fun <reified T : Remote> RemoteCallbackBuilder.put(remote: T) {
	services[nameOf<T>()] = remote
}

/**
 * 使用默认命名挂载 RMI 服务
 */
inline fun <reified T : Remote> RemoteHub.load(remote: T) =
	load(nameOf<T>(), remote)

/**
 * 使用默认命名挂载 RMI 服务
 */
inline fun <reified T : Remote> RemoteHub.cancel() =
	cancel(nameOf<T>())

/**
 * 连接默认命名的 RMI 服务
 */
inline fun <reified T : Remote> RemoteHub.connectRMI(name: String) =
	connectRMI<T>(name, nameOf<T>())
