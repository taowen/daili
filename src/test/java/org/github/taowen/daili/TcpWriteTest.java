package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class TcpWriteTest extends UsingFixture {
    public void test() throws Exception {
        DefaultScheduler scheduler = new TestScheduler(this);
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                SocketChannel channel = getScheduler().accept(serverSocketChannel, 1000);
                channel.configureBlocking(false);
                ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
                getScheduler().write(channel, byteBuffer, 1000);
            }
        };
        scheduler.callSoon(task);
        scheduler.loopOnce();
        Socket client = new Socket();
        client.connect(new InetSocketAddress(9090));
        assertTrue(client.isConnected());
        scheduler.loopOnce();
        scheduler.loopOnce();
        byte[] output = new byte[4];
        client.getInputStream().read(output);
        scheduler.loopOnce();
        scheduler.loopOnce();
        assertTrue(Arrays.equals(new byte[]{1, 2, 3, 4}, output));
    }

    private ServerSocketChannel open() throws IOException {
        return TestSocket.openServerSocketChannel(this);
    }
}
