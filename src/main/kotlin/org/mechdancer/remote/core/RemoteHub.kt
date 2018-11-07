package org.mechdancer.remote.core

import org.mechdancer.remote.core.RemoteHub.UdpCmd.*
import java.io.*
import java.net.*
import java.net.InetAddress.getByAddress
import java.net.InetAddress.getByName
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
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
@Suppress("unused")
class RemoteHub(
	name: String,
	netFilter: (NetworkInterface) -> Boolean,
	private val newMemberDetected: String.() -> Unit,
	private val broadcastReceived: Received,
	private val commandReceived: CallBack
) : Closeable {
	// 默认套接字
	private val default: MulticastSocket
	// TCP监听
	private val server = ServerSocket(0)
	// 组成员列表
	private val group = ConcurrentHashMap<String, ConnectionInfo>()
	// 地址询问阻塞
	private val addressSignal = SignalBlocker()
	// 存活时间
	private val aliveTime = AtomicInteger(10000)
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
		get() = group.toMap()
			.filterValues { now - it.stamp < timeToLive }
			.mapValues { it.value.address }

	/**
	 * 存活时间条件
	 */
	var timeToLive: Int
		get() = aliveTime.get()
		set(value) = aliveTime.set(if (value <= 0) Int.MAX_VALUE else value)

	// 更新成员信息
	// 未知成员: 添加到表并初始化IP地址
	// 已知成员: 更新时间戳
	private fun updateGroup(sender: String, address: InetSocketAddress?) {
		val old = group[sender]
		if (old == null) {
			group[sender] = ConnectionInfo(now, address)
			newMemberDetected(sender)
		} else {
			group[sender] = old.copy(stamp = now)
		}
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

	/**
	 * 广播自己的名字
	 * 使得所有在线节点也广播自己的名字，从而得知完整的组列表
	 */
	fun yell() = broadcast(YellActive.id)

	/**
	 * 广播一包数据
	 * @param msg 数据报
	 */
	infix fun broadcast(msg: ByteArray) = broadcast(Broadcast.id, msg)

	/**
	 * 广播一包数据
	 * 用于插件服务
	 * @param id  插件识别号
	 * @param msg 数据报
	 */
	fun broadcast(id: Char, msg: ByteArray) = broadcast(id.toByte(), msg)

	// 处理 UDP 包
	private fun processUdp(pack: DatagramPacket) =
		unpack(pack.data.copyOfRange(0, pack.length))
			.takeIf { it.second != name }
			?.also {
				val (cmd, sender, payload) = it
				// 更新时间
				updateGroup(sender, InetSocketAddress(pack.address, pack.port))
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
	 *                此时间会覆盖之前设置的离线时间。
	 */
	fun refresh(timeout: Int): Set<String> {
		assert(timeout > 0)

		yell()
		multicastOn(null).use {
			udpReceiveLoop(it, 256, timeout) { pack ->
				val (_, sender, _) = unpack(pack.actualData)
				if (sender != name) updateGroup(sender, pack.actualAddress)
			}
		}
		timeToLive = (timeout * 1.5 + 20).toInt()
		return group.toMap().filterValues { now - it.stamp < timeToLive }.keys
	}

	/**
	 * 监听并解析 UDP 包
	 * @param timeout    以毫秒为单位的超时时间，方法最多阻塞这么长时间。
	 *                   默认值为 0，指示超时时间无穷大。
	 * @param bufferSize 缓冲区大小，超过缓冲容量的数据包无法接收。
	 *                   默认值 65536 是 UDP 支持的最大包长度。
	 */
	operator fun invoke(
		timeout: Int = 0,
		bufferSize: Int = 65536
	) {
		assert(timeout > 0)
		assert(bufferSize in 0..65536)
		when (timeout) {
			0    ->
				DatagramPacket(ByteArray(bufferSize), bufferSize)
					.apply(default::receive)
					.let(::processUdp)
			else -> {
				multicastOn(null).use {
					udpReceiveLoop(it, bufferSize, timeout, ::processUdp)
				}
			}
		}
	}

	// 尝试连接一个远端 TCP 服务器
	private fun connect(other: String): Socket {
		while (true) {
			group[other]
				?.address
				?.also { ip ->
					try {
						return Socket(ip.address, ip.port)
					} catch (_: SocketException) {
						group[other] = group[other]!!.copy(address = null)
					}
				}
			broadcast(AddressAsk.id, other.toByteArray())
			addressSignal.block(1000)
		}
	}

	/**
	 * 通过 TCP 发送，并在传输完后立即返回
	 * @param other 目标终端名字
	 * @param msg   报文
	 */
	fun send(other: String, msg: ByteArray) =
		connect(other)
			.getOutputStream()
			.sendTcp(pack(TcpCmd.Call.id, name, msg))
			.close()

	// 通用远程调用
	private fun Socket.call(id: Byte, msg: ByteArray) =
		use {
			it.getOutputStream().sendTcp(pack(id, name, msg))
			it.getInputStream().receiveTcp()
		}

	/**
	 * 通过 TCP 发送，并阻塞接收反馈
	 * @param other 目标终端名字
	 * @param msg   报文
	 */
	fun call(other: String, msg: ByteArray) =
		connect(other).call(TcpCmd.CallBack.id, msg)

	/**
	 * 调用 TCP 插件服务
	 */
	fun call(id: Char, other: String, msg: ByteArray) =
		connect(other).call(id.toByte(), msg)

	/**
	 * 监听并解析 TCP 包
	 */
	fun listen() =
		server
			.accept()
			.use { server ->
				val (cmd, sender, payload) =
					server
						.getInputStream()
						.receiveTcp()
						.let(::unpack)
				updateGroup(sender, null)
				fun reply(msg: ByteArray) = server.getOutputStream().sendTcp(msg)
				when (cmd.toTcpCmd()) {
					TcpCmd.Call     -> commandReceived(sender, payload)
					TcpCmd.CallBack -> commandReceived(sender, payload).let(::reply)
					null            ->
						cmd.toChar()
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
		val network = NetworkInterface
			.getNetworkInterfaces()
			.asSequence()
			.filter(NetworkInterface::isUp)
			.filter(NetworkInterface::supportsMulticast)
			.filter { it.inetAddresses.hasMoreElements() }
			.filter(netFilter)
			.toList()
			.run {
				firstOrNull(::wlan)
					?: firstOrNull(::eth)
					?: firstOrNull { !it.isLoopback }
					?: first()
					?: throw RuntimeException("no available network")
			}
		address = InetSocketAddress(
			network.inetAddresses.asSequence().first(),
			server.localPort
		)
		// 定名
		this.name = name.takeIf { it.isNotBlank() } ?: "Hub[$address]"
		// 入组
		default = multicastOn(network)
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
	 * @param stamp   最后到达时间
	 * @param address 地址
	 */
	private data class ConnectionInfo(
		val stamp: Long,
		val address: InetSocketAddress?
	)

	private companion object {
		@JvmStatic
		val now
			get() = System.currentTimeMillis()

		val ADDRESS = InetSocketAddress(getByName("238.88.88.88"), 23333)

		@JvmStatic
		fun multicastOn(net: NetworkInterface?) =
			MulticastSocket(ADDRESS.port).apply {
				net?.let(this::setNetworkInterface)
				joinGroup(ADDRESS.address)
			}

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
			cmd: Byte,
			sender: String,
			payload: ByteArray
		): ByteArray =
			ByteArrayOutputStream().apply {
				DataOutputStream(this).apply {
					writeByte(cmd.toInt())
					writeByte(sender.length)
					writeBytes(sender)
				}
				write(payload)
			}.toByteArray()

		@JvmStatic
		fun unpack(pack: ByteArray) =
			pack.let(::ByteArrayInputStream)
				.let(::DataInputStream)
				.let {
					val cmd = it.readByte()
					val senderLength = it.readByte().toInt()
					val sender = String(it.readNBytes(senderLength))
					val payload = it.readBytes()
					Triple(cmd, sender, payload)
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
		val DatagramPacket.actualData
			get() = data.copyOfRange(0, length)

		@JvmStatic
		val DatagramPacket.actualAddress
			get() = InetSocketAddress(address, port)

		@JvmStatic
		fun udpReceiveLoop(
			socket: DatagramSocket,
			bufferSize: Int,
			timeout: Int,
			block: (DatagramPacket) -> Any?) {
			val buffer = DatagramPacket(ByteArray(bufferSize), bufferSize)
			val endTime = System.currentTimeMillis() + timeout
			while (true) {
				// 设置超时时间
				socket.soTimeout =
					(endTime - System.currentTimeMillis())
						.toInt()
						.takeIf { it > 0 }
					?: return
				// 接收，超时直接退出
				try {
					socket.receive(buffer)
				} catch (_: SocketTimeoutException) {
					return
				}
				block(buffer)
			}
		}
	}
}
