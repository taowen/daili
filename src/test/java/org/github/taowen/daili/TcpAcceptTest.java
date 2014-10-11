package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeoutException;

public class TcpAcceptTest extends UsingFixture {
    public void testIncomplete() throws IOException {
        DefaultScheduler scheduler = new TestScheduler(this);
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                getScheduler().accept(serverSocketChannel, 1000);
                exit("accepted");
            }
        };
        scheduler.callSoon(task);
        scheduler.loopOnce();
        assertFalse("accepted".equals(task.exitResult));
    }

    public void testSuccess() throws IOException {
        DefaultScheduler scheduler = new TestScheduler(this);
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                getScheduler().accept(serverSocketChannel, 1000);
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

    public void testTimeout() throws IOException {
        TestScheduler scheduler = new TestScheduler(this);
        scheduler.fixedCurrentTimeMillis = System.currentTimeMillis();
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                try {
                    getScheduler().accept(serverSocketChannel, 1000);
                } catch (TimeoutException e) {
                    exit("timeout");
                }
            }
        };
        scheduler.callSoon(task);
        scheduler.loopOnce();
        scheduler.fixedCurrentTimeMillis += 3000;
        scheduler.loopOnce();
        assertEquals("timeout", task.exitResult);
    }

    public void testOneTimeoutOneSuccess() throws IOException {
        TestScheduler scheduler = new TestScheduler(this);
        scheduler.fixedCurrentTimeMillis = System.currentTimeMillis();
        DailiTask task1 = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(8090));
                serverSocketChannel.configureBlocking(false);
                try {
                    getScheduler().accept(serverSocketChannel, 1000);
                } catch (TimeoutException e) {
                    exitResult = "timeout";
                }
                getScheduler().accept(serverSocketChannel, 5000);
                exit("done");
            }
        };
        scheduler.callSoon(task1);
        DailiTask task2 = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(8091));
                serverSocketChannel.configureBlocking(false);
                try {
                    getScheduler().accept(serverSocketChannel, 2000);
                } catch (RuntimeException e) {
                    exit("timeout");
                }
            }
        };
        scheduler.callSoon(task2);
        scheduler.loopOnce();
        scheduler.fixedCurrentTimeMillis += 1500;
        scheduler.loopOnce();
        assertEquals("timeout", task1.exitResult);
        assertFalse("timeout".equals(task2.exitResult));
        Socket client = new Socket();
        client.connect(new InetSocketAddress(8090));
        assertTrue(client.isConnected());
        scheduler.loopOnce();
        scheduler.loopOnce();
        assertEquals("done", task1.exitResult);
    }

    private ServerSocketChannel open() throws IOException {
        return TestSocket.openServerSocketChannel(this);
    }
}
