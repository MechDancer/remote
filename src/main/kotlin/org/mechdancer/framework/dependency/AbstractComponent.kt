package org.mechdancer.framework.dependency

import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

/**
 * 抽象组件
 *   封装了默认的哈希函数和判等条件
 *   需要实现类提供自己的具体类型 [type]
 *   泛型 [T] 可保证此类型来自这个实现类
 */
abstract class AbstractComponent<T : AbstractComponent<T>>
    (private val type: KClass<T>) : Component {

    override fun equals(other: Any?) =
        this === other || type.safeCast(other) !== null

    override fun hashCode() =
        type.hashCode()
}
