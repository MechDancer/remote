package org.mechdancer.version2

import org.mechdancer.version2.dependency.Dependency
import org.mechdancer.version2.dependency.FunctionModule
import org.mechdancer.version2.dependency.ResourceFactory
import org.mechdancer.version2.dependency.resource.basic.HostInfo

/**
 * 远程终端
 */
class RemoteHub(val name: String) {
    private val _resources = hashSetOf<ResourceFactory<*, *>>()
    private val _functions = hashSetOf<FunctionModule>()

    init {
        _resources.add(HostInfo(this))
    }

    /**
     * 浏览所有资源
     */
    val resources = object : Set<ResourceFactory<*, *>> by _resources {}

    /**
     * 浏览所有功能
     */
    val functions = object : Set<FunctionModule> by _functions {}

    /**
     * 加载插件
     */
    infix fun setup(dependency: Dependency) {
        when (dependency) {
            is ResourceFactory<*, *> -> _resources.add(dependency)
            is FunctionModule        -> _functions.add(dependency)
        }
    }

    /**
     * 同步所有依赖项
     */
    fun syncDependencies() {
        val dependencies = _resources + _functions
        _functions.forEach { it.loadDependencies(dependencies) }
    }
}
