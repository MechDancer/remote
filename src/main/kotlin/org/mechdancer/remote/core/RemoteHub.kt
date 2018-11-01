package org.mechdancer.remote.core

import java.io.*
import java.net.*
import java.net.InetAddress.getByAddress
import java.net.InetAddress.getByName
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * 远程终端
 *
 * 1. UDP 组播
 * 2. TCP 可靠传输
 * 3. RMI Java 远程调用
 *
 * 初始化参数
 * @param name      进程名
 * @param netFilter 自定义选网策略
 *
 * 回调参数
 * @param newMemberDetected 发现新成员
 * @param broadcastReceived 收到广播
 * @param commandReceived   收到 TCP
 * @param rmiRemote         远程过程实现
 */
class RemoteHub<out T : Remote>(
	name: String,
	netFilter: (NetworkInterface) -> Boolean,
	private val newMemberDetected: String.() -> Unit,
	private val broadcastReceived: RemoteHub<T>.(String, ByteArray) -> Unit,
	private val commandReceived: RemoteHub<T>.(String, ByteArray) -> ByteArray,
	private val rmiRemote: T?
) : Closeable {
	// 默认套接字
	private val default = MulticastSocket(ADDRESS.port)
	// TCP监听
	private val server = newServerSocket()
	// RMI服务
	private val registry = rmiRemote?.let { newRegistry() }
	// 组成员列表
	private val group = ConcurrentHashMap<String, ConnectionInfo>()
	// 地址询问阻塞
	private val addressSignal = SignalBlocker()
	// 存活时间
	private var aliveTime = 10000

	/**
	 * 终端名字
	 */
	val name: String

	/**
	 * 终端地址
	 */
	val address: HubAddress

	/**
	 * 组成员列表
	 */
	val members: Map<String, HubAddress?>
		get() {
			val now = System.currentTimeMillis()
			return group.toMap()
				.filterValues { now - it.stamp < aliveTime }
				.mapValues { it.value.address }
		}

	// 发送组播报文
	private fun send(cmd: UdpCmd, payload: ByteArray = ByteArray(0)) =
		pack(cmd.id, name, payload)
			.let { DatagramPacket(it, it.size, ADDRESS) }
			.let(default::send)

	// 广播自己的IP地址
	private fun tcpAck() = send(UdpCmd.AddressAck, address.bytes)

	// 更新时间戳
	private fun updateGroup(sender: String) {
		val now = System.currentTimeMillis()
		group[sender] = group[sender]
			?.copy(stamp = now)
			?: ConnectionInfo(null, now).also { newMemberDetected(sender) }
	}

	// 解析收到的IP地址
	private fun parse(sender: String, payload: ByteArray) {
		val address = payload
			.let(::ByteArrayInputStream)
			.let(::DataInputStream)
			.use { stream ->
				HubAddress(
					getByAddress(stream.readNBytes(4)),
					stream.readInt(),
					stream.readInt()
				)
			}
		group[sender] = group[sender]!!.copy(address = address)
		addressSignal.awake()
	}

	// 尝试连接一个远端 TCP 服务器
	private tailrec fun connect(name: String): Socket {
		val result = group[name]
			?.address
			?.let {
				try {
					Socket(it.address, it.tcpPort)
				} catch (e: IOException) {
					group[name]?.copy(address = null)?.let { info -> group.put(name, info) }
					null
				}
			}
		return if (result != null) result
		else {
			send(UdpCmd.AddressAsk, name.toByteArray())
			addressSignal.block(1000)
			connect(name)
		}
	}

	/**
	 * 广播自己的名字
	 * 使得所有在线节点也广播自己的名字，从而得知完整的组列表
	 */
	fun yell() = send(UdpCmd.YellActive)

	/**
	 * 通过 TCP 发送，并在传输完后立即返回
	 */
	fun send(name: String, msg: ByteArray) =
		connect(name).use {
			it.getOutputStream()
				.writePack(TcpCmd.Call.id, this.name, msg)
		}

	/**
	 * 通过 TCP 发送，并阻塞接收反馈
	 */
	fun call(name: String, msg: ByteArray) =
		connect(name).use {
			it.getOutputStream()
				.writePack(TcpCmd.CallBack.id, this.name, msg)
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
	tailrec fun <U : Remote> connect(name: String): U {
		val result = group[name]
			?.address
			?.let {
				try {
					LocateRegistry
						.getRegistry(it.address.hostAddress, it.rmiPort)
						.lookup(name)
				} catch (e: RemoteException) {
					println(e.detail)
					null
				}
			}
		@Suppress("UNCHECKED_CAST")
		return if (result != null) result as U
		else {
			send(UdpCmd.AddressAsk, name.toByteArray())
			addressSignal.block(1000)
			connect<U>(name)
		}
	}

	/**
	 * 开始响应 RMI
	 */
	fun startRMI() {
		registry
			?.second
			?.takeIf { it.list().isEmpty() }
			?.rebind(name, rmiRemote)
	}

	/**
	 * 停止 RMI 服务
	 * @param force 是否允许强制立即中止正在运行的线程
	 */
	fun stopRMI(force: Boolean = false) {
		registry
			?.second
			?.takeIf { it.list().isNotEmpty() }
			?.unbind(name)
			?.also { UnicastRemoteObject.unexportObject(rmiRemote, force) }
	}

	/**
	 * 停止所有功能，释放资源
	 * 调用此方法后再用终端进行收发操作将导致异常、阻塞或其他非预期的结果
	 */
	override fun close() {
		stopRMI()
		default.leaveGroup(ADDRESS.address)
		server.close()
	}

	// 接收并解析 UDP 包
	// s : socket   接收用套接字
	// r : receiver 接收缓冲区
	// e : end      结束时间
	// l : loop     是否循环到时间用尽
	private tailrec fun receive(
		s: DatagramSocket,
		r: DatagramPacket,
		e: Long,
		l: Boolean
	) {
		// 设置超时时间
		s.soTimeout = (e - System.currentTimeMillis())
			.let {
				when {
					it > Int.MAX_VALUE -> 0
					it > 0             -> it.toInt()
					else               -> return
				}
			}
		// 接收，超时直接退出
		try {
			r.apply(s::receive)
		} catch (_: SocketTimeoutException) {
			return
		}
			.data
			.copyOf(r.length)
			.let(::ByteArrayInputStream)
			.readPack()
			.takeIf { it.second != name }
			?.let {
				val (type, sender, payload) = it
				// 更新时间
				updateGroup(sender)
				// 响应指令
				when (type.toUdpCmd()) {
					UdpCmd.YellActive -> send(UdpCmd.YellReply)
					UdpCmd.YellReply  -> Unit
					UdpCmd.AddressAsk -> if (name == String(payload)) tcpAck()
					UdpCmd.AddressAck -> parse(sender, payload)
					UdpCmd.Broadcast  -> broadcastReceived(sender, payload)
					null              -> Unit
				}
			}
		return if (l) receive(s, r, e, l) else Unit
	}

	/**
	 * 更新成员表
	 *
	 * 方法将阻塞 [timeout] ms
	 * 在此时段内未出现的终端视作已离线
	 *
	 * @param timeout 检查时间，单位毫秒
	 */
	fun refresh(timeout: Int): Set<String> {
		assert(timeout > 100)
		aliveTime = max(1000, timeout)
		yell()
		invoke(timeout)
		return members.keys
	}

	/**
	 * 监听并解析 UDP 包
	 *
	 * @param timeout    超时时间，单位毫秒，方法最多阻塞这么长时间
	 * @param bufferSize 缓冲区大小，超过缓冲容量的数据包无法接收
	 */
	operator fun invoke(
		timeout: Int = 0,
		bufferSize: Int = 2048
	) {
		when (timeout) {
			0    -> receive(
				default,
				DatagramPacket(ByteArray(bufferSize), bufferSize),
				Long.MAX_VALUE,
				false
			)
			else -> MulticastSocket(ADDRESS.port)
				.apply {
					networkInterface = default.networkInterface
					joinGroup(ADDRESS.address)
				}
				.use {
					receive(
						it,
						DatagramPacket(ByteArray(bufferSize), bufferSize),
						System.currentTimeMillis() + timeout,
						true
					)
				}
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
				updateGroup(sender)
				commandReceived(sender, payload)
					.takeIf { type.toTcpCmd() == TcpCmd.CallBack }
					?.let { server.getOutputStream().writePack(TcpCmd.Back.id, name, it) }
			}

	init {
		// 选网
		val network =
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
		address = HubAddress(
			network.inetAddresses.asSequence().first(),
			server.localPort,
			registry?.first ?: 0
		)
		// 定名
		this.name = name.takeIf { it.isNotBlank() } ?: "Hub[$address]"
		// 入组
		default.networkInterface = network
		default.joinGroup(ADDRESS.address)
	}

	// 指令 ID
	private enum class UdpCmd(val id: Byte) {
		YellActive(0),
		YellReply(1),
		AddressAsk(2),
		AddressAck(3),
		Broadcast(127)
	}

	// 指令 ID
	private enum class TcpCmd(val id: Byte) {
		Call(0),
		CallBack(1),
		Back(2)
	}

	private companion object {
		val ADDRESS = InetSocketAddress(getByName("238.88.88.88"), 23333)

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
		@Deprecated(message = "only for who is below jdk11")
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
