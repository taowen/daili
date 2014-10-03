package org.github.taowen.daili;

import kilim.Pausable;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Main {
    public static void main(String[] args) throws Exception {
        Scheduler scheduler = new Scheduler();
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.socket().bind(new InetSocketAddress(9090));
                ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                scheduler.receive(channel, buffer);
                System.out.println(buffer);
            }
        };
        scheduler.callSoon(task);
        scheduler.loop();
    }
}
