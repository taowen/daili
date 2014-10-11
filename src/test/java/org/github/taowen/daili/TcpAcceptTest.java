package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
                scheduler().accept(serverSocketChannel, 1000);
                exit("accepted");
            }
        };
        task.run();
        scheduler.loopOnce();
        assertThat(task.exitResult, not((Object)"accepted"));
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
                scheduler().accept(serverSocketChannel, 1000);
                exit("accepted");
            }
        };
        task.run();
        scheduler.loopOnce();
        Socket client = new Socket();
        client.connect(new InetSocketAddress(9090));
        assertThat(client.isConnected(), is(true));
        scheduler.loopOnce();
        scheduler.loopOnce();
        assertThat(task.exitResult, is((Object)"accepted"));
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
                    scheduler().accept(serverSocketChannel, 1000);
                } catch (TimeoutException e) {
                    exit("timeout");
                }
            }
        };
        task.run();
        scheduler.loopOnce();
        scheduler.fixedCurrentTimeMillis += 3000;
        scheduler.loopOnce();
        assertThat(task.exitResult, is((Object)"timeout"));
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
                    scheduler().accept(serverSocketChannel, 1000);
                } catch (TimeoutException e) {
                    exitResult = "timeout";
                }
                scheduler().accept(serverSocketChannel, 5000);
                exit("done");
            }
        };
        task1.run();
        DailiTask task2 = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                ServerSocketChannel serverSocketChannel = open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(8091));
                serverSocketChannel.configureBlocking(false);
                try {
                    scheduler().accept(serverSocketChannel, 2000);
                } catch (RuntimeException e) {
                    exit("timeout");
                }
            }
        };
        task2.run();
        scheduler.loopOnce();
        scheduler.fixedCurrentTimeMillis += 1500;
        scheduler.loopOnce();
        assertThat(task1.exitResult, is((Object)"timeout"));
        assertThat(task2.exitResult, not((Object)"timeout"));
        Socket client = new Socket();
        client.connect(new InetSocketAddress(8090));
        assertThat(client.isConnected(), is(true));
        scheduler.loopOnce();
        scheduler.loopOnce();
        assertThat(task1.exitResult, is((Object)"done"));
    }

    private ServerSocketChannel open() throws IOException {
        return TestSocket.openServerSocketChannel(this);
    }
}
