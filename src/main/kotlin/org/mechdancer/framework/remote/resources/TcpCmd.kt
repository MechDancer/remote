package org.mechdancer.framework.remote.resources

/**
 * TCP 协议指令
 */
enum class TcpCmd(override val id: Byte) : Command {
    Mail(0),
    Dialog(1),
    COMMON(127);

    companion object {
        private val memory = Command.memoryOf<TcpCmd>()
        operator fun get(id: Byte) = memory[id]
    }
}
