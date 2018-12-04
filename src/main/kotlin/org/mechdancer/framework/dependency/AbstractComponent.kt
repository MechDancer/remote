package org.mechdancer.framework.dependency

import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

abstract class AbstractComponent<T : AbstractComponent<T>>
    (private val type: KClass<T>) : Component {

    override fun equals(other: Any?) =
        this === other || type.safeCast(other) !== null

    override fun hashCode() =
        type.hashCode()
}
