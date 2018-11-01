package org.mechdancer.remote.core

import kotlin.concurrent.thread

fun launch(block: () -> Unit) =
	thread(isDaemon = true) { while (true) block() }

fun forever(block: () -> Unit) =
	run { while (true) block() }
