package org.mechdancer.remote.core

/**
 * 信号阻塞
 */
class SignalBlocker {
	private val core = Object()

	fun block(timeout: Long = 0L) =
		synchronized(core) { core.wait(timeout) }

	fun awake() =
		synchronized(core) { core.notifyAll() }
}
