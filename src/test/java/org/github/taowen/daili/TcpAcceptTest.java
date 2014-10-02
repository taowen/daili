package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeoutException;

public class TcpAcceptTest extends UsingFixture {
    public void testAcceptZero() throws IOException {
        Scheduler scheduler = new TestScheduler(this);
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
        assertFalse("accepted".equals(task.exitResult));
    }
    public void testAcceptOne() throws IOException {
        Scheduler scheduler = new TestScheduler(this);
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
    public void testAcceptTimeout() throws IOException {
        TestScheduler scheduler = new TestScheduler(this);
        scheduler.fixedCurrentTimeMillis = System.currentTimeMillis();
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                scheduler.timeout = 1000;
                try {
                    scheduler.accept(serverSocketChannel);
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
    public void testAcceptTwo() throws IOException {
        TestScheduler scheduler = new TestScheduler(this);
        scheduler.fixedCurrentTimeMillis = System.currentTimeMillis();
        DailiTask task1 = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9090));
                serverSocketChannel.configureBlocking(false);
                scheduler.timeout = 1000;
                try {
                    scheduler.accept(serverSocketChannel);
                } catch (TimeoutException e) {
                    exitResult = "timeout";
                }
                scheduler.timeout = 5000;
                scheduler.accept(serverSocketChannel);
                exit("done");
            }
        };
        scheduler.callSoon(task1);
        DailiTask task2 = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(9091));
                serverSocketChannel.configureBlocking(false);
                scheduler.timeout = 2000;
                try {
                    scheduler.accept(serverSocketChannel);
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
        client.connect(new InetSocketAddress(9090));
        assertTrue(client.isConnected());
        scheduler.loopOnce();
        scheduler.loopOnce();
        assertEquals("done", task1.exitResult);
    }
}
