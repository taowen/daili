package org.github.taowen.daili;

import kilim.Pausable;
import org.github.taowen.proto_dns.DnsReader;
import org.github.taowen.proto_dns.DnsWriter;

import java.io.EOFException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Main {
    public static void main(String[] args) throws Exception {
        Scheduler scheduler = new DefaultScheduler();
        final PinnedWorker worker = new PinnedWorker();
        DailiTask task = new DailiTask(scheduler, worker) {
            @Override
            public void execute() throws Pausable, Exception {
                final DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.socket().bind(new InetSocketAddress(9090));
                while (true) {
                    final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                    final SocketAddress address = scheduler().receive(channel, buffer, 60 * 1000);
                    new DailiTask(scheduler(), worker){
                        @Override
                        public void execute() throws Pausable, Exception {
                            buffer.flip();
                            int id = readPacket(buffer);
                            buffer.clear();
                            DnsWriter dnsPacket = new DnsWriter();
                            dnsPacket.byteBuffer(buffer);
                            dnsPacket.writeId(id);
                            dnsPacket.startFlags();
                            dnsPacket.writeOpcode(dnsPacket.OPCODE_QUERY);
                            dnsPacket.endFlags();
                            dnsPacket.writeQuestionRecordsCount(1);
                            dnsPacket.writeAnswerRecordsCount(1);
                            dnsPacket.writeAuthorityRecordsCount(0);
                            dnsPacket.writeAdditionalRecordsCount(0);
                            dnsPacket.startRecord();
                            int wwwGoogleComPos = dnsPacket.byteBuffer().position();
                            dnsPacket.writeRecordName("www.google.com.");
                            dnsPacket.writeRecordType(dnsPacket.TYPE_A);
                            dnsPacket.writeRecordDClass(dnsPacket.DCLASS_IN);
                            dnsPacket.endRecord();
                            dnsPacket.startRecord();
                            dnsPacket.writeRecordNameLabel(wwwGoogleComPos);
                            dnsPacket.writeRecordType(dnsPacket.TYPE_A);
                            dnsPacket.writeRecordDClass(dnsPacket.DCLASS_IN);
                            dnsPacket.writeRecordTTL(60);
                            dnsPacket.writeRecordDataLength(4);
                            dnsPacket.writeRecordInetAddress(Inet4Address.getByName("1.2.3.4"));
                            dnsPacket.endRecord();
                            buffer.flip();
                            scheduler().send(channel, buffer, address, 60 * 1000);
                        }
                    }.run();
                }
            }
        };
        task.run();
        new Thread(worker).start();
        scheduler.loop();
    }

    private static int readPacket(ByteBuffer buffer) throws EOFException {
        final DnsReader dnsPacket = new DnsReader();
        dnsPacket.byteBuffer(buffer);
        int id = dnsPacket.readId();
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
        return id;
    }
}
