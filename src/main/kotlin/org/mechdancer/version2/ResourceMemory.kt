package org.mechdancer.version2

/**
 * 资源缓存
 * @param Parameter 参数类型
 * @param Resource  资源类型
 */
interface ResourceMemory<Parameter : Any, Resource : Any>
    : ResourceFactory<Parameter, Resource> {
    /**
     * 置入一项资源
     * @param parameter 对应的参数
     * @param resource  资源是不可空的，用输入空表示移除这项资源
     */
    operator fun set(parameter: Parameter, resource: Resource?)
}
