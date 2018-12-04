package org.mechdancer.framework.dependency

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 抽象依赖
 * 封装了依赖项管理功能
 */
abstract class AbstractDependent : Dependent {
    /** 尚未装载的依赖项集 */
    protected val dependencies = mutableListOf<AbstractDependency<*>>()

    /** 每一次扫描都清除成功装载的依赖项 */
    override fun sync(dependency: Component) =
        dependencies.removeIf { it.set(dependency) != null } && dependencies.isEmpty()

    /** 构造一个 [C] 类型的强依赖 */
    protected inline fun <reified C : Component> dependency() =
        AbstractDependency.Dependency(C::class).also { dependencies.add(it) }

    /** 构造一个 [C] 类型的弱依赖 */
    protected inline fun <reified C : Component> weakDependency() =
        AbstractDependency.WeakDependency(C::class).also { dependencies.add(it) }

    /** 从一个 [C] 类型的强依赖取值 */
    protected inline fun <reified C : Component, T> must(crossinline block: (C) -> T): Lazy<T> {
        val dependency = dependency<C>()
        return lazy { dependency.field.let(block) }
    }

    /** 从一个 [C] 类型的弱依赖取值 */
    protected inline fun <reified C : Component, T> maybe(default: T, crossinline block: (C) -> T): Lazy<T> {
        val dependency = weakDependency<C>()
        return lazy { dependency.field?.let(block) ?: default }
    }

    /** 构造一个 [C] 类型的强依赖属性代理 */
    protected inline fun <reified C : Component> must() =
        object : ReadOnlyProperty<AbstractDependent, C> {
            private val core = dependency<C>()
            override fun getValue(thisRef: AbstractDependent, property: KProperty<*>) = core.field
        }

    /** 构造一个 [C] 类型的弱依赖属性代理 */
    protected inline fun <reified C : Component> maybe() =
        object : ReadOnlyProperty<AbstractDependent, C?> {
            private val core = weakDependency<C>()
            override fun getValue(thisRef: AbstractDependent, property: KProperty<*>) = core.field
        }
}
