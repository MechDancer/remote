package org.mechdancer.remote;

import kotlin.Unit;
import org.mechdancer.remote.core.BroadcastServer;

class JavaBroadcast {
    public static void main(String[] args) {
        BroadcastServer temp = new BroadcastServer(
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

        while (true) ;
    }
}
