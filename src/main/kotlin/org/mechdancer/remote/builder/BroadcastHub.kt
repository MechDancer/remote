package org.mechdancer.remote.builder

import org.mechdancer.remote.core.BroadcastHub

fun broadcastHub(name: String, callbacks: InnerCmdCallbacksDsl.() -> Unit) =
    InnerCmdCallbacksDsl()
        .apply(callbacks)
        .run { BroadcastHub(name, newProcessDetected, broadcastReceived) }
