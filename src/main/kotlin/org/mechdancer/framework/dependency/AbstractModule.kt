package org.mechdancer.framework.dependency

/**
 * 抽象功能模块
 */
abstract class AbstractModule : FunctionModule {
    private lateinit var _host: DynamicScope

    protected val host = { _host }

    override fun onSetup(host: DynamicScope) {
        this._host = host
        sync()
    }

    override fun sync() = Unit

    // 默认的抽象功能可能与任何其他功能共存，用接口来标识其真实含义

    override fun equals(other: Any?) = this === other
    override fun hashCode() = javaClass.hashCode()
}