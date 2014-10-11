package org.github.taowen.daili;

import kilim.Pausable;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Main {
    public static void main(String[] args) throws Exception {
        Scheduler scheduler = new DefaultScheduler();
        final PinnedWorker worker = new PinnedWorker();
        DailiTask task = new DailiTask(scheduler, worker) {
            @Override
            public void execute() throws Pausable, Exception {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.socket().bind(new InetSocketAddress(9090));
                while (true) {
                    final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                    getScheduler().receive(channel, buffer, 60 * 1000);
                    new DailiTask(getScheduler(), worker){
                        @Override
                        public void execute() throws Pausable, Exception {
                            buffer.flip();
                            final DnsPacketReader dnsPacket = new DnsPacketReader();
                            dnsPacket.setByteBuffer(buffer);
                            dnsPacket.readId();
                            dnsPacket.startFlags();
                            dnsPacket.endFlags();
                            dnsPacket.readQuestionRecordsCount();
                            dnsPacket.readAnswerRecordsCount();
                            dnsPacket.readAuthorityRecordsCount();
                            dnsPacket.readAdditionalRecordsCount();
                            dnsPacket.startRecord();
                            System.out.println(dnsPacket.readRecordName());
                            dnsPacket.readRecordType();
                            dnsPacket.readRecordDClass();
                            dnsPacket.endRecord();
                        }
                    }.run();
                }
            }
        };
        task.run();
        new Thread(worker).start();
        scheduler.loop();
    }
}
