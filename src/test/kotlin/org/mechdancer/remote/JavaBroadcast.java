//package org.mechdancer.remote;
//
//import kotlin.Unit;
//import org.mechdancer.remote.core.RemoteHub;
//
//import java.lang.reflect.Array;
//
//class JavaBroadcast {
//    public static void main(String[] args) {
//        RemoteHub temp = new RemoteHub(
//                "X",
//                (remote) -> {
//                    System.out.println(remote);
//                    return Unit.INSTANCE;
//                },
//                (handler, remote, pack) -> {
//                    System.out.print(remote);
//                    System.out.print(": ");
//                    System.out.print(new String(pack));
//                    return Unit.INSTANCE;
//                },
//                (input) -> {
//                    System.out.println(new String(input));
//                    return
//                }
//        );
//
//        while (true) temp.invoke(2048);
//    }
//}
