package org.mechdancer.framework.remote

import org.mechdancer.framework.dependency.Dependency

/** 远程终端构建器 */
class RemoteDsl private constructor() {
    private var newMemberDetected: (String) -> Unit = {}
    private var dependencies = mutableListOf<Dependency>()

    fun newMemberDetected(block: (String) -> Unit) {
        newMemberDetected = block
    }

    fun inAddition(block: () -> Dependency) {
        dependencies.add(block())
    }

    companion object {
        fun remoteHub(
            name: String? = null,
            block: RemoteDsl.() -> Unit = {}
        ) = RemoteDsl().apply(block).let {
            RemoteHub(
                name,
                it.newMemberDetected,
                it.dependencies
            )
        }
    }
}
