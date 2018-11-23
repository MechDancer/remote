package org.mechdancer.framework.dependency

/**
 * 依赖项
 */
interface Dependency {
    /**
     * 此函数将控制依赖项互斥性，务必重写
     */
    override fun equals(other: Any?): Boolean

    /**
     * 建议使用依赖项类型的哈希作为依赖项的哈希
     */
    override fun hashCode(): Int
}
