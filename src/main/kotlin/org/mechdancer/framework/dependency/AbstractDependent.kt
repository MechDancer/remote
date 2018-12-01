package org.mechdancer.framework.dependency

/**
 * 抽象依赖
 * 封装了依赖项管理功能
 */
abstract class AbstractDependent : Dependent {
    /** 尚未绑定的依赖项集 */
    protected val dependencies = mutableListOf<AbstractDependency<*>>()

    /** 构造一个 [C] 类型的强依赖 */
    protected inline fun <reified C : Component> must() =
        AbstractDependency.Dependency(C::class).also { dependencies.add(it) }

    /** 构造一个 [C] 类型的强依赖 */
    protected inline fun <reified C : Component> maybe() =
        AbstractDependency.WeakDependency(C::class).also { dependencies.add(it) }

    override fun sync(dependency: Component) =
        dependencies.removeIf { it(dependency) != null } && dependencies.isEmpty()
}
