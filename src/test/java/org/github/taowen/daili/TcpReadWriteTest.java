package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

public class TcpReadWriteTest extends UsingFixture {
    public void test() throws Exception {
        DefaultScheduler scheduler = new DefaultScheduler();
        try {
            DailiTask serverTask = new DailiTask(scheduler) {
                @Override
                public void execute() throws Pausable, Exception {
                    ServerSocketChannel serverSocketChannel = open();
                    serverSocketChannel.socket().setReuseAddress(true);
                    serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                    serverSocketChannel.configureBlocking(false);
                    SocketChannel channel = getScheduler().accept(serverSocketChannel, 1000);
                    channel.configureBlocking(false);
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                    while (true) {
                        try {
                            getScheduler().read(channel, byteBuffer, 100);
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
                    getScheduler().connect(channel, new InetSocketAddress(9090), 1000);
                    getScheduler().write(channel, ByteBuffer.wrap(new byte[]{1, 2, 3, 4}), 1000);
                    getScheduler().write(channel, ByteBuffer.wrap(new byte[]{5, 6, 7, 8}), 1000);
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

    private ServerSocketChannel open() throws IOException {
        return TestSocket.openServerSocketChannel(this);
    }
}
