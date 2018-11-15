package org.mechdancer.remote.core.plugin

import org.mechdancer.remote.core.RemoteHub

interface RemotePlugin {
    val key: Key<*>


    fun onSetup(host: RemoteHub) {

    }

    fun onTeardown() {}


    interface Key<R : RemotePlugin> {
        /**
         * 指令识别号
         */
        val id: Char
    }
}
