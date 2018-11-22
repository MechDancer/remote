package org.mechdancer.version2.dependency

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
inline fun <reified R : Dependency> DynamicScope.get(): List<R> =
    dependencies.mapNotNull { it as? R }

/**
 * 找到一种依赖项
 * @param R 依赖项类型
 */
inline fun <reified R : Dependency> DynamicScope.maybe(): R? =
    dependencies.mapNotNull { it as? R }.singleOrNull()

/**
 * 找到一种依赖项
 * @param R 依赖项类型
 */
inline fun <reified R : Dependency> DynamicScope.must(): R =
    dependencies.mapNotNull { it as? R }.singleOrNull()
        ?: throw DependencyNotExistException(R::class)

/**
 * 构建一个每次检查依赖项的代理
 */
inline fun <reified R : Dependency> maybe(crossinline block: () -> DynamicScope) =
    Maybe { block().maybe<R>() }

/**
 * 构建一个每次检查依赖项的代理
 */
inline fun <reified R : Dependency> must(crossinline block: () -> DynamicScope) =
    lazy { block().must<R>() }

/**
 * 向终端添加新的依赖项
 */
operator fun DynamicScope.plusAssign(dependency: Dependency) =
    setup(dependency)

/**
 * 构造终端并扫描
 */
fun buildScope(block: DynamicScope.() -> Unit) =
    DynamicScope().apply(block).apply { sync() }
