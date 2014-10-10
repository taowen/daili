package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TcpReadTest extends UsingFixture {
    public void test() throws Exception {
        Scheduler scheduler = new TestScheduler(this);
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                SocketChannel channel = scheduler.accept(serverSocketChannel, 1000);
                channel.configureBlocking(false);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                scheduler.read(channel, byteBuffer, 1000);
                ByteBuffer expected = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
                exit(byteBuffer.flip().equals(expected));
            }
        };
        scheduler.callSoon(task);
        scheduler.loopOnce();
        Socket client = new Socket();
        client.connect(new InetSocketAddress(9090));
        assertTrue(client.isConnected());
        scheduler.loopOnce();
        scheduler.loopOnce();
        client.getOutputStream().write(new byte[]{1, 2, 3, 4});
        scheduler.loopOnce();
        scheduler.loopOnce();
        assertTrue((Boolean)task.exitResult);
    }

    private ServerSocketChannel open() throws IOException {
        return TestSocket.openServerSocketChannel(this);
    }
}
