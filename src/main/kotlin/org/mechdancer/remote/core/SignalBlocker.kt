package org.mechdancer.remote.core

/**
 * 信号阻塞
 */
internal class SignalBlocker {
	private val core = Object()

	/**
	 * 阻塞等待信号
	 * @param timeout 用毫秒表示的阻塞时间
	 */
	fun block(timeout: Long = 0) =
		synchronized(core) { core.wait(timeout) }

	/**
	 * 唤醒所有阻塞的线程
	 */
	fun awake() =
		synchronized(core) { core.notifyAll() }
}
