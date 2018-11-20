package org.mechdancer.version2

import org.mechdancer.version2.dependency.Dependency
import org.mechdancer.version2.dependency.FunctionModule

/**
 * 依赖管理器
 */
class Hub {
    private val _dependencies = hashSetOf<Dependency>()

    /**
     * 浏览所有依赖项
     */
    val dependenies = object : Set<Dependency> by _dependencies {}

    /**
     * 加载插件
     */
    infix fun setup(dependency: Dependency) {
        _dependencies.add(dependency)
        (dependency as? FunctionModule)?.onSetup(this)
    }

    /**
     * 重新同步依赖项
     */
    fun sync() {
        _dependencies
            .mapNotNull { it as? FunctionModule }
            .forEach { it.sync() }
    }
}
