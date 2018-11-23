package org.mechdancer.framework.dependency

import kotlin.reflect.KClass

/**
 * 功能模块
 */
interface FunctionModule : Dependency {
    /**
     * 加载终端
     */
    infix fun onSetup(host: DynamicScope)

    /**
     * 重新同步依赖项
     */
    fun sync()

    /**
     * 依赖项不存在
     */
    class DependencyNotExistException(which: KClass<out Dependency>) :
        RuntimeException("cannot this dependency: ${which.qualifiedName}")
}
