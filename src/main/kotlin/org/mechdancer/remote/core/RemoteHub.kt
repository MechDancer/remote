package org.mechdancer.remote.core

import java.io.*
import java.net.*
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.min

/**
 * 远程终端
 *
 * 1. UDP 组播
 * 2. TCP 可靠传输
 * 3. RMI Java 远程调用
 *
 * @param name 进程名
 */
class RemoteHub<out T : Remote>(
	val name: String,
	netFilter: (NetworkInterface) -> Boolean,
	private val newProcessDetected: String.() -> Unit,
	private val broadcastReceived: RemoteHub<T>.(String, ByteArray) -> Unit,
	private val remoteProcess: RemoteHub<T>.(String, ByteArray) -> ByteArray,
	private val rmiRemote: T?
) {
	// 组播监听
	private val socket = MulticastSocket(ADDRESS.port)
	// TCP监听
	private val server = newServerSocket()
	// RMI服务
	private val registry = rmiRemote?.let { newRegistry() }
	// 组成员列表
	private val group = mutableMapOf<String, HubAddress?>()
	// TCP挂起锁
	private val tcpLock = Object()

	/**
	 * 组成员列表k
	 */
	val members get() = group.keys

	/**
	 * 本进程地址
	 */
	val hubAddress
		get() = HubAddress(
			socket.networkInterface.inetAddresses.asSequence().first(),
			server.localPort,
			registry?.first ?: 0
		)

	// 发送组播报文
	private fun send(cmd: UdpCmd, payload: ByteArray = ByteArray(0)) =
		pack(cmd.id, name, payload)
			.let { DatagramPacket(it, it.size, ADDRESS) }
			.let(socket::send)

	// 广播自己的IP地址
	private fun tcpAck() =
		ByteArrayOutputStream()
			.apply { write(hubAddress.bytes) }
			.toByteArray()
			.let { send(UdpCmd.TcpAck, it) }

	// 解析收到的IP地址
	private fun parseTcp(sender: String, payload: ByteArray) =
		payload
			.let(::ByteArrayInputStream)
			.let {
				val address = it.readNBytes(4).let(InetAddress::getByAddress)
				val stream = DataInputStream(it)
				HubAddress(
					address,
					stream.readInt(),
					stream.readInt()
				)
			}
			.let { group[sender] = it }
			.also { synchronized(tcpLock) { tcpLock.notifyAll() } }

	// 尝试连接一个远端 TCP 服务器
	private tailrec fun connect(name: String): Socket {
		val result = group[name]
			?.let {
				try {
					Socket(it.address, it.tcpPort)
				} catch (e: IOException) {
					group[name] = null
					null
				}
			}
		return if (result != null) result
		else {
			send(UdpCmd.TcpAsk, name.toByteArray())
			synchronized(tcpLock) { tcpLock.wait(1000) }
			connect(name)
		}
	}

	/**
	 * 广播自己的名字
	 * 使得所有在线节点也广播自己的名字，从而得知完整的组列表
	 */
	fun yell() = send(UdpCmd.YellActive)

	/**
	 * 远程调用
	 * 通过TCP传输，并在传输完后立即返回
	 */
	fun remoteCall(name: String, msg: ByteArray) =
		connect(name).use {
			it.getOutputStream()
				.writePack(TcpCmd.Call.id, name, msg)
		}

	/**
	 * 远程调用
	 * 通过TCP传输，并阻塞接收反馈
	 */
	fun remoteCallBack(name: String, msg: ByteArray) =
		connect(name).use {
			it.getOutputStream()
				.writePack(TcpCmd.CallBack.id, name, msg)
			it.shutdownOutput()
			it.getInputStream()
				.readPack()
				.let { pack ->
					assert(pack.second == name)
					pack.third
				}
		}

	/**
	 * 广播一包数据
	 */
	infix fun broadcast(msg: ByteArray) = send(UdpCmd.Broadcast, msg)

	/**
	 * 尝试连接一个远程调用服务
	 */
	tailrec fun <U : Remote> getRegistry(name: String): U {
		val result = group[name]
			?.let {
				try {
					LocateRegistry
						.getRegistry(it.address.hostAddress, it.rmiPort)
						.lookup(name)
				} catch (e: RemoteException) {
					println(e.detail)
					group[name] = null
					null
				}
			}
		@Suppress("UNCHECKED_CAST")
		return if (result != null) result as U
		else {
			send(UdpCmd.TcpAsk, name.toByteArray())
			synchronized(tcpLock) { tcpLock.wait(1000) }
			getRegistry(name)
		}
	}

	/**
	 * 监听并解析 UDP 包
	 */
	operator fun invoke(bufferSize: Int = 2048) {
		val receiver = DatagramPacket(ByteArray(bufferSize), bufferSize)
		tailrec fun receive(): Triple<Byte, String, ByteArray> =
			receiver
				.apply(socket::receive)
				.data
				.copyOf(receiver.length)
				.let(::ByteArrayInputStream)
				.readPack()
				.takeIf { it.second != name }
				?: receive()
		// 接收
		val (type, sender, payload) = receive()
		// 记录不认识的名字
		sender
			.takeUnless(group::containsKey)
			?.also { group[it] = null }
			?.also(newProcessDetected)
		// 响应指令
		when (type.toUdpCmd()) {
			UdpCmd.YellActive -> send(UdpCmd.YellReply)
			UdpCmd.YellReply  -> Unit
			UdpCmd.TcpAsk     -> if (name == String(payload)) tcpAck()
			UdpCmd.TcpAck     -> parseTcp(sender, payload)
			UdpCmd.Broadcast  -> broadcastReceived(sender, payload)
			null              -> Unit
		}
	}

	/**
	 * 监听并解析 TCP 包
	 */
	fun listen() =
		server
			.accept()
			.use { server ->
				val (type, sender, payload) = server.getInputStream().readPack()
				remoteProcess(sender, payload)
					.takeIf { type.toTcpCmd() == TcpCmd.CallBack }
					?.let { server.getOutputStream().writePack(TcpCmd.Back.id, name, it) }
			}

	init {
		assert(name.isNotBlank())
		// 选择一条靠谱的网络
		socket.networkInterface =
			NetworkInterface
				.getNetworkInterfaces()
				.asSequence()
				.filter(NetworkInterface::isUp)
				.filter(NetworkInterface::supportsMulticast)
				.filterNot(NetworkInterface::isVirtual)
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
		// 注册远程服务
		registry?.second?.rebind(name, rmiRemote)
	}

	// 指令 ID
	private enum class UdpCmd(val id: Byte) {
		YellActive(0),
		YellReply(1),
		TcpAsk(2),
		TcpAck(3),
		Broadcast(127)
	}

	// 指令 ID
	private enum class TcpCmd(val id: Byte) {
		Call(0),
		CallBack(1),
		Back(2)
	}

	private companion object {
		val ADDRESS = InetSocketAddress(InetAddress.getByName("238.88.88.88"), 23333)

		@JvmStatic
		fun Byte.toUdpCmd() = UdpCmd.values().firstOrNull { it.id == this }

		@JvmStatic
		fun Byte.toTcpCmd() = TcpCmd.values().firstOrNull { it.id == this }

		@JvmStatic
		fun wlan(net: NetworkInterface) = net.name.startsWith("wlan")

		@JvmStatic
		fun eth(net: NetworkInterface) = net.name.startsWith("eth")

		@JvmStatic
		fun newPort() = Random().nextInt(55536).absoluteValue + 10000

		@JvmStatic
		tailrec fun newServerSocket(): ServerSocket =
			try {
				ServerSocket(newPort())
			} catch (e: BindException) {
				null
			} ?: newServerSocket()

		@JvmStatic
		tailrec fun newRegistry(): Pair<Int, Registry> =
			try {
				val port = newPort()
				port to LocateRegistry.createRegistry(port)
			} catch (e: BindException) {
				null
			} ?: newRegistry()

		@JvmStatic
		@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
		@Deprecated(message = "only for who under jdk11")
		fun InputStream.readNBytes(len: Int): ByteArray {
			val count = min(available(), len)
			val buffer = ByteArray(count)
			read(buffer, 0, count)
			return buffer
		}

		// 打包一包
		@JvmStatic
		fun pack(
			type: Byte,
			name: String,
			payload: ByteArray
		): ByteArray =
			ByteArrayOutputStream().apply {
				DataOutputStream(this).apply {
					writeByte(type.toInt())
					writeByte(name.length)
					writeBytes(name)
				}
				write(payload)
			}.toByteArray()

		// 把一包写入输出流
		@JvmStatic
		fun OutputStream.writePack(
			type: Byte,
			name: String,
			payload: ByteArray
		) = write(pack(type, name, payload))

		//从输入流读取一包
		@JvmStatic
		fun InputStream.readPack() =
			DataInputStream(this).let {
				val type = it.readByte()
				val length = it.readByte().toInt()
				val name = String(it.readNBytes(length))
				val payload = it.readBytes()
				Triple(type, name, payload)
			}
	}
}
