package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.Component
import org.mechdancer.framework.dependency.hashOf

class Name(value: String) : Component {
    override fun equals(other: Any?) = other is Name
    override fun hashCode() = TYPE_HASH

    val field = value.trim()

    private companion object {
        val TYPE_HASH = hashOf<Name>()
    }
}
