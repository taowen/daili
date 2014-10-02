package org.github.taowen.daili;

import kilim.Pausable;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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
                scheduler.timeout = 5000;
                SocketChannel socketChannel = scheduler.accept(serverSocketChannel);
                socketChannel.configureBlocking(false);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                while (scheduler.read(socketChannel, byteBuffer) > 0) {
                    byteBuffer.flip();
                    scheduler.write(socketChannel, byteBuffer);
                    byteBuffer.clear();
                }
            }
        };
        scheduler.callSoon(task);
        scheduler.loop();
    }
}
