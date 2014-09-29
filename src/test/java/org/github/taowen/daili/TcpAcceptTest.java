package org.github.taowen.daili;

import junit.framework.TestCase;
import kilim.Pausable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;

public class TcpAcceptTest extends UsingFixture {
    public void testAcceptZero() throws IOException {
        Scheduler scheduler = new SchedulerFixture(this).scheduler;
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                scheduler.accept(serverSocketChannel);
                exit("accepted");
            }
        };
        scheduler.callSoon(task);
        scheduler.loopOnce();
        assertNotSame("accepted", task.exitResult);
    }
    public void testAcceptOne() throws IOException {
        Scheduler scheduler = new SchedulerFixture(this).scheduler;
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                scheduler.accept(serverSocketChannel);
                exit("accepted");
            }
        };
        scheduler.callSoon(task);
        scheduler.loopOnce();
        Socket client = new Socket();
        client.connect(new InetSocketAddress(9090));
        assertTrue(client.isConnected());
        scheduler.loopOnce();
        scheduler.loopOnce();
        assertEquals("accepted", task.exitResult);
    }
}
