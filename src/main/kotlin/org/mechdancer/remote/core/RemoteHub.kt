package org.mechdancer.remote.core

import java.io.*
import java.net.*
import java.net.InetAddress.getByAddress
import java.net.InetAddress.getByName
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * 远程终端
 *
 * 1. UDP 组播
 * 2. TCP 可靠传输
 *
 * 初始化参数
 * @param name      进程名
 * @param netFilter 自定义选网策略
 *
 * 回调参数
 * @param newMemberDetected 发现新成员
 * @param broadcastReceived 收到广播
 * @param commandReceived   收到 TCP
 */
class RemoteHub(
	name: String,
	netFilter: (NetworkInterface) -> Boolean,
	private val newMemberDetected: String.() -> Unit,
	private val broadcastReceived: RemoteHub.(String, ByteArray) -> Unit,
	private val commandReceived: RemoteHub.(String, ByteArray) -> ByteArray
) : Closeable {
	// 默认套接字
	private val default = MulticastSocket(ADDRESS.port)
	// TCP监听
	private val server = ServerSocket(0)
	// 组成员列表
	private val group = ConcurrentHashMap<String, ConnectionInfo>()
	// 地址询问阻塞
	private val addressSignal = SignalBlocker()
	// 存活时间
	private val alive = AtomicLong(10000)
	// UDP 插件服务
	private val udpPlugins = mutableMapOf<Char, RemoteHub.(String, ByteArray) -> Unit>()
	// TCP 插件服务
	private val tcpPlugins = mutableMapOf<Char, RemoteHub.(String, ByteArray) -> ByteArray>()

	/**
	 * 序列号
	 */
	val uuid: UUID = UUID.randomUUID()

	/**
	 * 终端名字
	 */
	val name: String

	/**
	 * 终端地址
	 */
	val address: InetSocketAddress

	/**
	 * 组成员列表
	 */
	val members: Map<String, InetSocketAddress?>
		get() {
			val now = System.currentTimeMillis()
			return group.toMap()
				.filterValues { now - it.stamp < aliveTime }
				.mapValues { it.value.address }
		}

	/**
	 * 存活时间条件
	 */
	var aliveTime: Long
		get() = alive.get()
		set(value) = alive.set(if (value <= 0) Long.MAX_VALUE else value)

	// 发送组播报文
	private fun send(cmd: Byte, payload: ByteArray = ByteArray(0)) =
		pack(cmd, name, payload)
			.let { DatagramPacket(it, it.size, ADDRESS) }
			.let(default::send)

	// 广播自己的IP地址
	private fun tcpAck() = send(UdpCmd.AddressAck.id, address.bytes)

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
			.use { InetSocketAddress(getByAddress(it.readNBytes(4)), it.readInt()) }
		group[sender] = group[sender]!!.copy(address = address)
		addressSignal.awake()
	}

	// 清除一个地址
	private fun clearAddress(sender: String) {
		group[sender]?.copy(address = null)?.let { info -> group.put(name, info) }
	}

	// 尝试连接一个远端 TCP 服务器
	private tailrec fun connect(name: String): Socket {
		val result = group[name]
			?.address
			?.let {
				runCatching { Socket(it.address, it.port) }
					.apply { onFailure { clearAddress(name) } }
					.getOrNull()
			}
		return if (result != null) result
		else {
			send(UdpCmd.AddressAsk.id, name.toByteArray())
			addressSignal.block(1000)
			connect(name)
		}
	}

	/**
	 * 广播自己的名字
	 * 使得所有在线节点也广播自己的名字，从而得知完整的组列表
	 */
	fun yell() = send(UdpCmd.YellActive.id)

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
	 * 调用 TCP 插件服务
	 */
	fun call(id: Char, name: String, msg: ByteArray) =
		connect(name).use {
			it.getOutputStream()
				.writePack(id.toByte(), this.name, msg)
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
	infix fun broadcast(msg: ByteArray) = send(UdpCmd.Broadcast.id, msg)

	/**
	 * 广播一包数据
	 * 用于插件服务
	 */
	fun broadcast(id: Char, msg: ByteArray) = send(id.toByte(), msg)

	/**
	 * 加载插件
	 */
	infix fun setup(plugin: RemotePlugin) {
		assert(plugin.id.isLetterOrDigit())
		when (plugin) {
			is BroadcastPlugin -> udpPlugins[plugin.id] = plugin::invoke
			is CallBackPlugin  -> tcpPlugins[plugin.id] = plugin::invoke
			else               -> throw RuntimeException("unknown plugin type")
		}

	}

	/**
	 * 停止所有功能，释放资源
	 * 调用此方法后再用终端进行收发操作将导致异常、阻塞或其他非预期的结果
	 */
	override fun close() {
		default.leaveGroup(ADDRESS.address)
		default.close()
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
		r.runCatching(s::receive)
			.onFailure { if (it is SocketTimeoutException) return else throw it }
		// 解包
		ByteArrayInputStream(r.data, 0, r.length)
			.readPack()
			.takeIf { it.second != name }
			?.let {
				val (type, sender, payload) = it
				// 更新时间
				updateGroup(sender)
				// 响应指令
				when (type.toUdpCmd()) {
					UdpCmd.YellActive -> send(UdpCmd.YellReply.id)
					UdpCmd.YellReply  -> Unit
					UdpCmd.AddressAsk -> if (name == String(payload)) tcpAck() else Unit
					UdpCmd.AddressAck -> parse(sender, payload)
					UdpCmd.Broadcast  -> broadcastReceived(sender, payload)
					null              ->
						type.toChar()
							.takeIf(Char::isLetterOrDigit)
							?.let(udpPlugins::get)
							?.invoke(this, sender, payload)
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
		yell()
		invoke(timeout)
		aliveTime = timeout + 500L // 调用 invoke 构造套接字通常耗费不少于 400ms
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
				fun reply(msg: ByteArray) =
					server.getOutputStream().writePack(TcpCmd.Back.id, name, msg)
				when (type.toTcpCmd()) {
					TcpCmd.Call     -> commandReceived(sender, payload)
					TcpCmd.CallBack -> commandReceived(sender, payload).let(::reply)
					TcpCmd.Back     -> Unit
					null            ->
						type.toChar()
							.takeIf(Char::isLetterOrDigit)
							?.let(tcpPlugins::get)
							?.invoke(this, sender, payload)
							?.let(::reply)
				}
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
		address = InetSocketAddress(
			network.inetAddresses.asSequence().first(),
			server.localPort
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

	/**
	 * 连接信息
	 * @param address 地址
	 * @param stamp   最后到达时间
	 */
	private data class ConnectionInfo(
		val address: InetSocketAddress?,
		val stamp: Long
	)

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
		val InetSocketAddress.bytes: ByteArray
			get() =
				ByteArrayOutputStream().apply {
					write(address.address)
					DataOutputStream(this).writeInt(port)
				}.toByteArray()

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
