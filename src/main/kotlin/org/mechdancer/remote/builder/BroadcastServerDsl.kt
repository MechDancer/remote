package org.mechdancer.remote.builder

import org.mechdancer.remote.core.BroadcastServer

fun broadcastServer(name: String, callbacks: InnerCmdCallbacksDsl.() -> Unit) =
    InnerCmdCallbacksDsl()
        .apply(callbacks)
        .run { BroadcastServer(name, newProcessDetected, broadcastReceived) }

