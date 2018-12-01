package org.mechdancer.framework.remote

import org.mechdancer.framework.dependency.Component

/** 远程终端构建器 */
class RemoteDsl private constructor() {
    private var newMemberDetected: (String) -> Unit = {}
    private var dependencies = mutableListOf<Component>()

    fun newMemberDetected(block: (String) -> Unit) {
        newMemberDetected = block
    }

    fun inAddition(block: () -> Component) {
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
