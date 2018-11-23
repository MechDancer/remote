package org.mechdancer.framework.remote.resources

import org.mechdancer.framework.dependency.ResourceFactory
import org.mechdancer.framework.dependency.hashOf
import org.mechdancer.framework.remote.resources.Name.Type
import org.mechdancer.framework.remote.resources.Name.Type.NAME

/**
 * @param name 名字
 */
class Name(private val name: String) :
    ResourceFactory<Type, String> {

    override fun get(parameter: Type) =
        when (parameter) {
            NAME -> name
        }

    override fun equals(other: Any?) = other is Name
    override fun hashCode() = TYPE_HASH

    enum class Type { NAME }

    private companion object {
        val TYPE_HASH = hashOf<Name>()
    }
}
