package org.mechdancer.remote;

import kotlin.Unit;
import org.mechdancer.remote.core.BroadcastHub;

class JavaBroadcast {
    public static void main(String[] args) {
        BroadcastHub temp = new BroadcastHub(
                "X",
                (remote) -> {
                    System.out.println(remote);
                    return Unit.INSTANCE;
                },
                (handler, remote, pack) -> {
                    System.out.print(remote);
                    System.out.print(": ");
                    System.out.print(new String(pack));
                    return Unit.INSTANCE;
                }
        );

        while (true) temp.invoke(2048);
    }
}
