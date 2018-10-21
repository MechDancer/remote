package org.mechdancer.remote.core

import java.io.*
import java.net.*
import java.util.*
import kotlin.math.abs

/**
 * 广播服务器
 * @param name 进程名
 */
class BroadcastHub(
	val name: String,
	netFilter: (NetworkInterface) -> Boolean,
	private val newProcessDetected: String.() -> Unit,
	private val broadcastReceived: BroadcastHub.(String, ByteArray) -> Unit,
	private val remoteProcess: (ByteArray) -> ByteArray
) {
	// 组播监听
	private val socket = MulticastSocket(ADDRESS.port)
	// TCP监听
	private val server = newServerSocket()
	// 组成员列表
	private val group = mutableMapOf<String, InetSocketAddress?>()
	// TCP挂起锁
	private val tcpLock = Object()

	/**
	 * 组成员列表
	 */
	val members get() = group.keys

	/**
	 * 本进程地址
	 */
	val tcpAddress
		get() = InetSocketAddress(
			socket.networkInterface.inetAddresses.asSequence().first(),
			server.localPort
		)

	// 发送组播报文
	private fun send(cmd: Cmd, payload: ByteArray = ByteArray(0)) =
		ByteArrayOutputStream()
			.apply {
				DataOutputStream(this).use {
					cmd.id.toInt().let(it::writeByte)
					name.length.let(it::writeByte)
					name.toByteArray().let(it::write)
					payload.let(it::write)
				}
			}
			.toByteArray()
			.let { DatagramPacket(it, it.size, ADDRESS) }
			.let(socket::send)

	// 广播自己的名字
	fun yell() = send(Cmd.YellActive)

	// 广播自己的IP地址
	private fun tcpAck() =
		ByteArrayOutputStream()
			.apply { ObjectOutputStream(this).writeObject(tcpAddress) }
			.toByteArray()
			.let { send(Cmd.TcpAck, it) }

	/**
	 * 远程调用
	 * 通过TCP传输，并阻塞接收反馈
	 */
	tailrec fun remoteCall(name: String, msg: ByteArray): ByteArray =
		group[name]
			?.let {
				try {
					Socket(it.address, it.port)
				} catch (e: IOException) {
					group[name] = null
					null
				}
			}
			?.run {
				getOutputStream().write(msg)
				shutdownOutput()
				getInputStream().readBytes().also { close() }
			}
			?: run {
				send(Cmd.TcpAsk, name.toByteArray())
				synchronized(tcpLock) { tcpLock.wait(1000) }
				null
			}
			?: remoteCall(name, msg)

	/**
	 * 广播一包数据
	 */
	infix fun broadcast(msg: ByteArray) = send(Cmd.Broadcast, msg)

	/**
	 * 监听并解析 UDP 包
	 */
	operator fun invoke(bufferSize: Int = 2048) {
		val receiver = DatagramPacket(ByteArray(bufferSize), bufferSize)

		tailrec fun receive(): String =
			receiver
				.apply(socket::receive)
				.let { String(it.data, 2, it.data[1].toInt()) }
				.takeUnless { it == name }
				?: receive()
		// 接收
		val senderName = receive()
		// 记录不认识的名字
		senderName
			.takeIf { it !in group }
			?.also { group[it] = null }
			?.also(newProcessDetected)
		// 解析负载
		val payload = receiver.data.copyOfRange(senderName.length + 2, receiver.length)
		// 响应指令
		when (receiver.data[0].toCmd()) {
			Cmd.YellActive -> send(Cmd.YellReply)
			Cmd.YellReply  -> Unit
			Cmd.TcpAsk     -> if (name == String(payload)) tcpAck()
			Cmd.TcpAck     -> {
				payload
					.let(::ByteArrayInputStream)
					.let(::ObjectInputStream)
					.use { group[senderName] = it.readObject() as InetSocketAddress }
				synchronized(tcpLock) { tcpLock.notifyAll() }
			}
			Cmd.Broadcast  -> broadcastReceived(senderName, payload)
			null           -> Unit
		}
	}

	/**
	 * 监听并解析 TCP 包
	 */
	fun listen() =
		server
			.accept()
			.apply {
				getInputStream()
					.readBytes()
					.let(remoteProcess)
					.let(getOutputStream()::write)
			}
			.close()

	init {
		assert(name.isNotBlank())
		// 选择一条靠谱的网络
		socket.networkInterface =
			NetworkInterface
				.getNetworkInterfaces()
				.asSequence()
				.filter { it.isUp }
				.filter { it.supportsMulticast() }
				.filter { !it.isVirtual }
				.filter(netFilter)
				.toList()
				.run {
					firstOrNull(::wlan)
						?: firstOrNull(::eth)
						?: firstOrNull { !it.isLoopback }
						?: first()
				}
		// 加入组播
		socket.joinGroup(ADDRESS.address)
	}

	// 指令 ID
	private enum class Cmd(val id: Byte) {
		YellActive(0),
		YellReply(1),
		TcpAsk(2),
		TcpAck(3),
		Broadcast(127)
	}

	private companion object {
		val ADDRESS = InetSocketAddress(InetAddress.getByName("238.88.88.88"), 23333)

		@JvmStatic
		fun Byte.toCmd() = Cmd.values().firstOrNull { it.id == this }

		@JvmStatic
		fun wlan(net: NetworkInterface) = net.name.startsWith("wlan")

		@JvmStatic
		fun eth(net: NetworkInterface) = net.name.startsWith("eth")

		@JvmStatic
		tailrec fun newServerSocket(): ServerSocket =
			Random()
				.nextInt(55536)
				.let {
					try {
						ServerSocket(10000 + abs(it))
					} catch (e: BindException) {
						null
					}
				}
				?: newServerSocket()
	}
}
