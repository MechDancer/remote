package org.mechdancer.remote.builder

import org.mechdancer.remote.plugins.ResourcePlugin

class ResourcePluginBuilder internal constructor() {
    val resources = mutableMapOf<String, ByteArray>()
}

fun RemoteCallbackBuilder.Plugins.resourcePlugin(
    retry: Long = 2000,
    block: ResourcePluginBuilder.() -> Unit
) = setup(
    ResourcePlugin(
        retry,
        *ResourcePluginBuilder()
            .also(block)
            .resources
            .map { it.toPair() }
            .toTypedArray()
    )
)
