package org.github.taowen.daili;

import kilim.Pausable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UdpSendTest extends UsingFixture {
    public void test() throws Exception {
        Scheduler scheduler = new TestScheduler(this);
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
                scheduler.send(channel, buffer, new InetSocketAddress(9090), 1000);
            }
        };
        scheduler.callSoon(task);
        DatagramSocket client = new DatagramSocket(new InetSocketAddress(9090));
        try {
            scheduler.loopOnce();
            DatagramPacket packet = new DatagramPacket(new byte[4], 4);
            client.receive(packet);
            scheduler.loopOnce();
        } finally {
            client.close();
        }
    }
}
