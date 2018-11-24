package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.Dependency
import org.mechdancer.framework.dependency.hashOf

/**
 * @param value 名字
 */
data class Name(val value: String) : Dependency {
    override fun equals(other: Any?) = other is Name
    override fun hashCode() = TYPE_HASH

    private companion object {
        val TYPE_HASH = hashOf<Name>()
    }
}
