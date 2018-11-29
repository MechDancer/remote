package org.mechdancer.framework.dependency

import kotlin.reflect.KClass

/**
 * 功能模块
 */
interface FunctionModule : Dependency {
    /**
     * 加入动态域
     */
    infix fun onSetup(dependencies: Set<Dependency>)

    /**
     * 重新同步依赖项
     */
    fun sync()

    /**
     * 依赖项不存在
     */
    class DependencyNotExistException(which: KClass<out Dependency>) :
        RuntimeException("cannot find this dependency: ${which.qualifiedName}")
}
