package org.github.taowen.daili;

import kilim.Pausable;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

public class TcpReadWriteTest extends UsingFixture {
    public void test() throws Exception {
        Scheduler scheduler = new Scheduler();
        try {
            DailiTask serverTask = new DailiTask(scheduler) {
                @Override
                public void execute() throws Pausable, Exception {
                    scheduler.timeout = 100;
                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.socket().setReuseAddress(true);
                    serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                    serverSocketChannel.configureBlocking(false);
                    SocketChannel channel = scheduler.accept(serverSocketChannel);
                    channel.configureBlocking(false);
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                    while (true) {
                        try {
                            scheduler.read(channel, byteBuffer);
                        } catch (TimeoutException e) {
                            ByteBuffer expected = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
                            byteBuffer.flip();
                            exit(expected.equals(byteBuffer));
                        }
                    }
                }
            };
            DailiTask clientTask = new DailiTask(scheduler) {
                @Override
                public void execute() throws Pausable, Exception {
                    SocketChannel channel = SocketChannel.open();
                    scheduler.connect(channel, new InetSocketAddress(9090));
                    scheduler.write(channel, ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));
                    scheduler.write(channel, ByteBuffer.wrap(new byte[]{5, 6, 7, 8}));
                }
            };
            scheduler.callSoon(serverTask);
            scheduler.callSoon(clientTask);
            scheduler.loop();
            assertTrue((Boolean)serverTask.exitResult);
        } finally {
            scheduler.close();
        }
    }
}
