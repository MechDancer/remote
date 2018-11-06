package org.mechdancer.remote.core

import org.mechdancer.remote.core.RemoteHub.UdpCmd.*
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
 * @param commandReceived   收到通用 TCP
 */
class RemoteHub(
	name: String,
	netFilter: (NetworkInterface) -> Boolean,
	private val newMemberDetected: String.() -> Unit,
	private val broadcastReceived: Received,
	private val commandReceived: CallBack
) : Closeable {
	private val network: NetworkInterface
	// 默认套接字
	private val default: MulticastSocket
	// TCP监听
	private val server = ServerSocket(0)
	// 组成员列表
	private val group = ConcurrentHashMap<String, ConnectionInfo>()
	// 地址询问阻塞
	private val addressSignal = SignalBlocker()
	// 存活时间
	private val alive = AtomicLong(10000)
	// UDP 插件服务
	private val udpPlugins = mutableMapOf<Char, Received>()
	// TCP 插件服务
	private val tcpPlugins = mutableMapOf<Char, CallBack>()

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

	// 更新时间戳
	private fun updateGroup(sender: String) {
		val now = System.currentTimeMillis()
		group[sender] = group[sender]
			?.copy(stamp = now)
			?: ConnectionInfo(null, now).also { newMemberDetected(sender) }
	}

	// 发送组播报文
	private fun broadcast(cmd: Byte, payload: ByteArray = ByteArray(0)) =
		pack(cmd, name, payload)
			.let { DatagramPacket(it, it.size, ADDRESS) }
			.let(default::send)

	// 广播自己的IP地址
	private fun tcpAck() = broadcast(AddressAck.id, address.bytes)

	// 解析收到的IP地址
	private fun tcpParse(sender: String, payload: ByteArray) {
		val address = payload
			.let(::ByteArrayInputStream)
			.let(::DataInputStream)
			.use { InetSocketAddress(getByAddress(it.readNBytes(4)), it.readInt()) }
		group[sender] = group[sender]!!.copy(address = address)
		addressSignal.awake()
	}

	// 清除一个地址
	private fun clearAddress(sender: String) {
		group[sender]?.copy(address = null)?.let { info -> group[sender] = info }
	}

	/**
	 * 广播自己的名字
	 * 使得所有在线节点也广播自己的名字，从而得知完整的组列表
	 */
	fun yell() = broadcast(YellActive.id)

	/**
	 * 广播一包数据
	 */
	infix fun broadcast(msg: ByteArray) = broadcast(Broadcast.id, msg)

	/**
	 * 广播一包数据
	 * 用于插件服务
	 */
	fun broadcast(id: Char, msg: ByteArray) = broadcast(id.toByte(), msg)

	// 处理 UDP 包
	private fun processUdp(pack: ByteArray) =
		unpack(pack)
			.takeIf { it.second != name }
			?.also {
				val (cmd, sender, payload) = it
				// 更新时间
				updateGroup(sender)
				// 响应指令
				when (cmd.toUdpCmd()) {
					UdpCmd.YellActive -> broadcast(YellReply.id)
					UdpCmd.YellReply  -> Unit
					UdpCmd.AddressAsk -> if (name == String(payload)) tcpAck() else Unit
					UdpCmd.AddressAck -> tcpParse(sender, payload)
					UdpCmd.Broadcast  -> broadcastReceived(sender, payload)
					null              ->
						cmd.toChar()
							.takeIf(Char::isLetterOrDigit)
							?.let(udpPlugins::get)
							?.invoke(this@RemoteHub, sender, payload)
				}
			}

	/**
	 * 更新成员表
	 * @param timeout 以毫秒为单位的检查时间，方法最多阻塞这么长时间。
	 *                设置在区间 (0, 500) 的检查时间将导致方法不启动重新检查，仅仅发出响应请求。
	 *                同时此时间会覆盖之前设置的离线时间。
	 */
	fun refresh(timeout: Int): Set<String> {
		assert(timeout > 0)

		yell()
		if (timeout >= 500)
			NewMulticast().use {
				udpReceiveLoop(it, 256, timeout) { pack ->
					val (_, sender, _) = unpack(pack)
					if (sender != name) updateGroup(sender)
				}
			}
		aliveTime = timeout + 20L
		return members.keys
	}

	/**
	 * 监听并解析 UDP 包
	 * @param timeout    以毫秒为单位的超时时间，方法最多阻塞这么长时间。
	 *                   默认值为 0，指示超时时间无穷大。
	 *                   设置在区间 (0, 500) 的超时时间将导致方法立即返回。
	 * @param bufferSize 缓冲区大小，超过缓冲容量的数据包无法接收。
	 *                   默认值 65536 是 UDP 支持的最大包长度。
	 */
	operator fun invoke(
		timeout: Int = 0,
		bufferSize: Int = 65536
	) {
		assert(bufferSize in 0..65536)
		when (timeout) {
			in 1..499 -> Unit
			0         ->
				DatagramPacket(ByteArray(bufferSize), bufferSize)
					.apply(default::receive)
					.let { it.data.copyOfRange(0, it.length) }
					.let(::processUdp)
			else      -> {
				NewMulticast().use {
					udpReceiveLoop(it, bufferSize, timeout, ::processUdp)
				}
			}
		}
	}

	// 尝试连接一个远端 TCP 服务器
	private fun connect(name: String): Socket {
		while (true) {
			group[name]
				?.address
				?.also { ip ->
					runCatching { Socket(ip.address, ip.port) }
						.onFailure { clearAddress(name) }
						.onSuccess { return it }
				}
			broadcast(AddressAsk.id, name.toByteArray())
			addressSignal.block(1000)
		}
	}

	/**
	 * 通过 TCP 发送，并在传输完后立即返回
	 * @param name 目标终端名字
	 * @param msg  报文
	 */
	fun send(name: String, msg: ByteArray) =
		connect(name)
			.getOutputStream()
			.sendTcp(pack(TcpCmd.Call.id, this.name, msg))
			.close()

	// 通用远程调用
	private fun Socket.call(id: Byte, msg: ByteArray) =
		use {
			it.getOutputStream().sendTcp(pack(id, this@RemoteHub.name, msg))
			it.getInputStream().receiveTcp()
		}

	/**
	 * 通过 TCP 发送，并阻塞接收反馈
	 * @param name 目标终端名字
	 * @param msg  报文
	 */
	fun call(name: String, msg: ByteArray) =
		connect(name).call(TcpCmd.CallBack.id, msg)

	/**
	 * 调用 TCP 插件服务
	 */
	fun call(id: Char, name: String, msg: ByteArray) =
		connect(name).call(id.toByte(), msg)

	/**
	 * 监听并解析 TCP 包
	 */
	fun listen() =
		server
			.accept()
			.use { server ->
				val (type, sender, payload) =
					server
						.getInputStream()
						.receiveTcp()
						.let(::unpack)
				updateGroup(sender)
				fun reply(msg: ByteArray) = server.getOutputStream().sendTcp(msg)
				when (type.toTcpCmd()) {
					TcpCmd.Call     -> commandReceived(sender, payload)
					TcpCmd.CallBack -> commandReceived(sender, payload).let(::reply)
					null            ->
						type.toChar()
							.takeIf(Char::isLetterOrDigit)
							?.let(tcpPlugins::get)
							?.invoke(this, sender, payload)
							?.let(::reply)
							?: Unit
				}
			}

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

	init {
		// 选网
		network =
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
		default = NewMulticast()
		default.networkInterface = network
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
		CallBack(1)
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
		fun NewMulticast() = MulticastSocket(ADDRESS.port).apply { joinGroup(ADDRESS.address) }

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

		// 打包解包

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

		@JvmStatic
		fun unpack(pack: ByteArray) =
			pack.let(::ByteArrayInputStream)
				.let(::DataInputStream)
				.let {
					val type = it.readByte()
					val nameLength = it.readByte().toInt()
					val name = String(it.readNBytes(nameLength))
					val payload = it.readBytes()
					Triple(type, name, payload)
				}

		// TCP 收发

		@JvmStatic
		fun OutputStream.sendTcp(pack: ByteArray) =
			apply {
				DataOutputStream(this).writeInt(pack.size)
				write(pack)
			}

		@JvmStatic
		fun InputStream.receiveTcp(): ByteArray =
			readNBytes(DataInputStream(this).readInt())

		// UDP 接收循环

		@JvmStatic
		fun udpReceiveLoop(
			socket: DatagramSocket,
			bufferSize: Int,
			timeout: Int,
			block: (ByteArray) -> Any?) {
			val buffer = DatagramPacket(ByteArray(bufferSize), bufferSize)
			val endTime = System.currentTimeMillis() + timeout
			while (true) {
				// 设置超时时间
				socket.soTimeout =
					(endTime - System.currentTimeMillis())
						.toInt()
						.takeIf { it >= 500 }
					?: return
				// 接收，超时直接退出
				try {
					socket.receive(buffer)
				} catch (_: SocketTimeoutException) {
					return
				}
				block(buffer.data.copyOfRange(0, buffer.length))
			}
		}
	}
}
