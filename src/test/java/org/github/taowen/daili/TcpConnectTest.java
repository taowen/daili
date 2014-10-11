package org.github.taowen.daili;

import kilim.Pausable;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TcpConnectTest extends UsingFixture {
    public void test() throws Exception {
        ServerSocket serverSocket = new ServerSocket();
        try {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(9090));
            DefaultScheduler scheduler = new TestScheduler(this);
            DailiTask task = new DailiTask(scheduler) {
                @Override
                public void execute() throws Pausable, Exception {
                    SocketChannel socketChannel = SocketChannel.open();
                    scheduler().connect(socketChannel, new InetSocketAddress(9090), 1000);
                    exit("accepted");
                }
            };
            task.run();
            scheduler.loopOnce();
            assertThat(task.exitResult, is((Object) "accepted"));
            Socket clientSocket = serverSocket.accept();
            assertThat(clientSocket, notNullValue());
        } finally {
            serverSocket.close();
        }
    }
}
