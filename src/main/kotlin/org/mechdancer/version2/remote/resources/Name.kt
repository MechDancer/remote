package org.mechdancer.version2.remote.resources

import org.mechdancer.version2.dependency.ResourceFactory
import org.mechdancer.version2.hashOf
import org.mechdancer.version2.remote.resources.Name.Type
import org.mechdancer.version2.remote.resources.Name.Type.NAME

/**
 * @param name 名字
 */
class Name(private val name: String) :
    ResourceFactory<Type, String> {

    override fun get(parameter: Type) =
        when (parameter) {
            NAME -> name
        }

    override fun equals(other: Any?) = other is Group
    override fun hashCode() = TYPE_HASH

    enum class Type { NAME }

    private companion object {
        val TYPE_HASH = hashOf<Name>()
    }
}
