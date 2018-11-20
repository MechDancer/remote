package org.mechdancer.version2

import org.mechdancer.version2.dependency.Dependency
import org.mechdancer.version2.dependency.FunctionModule.DependencyNotExistException

/**
 * 计算资源工厂的哈希值
 */
inline fun <reified D : Dependency> hashOf() =
    D::class.java.hashCode()

/**
 * 找到一种依赖项
 * @param R 依赖项类型
 */
inline fun <reified R : Dependency> Iterable<Dependency>.get(): List<R> =
    mapNotNull { it as? R }

/**
 * 找到一种依赖项
 * @param R 依赖项类型
 */
inline fun <reified R : Dependency> Iterable<Dependency>.maybe(): R? =
    mapNotNull { it as? R }.singleOrNull()

/**
 * 找到一种依赖项
 * @param R 依赖项类型
 */
inline fun <reified R : Dependency> Iterable<Dependency>.must(): R =
    mapNotNull { it as? R }.singleOrNull()
        ?: throw DependencyNotExistException(R::class)

/**
 * 向终端添加新的依赖项并立即扫描
 */
operator fun RemoteHub.plusAssign(dependency: Dependency) {
    setup(dependency)
    syncDependencies()
}

/**
 * 构造终端并扫描
 */
fun remoteHub(name: String, block: RemoteHub.() -> Unit) =
    RemoteHub(name).apply(block).apply { syncDependencies() }
