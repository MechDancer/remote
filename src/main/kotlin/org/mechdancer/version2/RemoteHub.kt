package org.mechdancer.version2

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
     * 动态添加一项新的资源
     * @return 是否作为新资源添加到终端
     *         若资源已存在，返回 `false`
     */
    fun addResource(resourceFactory: ResourceFactory<*, *>) =
        _resources.add(resourceFactory)

    /**
     * 动态添加一项新的功能
     * @return 是否作为新功能添加到终端
     *         若功能已存在，返回 `false`
     */
    fun addFunction(function: FunctionModule) =
        _functions.add(function)

    /**
     * 同步所有依赖项
     */
    fun syncDependencies() {
        val dependencies = _resources + _functions
        _functions.forEach { it.loadDependencies(dependencies) }
    }
}
