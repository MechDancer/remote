package org.mechdancer.version2.dependency

import kotlin.reflect.KClass

/**
 * 功能模块
 */
interface FunctionModule : Dependency {
    /**
     * 浏览资源依赖项集
     */
    val dependencies: Set<Dependency>

    /**
     * 加载全部依赖项
     */
    @Throws(DependencyNotExistException::class)
    fun loadDependencies(dependency: Iterable<Dependency>)

    /**
     * 依赖项不存在
     */
    class DependencyNotExistException(which: KClass<out Dependency>) :
        RuntimeException("cannot this dependency: ${which.qualifiedName}")
}
