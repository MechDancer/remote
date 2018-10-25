package org.mechdancer.remote.core

import org.mechdancer.remote.readNBytes
import java.io.*
import java.net.*
import java.util.*
import kotlin.math.abs

/**
 * 广播服务器
 * @param name 进程名
 */
class RemoteHub(
	val name: String,
	netFilter: (NetworkInterface) -> Boolean,
	private val newProcessDetected: String.() -> Unit,
	private val broadcastReceived: RemoteHub.(String, ByteArray) -> Unit,
	private val remoteProcess: RemoteHub.(String, ByteArray) -> ByteArray
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
	private fun send(cmd: UdpCmd, payload: ByteArray = ByteArray(0)) =
		PackageIO(cmd.id, name, payload)
			.toByteArray()
			.let { DatagramPacket(it, it.size, ADDRESS) }
			.let(socket::send)

	// 广播自己的名字
	fun yell() = send(UdpCmd.YellActive)

	// 广播自己的IP地址
	private fun tcpAck() =
		ByteArrayOutputStream()
			.apply {
				write(tcpAddress.address.address)
				DataOutputStream(this).writeInt(tcpAddress.port)
			}
			.toByteArray()
			.let { send(UdpCmd.TcpAck, it) }

	// 解析收到的IP地址
	private fun parseTCP(pack: PackageIO) =
		pack.payload
			.let(::ByteArrayInputStream)
			.let { it.readNBytes(4).let(InetAddress::getByAddress) to DataInputStream(it).readInt() }
			.let { group[pack.name] = InetSocketAddress(it.first, it.second) }
			.also { synchronized(tcpLock) { tcpLock.notifyAll() } }

	// 尝试连接一个远端 TCP 服务器
	private tailrec fun connect(name: String): Socket {
		val result = group[name]
			?.let {
				try {
					Socket(it.address, it.port)
				} catch (e: IOException) {
					group[name] = null
					null
				}
			}
		return if (result == null) {
			send(UdpCmd.TcpAsk, name.toByteArray())
            synchronized(tcpLock) { tcpLock.wait(1000) }
			connect(name)
		} else result
	}

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
				.let { pack -> pack.name to pack.payload }
		}

	/**
	 * 广播一包数据
	 */
	infix fun broadcast(msg: ByteArray) = send(UdpCmd.Broadcast, msg)

	/**
	 * 监听并解析 UDP 包
	 */
	operator fun invoke(bufferSize: Int = 2048) {
		val receiver = DatagramPacket(ByteArray(bufferSize), bufferSize)
		tailrec fun receive(): PackageIO =
			receiver
				.apply(socket::receive)
				.data
				.copyOf(receiver.length)
				.let(::ByteArrayInputStream)
				.readPack()
				.takeIf { it.name != name }
				?: receive()
		// 接收
		val pack = receive()
		// 记录不认识的名字
		pack.name
			.takeUnless(group::containsKey)
			?.also { group[it] = null }
			?.also(newProcessDetected)
		// 响应指令
		when (pack.type.toUdpCmd()) {
			UdpCmd.YellActive -> send(UdpCmd.YellReply)
			UdpCmd.YellReply  -> Unit
			UdpCmd.TcpAsk     -> if (name == String(pack.payload)) tcpAck()
			UdpCmd.TcpAck     -> parseTCP(pack)
			UdpCmd.Broadcast  -> broadcastReceived(pack.name, pack.payload)
			null              -> Unit
		}
	}

	/**
	 * 监听并解析 TCP 包
	 */
	fun listen() =
		server
			.accept()
			.apply {
				val pack = getInputStream().readPack()
				remoteProcess(pack.name, pack.payload)
					.takeIf { pack.type.toTcpCmd() == TcpCmd.CallBack }
					?.let { getOutputStream().writePack(TcpCmd.Back.id, name, it) }
			}
			.close()

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
