package org.mechdancer.version2.dependency

/**
 * 资源缓存
 * @param Parameter 参数类型
 * @param Resource  资源类型
 */
interface ResourceMemory<Parameter : Any, Resource : Any>
    : ResourceFactory<Parameter, Resource> {
    /**
     * 更新一项资源
     * @param parameter 对应的参数
     * @param resource  资源是不可空的，用输入空表示移除这项资源
     * @return 参数对应资源之前的值，之前不存在返回空
     */
    fun update(parameter: Parameter, resource: Resource?): Resource?
}
