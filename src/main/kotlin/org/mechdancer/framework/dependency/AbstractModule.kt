package org.mechdancer.framework.dependency

/**
 * 抽象功能模块
 */
abstract class AbstractModule : FunctionModule {
    protected lateinit var host: DynamicScope

    override fun onSetup(host: DynamicScope) {
        this.host = host
        sync()
    }

    override fun sync() = Unit
}
