package org.mechdancer.remote

import kotlin.concurrent.thread

/**
 * 在后台线程中循环执行
 */
internal fun launch(block: () -> Unit) =
	thread(isDaemon = true) { while (true) block() }

/**
 * 在当前线程循环执行
 */
internal fun forever(block: () -> Unit) =
	run { while (true) block() }