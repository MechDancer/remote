package org.mechdancer.version2.dependency

/**
 * 依赖项
 */
interface Dependency {
    /**
     * 资源工厂只要类型相同就视作相同，务必重载
     */
    override fun equals(other: Any?): Boolean

    /**
     * 资源工厂的哈希值是其类型的哈希值，反射较慢，建议缓存
     */
    override fun hashCode(): Int
}
