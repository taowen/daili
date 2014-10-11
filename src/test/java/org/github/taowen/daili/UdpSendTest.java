package org.github.taowen.daili;

import kilim.Pausable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UdpSendTest extends UsingFixture {
    public void test() throws Exception {
        DefaultScheduler scheduler = new TestScheduler(this);
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
                scheduler().send(channel, buffer, new InetSocketAddress(9090), 1000);
            }
        };
        DatagramSocket client = new DatagramSocket(new InetSocketAddress(9090));
        try {
            task.run();
            DatagramPacket packet = new DatagramPacket(new byte[4], 4);
            client.setSoTimeout(100);
            client.receive(packet);
            scheduler.loopOnce();
            assertThat(packet.getData(), is(new byte[]{1, 2, 3, 4}));
        } finally {
            client.close();
        }
    }
}
