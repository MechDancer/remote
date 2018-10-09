//package org.mechdancer.remote
//
//import org.junit.Test
//import org.mechdancer.remote.builder.broadcastServer
//import org.mechdancer.console
//
//class TestPIDTransfer {
//    @Test
//    fun testPIDTransfer() {
//        val server = broadcastServer("Server") {
//            newProcessDetected = ::println
//            broadcastReceived = { name, _msg ->
//                val msg = String(_msg.takeWhile { it != 0.toByte() }.toByteArray())
//                when (name to msg) {
//                    "Robot" to "pid" -> broadcast("pid 1.0 0.0 0.0".toByteArray())
//                    else -> println("$name: $msg")
//                }
//            }
//        }
//
//        val robot = broadcastServer("Robot") {
//            newProcessDetected = ::println
//            broadcastReceived = { name, msg ->
//                when (name) {
//                    "Server" -> {
//                        val sentence = String(msg) scanBy DefailtScanners
//                        when {
//                            sentence.getOrNull(0)?.text == "pid" -> {
//                                val p = sentence.getOrNull(1)?.data as? Double
//                                val i = sentence.getOrNull(2)?.data as? Double
//                                val d = sentence.getOrNull(3)?.data as? Double
//                                println("p: $p, i: $i, d: $d")
//                            }
//                        }
//                    }
//                    else -> println("$name: ${String(msg)}")
//                }
//            }
//        }
//
//        robot.broadcast("pid".toByteArray())
//        while (true);
//    }
//}
