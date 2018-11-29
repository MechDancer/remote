package org.mechdancer.framework.remote

import org.mechdancer.framework.dependency.Dependency

class RemoteDsl private constructor() {
    private var newMemberDetected: (String) -> Unit = {}
    private var broadcastReceived: (String, ByteArray) -> Unit = { _, _ -> }
    private var dependencies = mutableListOf<Dependency>()

    fun newMemberDetected(block: (String) -> Unit) {
        newMemberDetected = block
    }

    fun broadcastReceived(block: (String, ByteArray) -> Unit) {
        broadcastReceived = block
    }

    fun inAddition(block: () -> Dependency) {
        dependencies.add(block())
    }

    companion object {
        fun remoteHub(
            name: String? = null,
            block: RemoteDsl.() -> Unit = {}
        ): RemoteHub {
            val dsl = RemoteDsl().apply(block)
            val hub = RemoteHub(name, dsl.newMemberDetected)

            for (dependency in dsl.dependencies)
                hub.scope setup dependency

            return hub.apply { scope.sync() }
        }
    }
}
