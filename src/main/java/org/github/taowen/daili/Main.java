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
                while (true) {
                    final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                    scheduler.receive(channel, buffer);
                    new DailiTask(scheduler){
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
                    }.resume();
                }
            }
        };
        scheduler.callSoon(task);
        scheduler.loop();
    }
}
