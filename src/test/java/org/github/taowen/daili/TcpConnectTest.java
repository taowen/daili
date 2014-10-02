package org.github.taowen.daili;

import kilim.Pausable;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class TcpConnectTest extends UsingFixture {
    private ServerSocket serverSocket;

    public void test() throws Exception {
        new Fixture(this) {
            @Override
            public void execute() throws Pausable, Exception {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(9090));
                yield();
                serverSocket.close();
            }
        };
        Scheduler scheduler = new TestScheduler(this);
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                SocketChannel socketChannel = SocketChannel.open();
                scheduler.connect(socketChannel, new InetSocketAddress(9090));
                exit("accepted");
            }
        };
        scheduler.callSoon(task);
        scheduler.loopOnce();
        assertEquals("accepted", task.exitResult);
        Socket clientSocket = serverSocket.accept();
        assertNotNull(clientSocket);
    }
}
