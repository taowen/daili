package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

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
                SocketChannel socketChannel = tryAccept(serverSocketChannel);
                socketChannel.configureBlocking(false);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                while (scheduler.read(socketChannel, byteBuffer) > 0) {
                    byteBuffer.flip();
                    scheduler.write(socketChannel, byteBuffer);
                    byteBuffer.clear();
                }
            }

            private SocketChannel tryAccept(ServerSocketChannel serverSocketChannel) throws IOException, Pausable {
                while(true) {
                    try {
                        return scheduler.accept(serverSocketChannel);
                    } catch (TimeoutException e) {
                        System.out.println("time out, try again");
                        continue;
                    }
                }
            }
        };
        scheduler.callSoon(task);
        scheduler.loop();
    }
}
