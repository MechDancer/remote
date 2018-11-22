package org.mechdancer.version2.dependency

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Maybe<out T : Dependency>(
    private val block: () -> T?
) : ReadOnlyProperty<Dependency, T?> {
    private var field: T? = null
    override fun getValue(thisRef: Dependency, property: KProperty<*>) =
        field ?: block()?.also { field = it }
}
