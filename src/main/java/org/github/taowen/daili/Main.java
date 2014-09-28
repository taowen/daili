package org.github.taowen.daili;

import kilim.Pausable;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class Main {
    public static void main(String[] args) throws Exception {
        Scheduler scheduler = new Scheduler();
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                System.out.println("listening...");
                scheduler.accept(serverSocketChannel);
                System.out.println("hello");
            }
        };
        scheduler.callSoon(task);
        scheduler.loop();
    }
}
