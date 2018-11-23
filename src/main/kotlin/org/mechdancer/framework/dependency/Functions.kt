package org.mechdancer.framework.dependency

import org.mechdancer.framework.dependency.FunctionModule.DependencyNotExistException

/**
 * 计算资源工厂的哈希值
 */
inline fun <reified D : Dependency> hashOf() =
    D::class.java.hashCode()

/**
 * 找到一种依赖项
 * @param D 依赖项类型
 */
inline fun <reified D : Dependency> DynamicScope.get(): List<D> =
    dependencies.mapNotNull { it as? D }

/**
 * 找到一种依赖项
 * @param D 依赖项类型
 */
inline fun <reified D : Dependency> DynamicScope.maybe(): D? =
    get<D>().singleOrNull()

/**
 * 找到一种依赖项
 * @param D 依赖项类型
 */
inline fun <reified D : Dependency> DynamicScope.must(): D =
    maybe() ?: throw DependencyNotExistException(D::class)

/**
 * 构建一个每次检查依赖项的代理
 */
inline fun <reified D : Dependency> maybe(crossinline block: () -> DynamicScope) =
    Maybe { block().maybe<D>() }

/**
 * 构建一个严格要求依赖项的代理
 */
inline fun <reified D : Dependency> must(crossinline block: () -> DynamicScope) =
    lazy { block().must<D>() }

/**
 * 向终端添加新的依赖项
 */
operator fun DynamicScope.plusAssign(dependency: Dependency) {
    setup(dependency)
}

/**
 * 构造终端并扫描
 */
fun scope(block: DynamicScope.() -> Unit) =
    DynamicScope().apply(block).apply { sync() }
