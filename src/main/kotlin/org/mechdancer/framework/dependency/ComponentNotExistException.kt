package org.mechdancer.framework.dependency

import kotlin.reflect.KClass

/**
 * 组件不存在异常
 * @param which 要找的组件类型
 */
class ComponentNotExistException(which: KClass<out Component>) :
    RuntimeException("cannot find this dependency: ${which.qualifiedName}")