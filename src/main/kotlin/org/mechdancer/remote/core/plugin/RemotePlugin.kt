package org.mechdancer.remote.core.plugin

interface RemotePlugin {
    val key: Key<*>

    interface Key<R : RemotePlugin> {
        /**
         * 指令识别号
         */
        val id: Char
    }
}
