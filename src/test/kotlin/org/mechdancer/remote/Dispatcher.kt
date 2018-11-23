package org.mechdancer.remote

import kotlin.concurrent.thread

internal object Dispatcher {
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
}
