package org.mechdancer.framework.dependency

/**
 * 资源工厂
 *
 * @param Parameter 参数类型
 * @param Resource  资源类型
 */
interface ResourceFactory
<Parameter : Any, Resource : Any>
    : Dependency {
    /**
     * 获取资源
     * @return 构造或从缓存中选取资源
     */
    operator fun get(parameter: Parameter): Resource?
}
