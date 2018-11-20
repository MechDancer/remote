package org.mechdancer.version2.dependency

import org.mechdancer.version2.Hub

abstract class AbstractModule : FunctionModule {
    protected lateinit var host: Hub

    override fun onSetup(host: Hub) {
        this.host = host
        sync()
    }

    override fun sync() = Unit
}
