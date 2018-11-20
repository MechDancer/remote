package org.mechdancer.version2.dependency.resource.basic

import org.mechdancer.version2.RemoteHub
import org.mechdancer.version2.dependency.ResourceFactory
import org.mechdancer.version2.dependency.resource.basic.HostInfo.Type
import org.mechdancer.version2.dependency.resource.basic.HostInfo.Type.Name
import org.mechdancer.version2.hashOf

class HostInfo(
    private val host: RemoteHub
) : ResourceFactory<Type, String> {
    override fun get(parameter: Type): String =
        when (parameter) {
            Name -> host.name
        }

    override fun equals(other: Any?) = other is HostInfo
    override fun hashCode() = TYPE_HASH

    enum class Type { Name }

    companion object {
        val TYPE_HASH = hashOf<HostInfo>()
    }
}
