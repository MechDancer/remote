package org.mechdancer.framework.dependency

import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

/**
 * 类型 [T] 的依赖项
 */
sealed class AbstractDependency<T : Component>(val type: KClass<T>) {
    private val _field = AtomicReference<T?>(null)
    operator fun invoke(value: Component): T? = _field.updateAndGet { type.safeCast(value) ?: it }

    open val field: T? get() = _field.get()
    open operator fun component1(): T? = field

    override fun equals(other: Any?) = this === other || (other as? AbstractDependency<*>)?.type == type
    override fun hashCode() = type.hashCode()

    /**
     * 类型 [T] 的弱依赖项
     */
    class WeakDependency<T : Component>(type: KClass<T>) : AbstractDependency<T>(type)

    /**
     * 类型 [T] 的强依赖项
     */
    class Dependency<T : Component>(type: KClass<T>) : AbstractDependency<T>(type) {
        override val field: T get() = super.field ?: throw ComponentNotExistException(type)
        override operator fun component1(): T = field
    }
}