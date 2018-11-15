package org.mechdancer.remote.builder

import org.mechdancer.remote.core.plugin.ResourcePlugin

class ResourcePluginBuilder {
    val resources = mutableMapOf<String, ByteArray>()
}

fun RemoteCallbackBuilder.Plugins.resourcePlugin(block: ResourcePluginBuilder.() -> Unit) =
    setup(ResourcePlugin(*ResourcePluginBuilder()
        .also(block)
        .resources
        .map { entry -> entry.toPair() }
        .toTypedArray()))