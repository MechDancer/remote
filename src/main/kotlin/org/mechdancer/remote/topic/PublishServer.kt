//@file:Suppress("UNCHECKED_CAST")
//
//package org.mechdancer.remote.topic
//
//import org.mechdancer.remote.core.RemoteHub
//import java.io.ByteArrayOutputStream
//import java.io.DataOutputStream
//import java.rmi.RemoteException
//import java.rmi.server.UnicastRemoteObject
//import java.util.concurrent.atomic.AtomicBoolean
//
///**
// * 数据发布服务器
// * @param core 挂载服务的终端
// * @param map  话题编解码方案
// */
//class PublishServer(
//	val core: RemoteHub,
//	val map: Map<String, SerialTool<*>>
//) {
//	private var started = AtomicBoolean(false)
//
//	/**
//	 * 启动解析服务
//	 */
//	fun start() {
//		if (!started.getAndSet(true)) {
//			core.load<ParserServer>(object : UnicastRemoteObject(), ParserServer {
//				override operator fun get(topic: String) =
//					map[topic]?.input ?: throw RemoteException("topic not exist")
//			})
//		}
//	}
//
//	/**
//	 * 停止解析服务
//	 */
//	fun stop() {
//		if (started.getAndSet(false))
//			core.cancel<ParserServer>()
//	}
//
//	/**
//	 * 发送数据
//	 * @param topic 话题名
//	 * @param data  数据
//	 */
//	operator fun <T> set(topic: String, data: T) =
//		(map[topic]?.output as? (T) -> ByteArray)
//			?.let {
//				ByteArrayOutputStream().apply {
//					DataOutputStream(this).writeByte(topic.length)
//					write(topic.toByteArray())
//					write(it(data))
//				}
//			}
//			?.toByteArray()
//			?.let { core.broadcast('T', it) }
//			?: throw IllegalArgumentException("topic is not register or type goes run")
//}
