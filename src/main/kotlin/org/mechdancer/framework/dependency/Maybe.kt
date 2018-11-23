package org.mechdancer.framework.dependency

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 惰性获取，但未取得将在下一次重试
 */
class Maybe<out T : Dependency>(
    private val block: () -> T?
) : ReadOnlyProperty<Dependency, T?> {

    private var field: T? = null

    override fun getValue(
        thisRef: Dependency,
        property: KProperty<*>
    ) = field ?: synchronized(block) {
        block()?.also { field = it }
    }
}
