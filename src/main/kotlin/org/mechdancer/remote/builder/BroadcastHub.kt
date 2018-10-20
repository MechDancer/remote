package org.mechdancer.remote.builder

import org.mechdancer.remote.core.BroadcastHub

/**
 * 建造一个广播服务器
 *
 * @param name 进程名
 * @param callbacks 请求回调
 */
fun broadcastHub(name: String, callbacks: InnerCmdCallbacksBuilder.() -> Unit) =
    InnerCmdCallbacksBuilder()
        .apply(callbacks)
        .run { BroadcastHub(name, newProcessDetected, broadcastReceived, remoteProcess) }
