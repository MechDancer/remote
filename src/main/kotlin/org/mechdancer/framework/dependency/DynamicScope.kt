package org.mechdancer.framework.dependency

/**
 * 动态域
 * 一个动态域里有共享的资源和相互依赖的功能
 */
class DynamicScope {
    private val _dependencies = hashSetOf<Dependency>()

    /**
     * 浏览所有依赖项
     */
    val dependencies = object : Set<Dependency> by _dependencies {}

    /**
     * 加载一个新的依赖项
     * @param dependency 依赖项
     * @return 是否加载成功
     */
    infix fun setup(dependency: Dependency) =
        _dependencies
            .add(dependency)
            .also { (dependency as? FunctionModule)?.onSetup(this) }

    /**
     * 重新同步依赖项
     */
    fun sync() {
        _dependencies
            .mapNotNull { it as? FunctionModule }
            .forEach { it.sync() }
    }
}
