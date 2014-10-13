package org.github.taowen.proto_dns;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xbill.DNS.*;

import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4.class)
public class DnsPacketWriterTest extends TestCase {

    @Test
    public void emptyMessage() throws IOException {
        DnsPacketWriter dnsPacket = new DnsPacketWriter();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
        dnsPacket.byteBuffer(byteBuffer);
        dnsPacket.writeId(123);
        dnsPacket.startFlags();
        dnsPacket.writeOpcode(Opcode.QUERY);
        dnsPacket.endFlags();
        dnsPacket.writeQuestionRecordsCount(0);
        dnsPacket.writeAnswerRecordsCount(0);
        dnsPacket.writeAuthorityRecordsCount(0);
        dnsPacket.writeAdditionalRecordsCount(0);
        byteBuffer.flip();
        byte[] bytes1 = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes1);

        Message message = new Message();
        Header header = message.getHeader();
        header.setID(123);
        header.setOpcode(Opcode.QUERY);
        byte[] bytes2 = message.toWire();
        assertThat(bytes1, is(bytes2));
    }

    @Test
    public void oneQuestion() throws TextParseException, EOFException {
        DnsPacketWriter dnsPacket = new DnsPacketWriter();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
        dnsPacket.byteBuffer(byteBuffer);
        dnsPacket.writeId(123);
        dnsPacket.startFlags();
        dnsPacket.writeFlagRD(true);
        dnsPacket.writeOpcode(Opcode.QUERY);
        dnsPacket.endFlags();
        dnsPacket.writeQuestionRecordsCount(1);
        dnsPacket.writeAnswerRecordsCount(0);
        dnsPacket.writeAuthorityRecordsCount(0);
        dnsPacket.writeAdditionalRecordsCount(0);
        dnsPacket.startRecord();
        dnsPacket.writeRecordName("www.google.com.");
        dnsPacket.writeRecordType(Type.A);
        dnsPacket.writeRecordDClass(DClass.IN);
        dnsPacket.endRecord();
        byteBuffer.flip();
        byte[] bytes1 = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes1);

        Record record = Record.newRecord(new Name("www.google.com."), Type.A, DClass.IN);
        Message message = Message.newQuery(record);
        Header header = message.getHeader();
        header.setID(123);
        byte[] bytes2 = message.toWire();
        assertThat(bytes1, is(bytes2));
    }

    @Test
    public void oneA() throws Exception {
        DnsPacketWriter dnsPacket = new DnsPacketWriter();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
        dnsPacket.byteBuffer(byteBuffer);
        dnsPacket.writeId(123);
        dnsPacket.startFlags();
        dnsPacket.writeOpcode(Opcode.QUERY);
        dnsPacket.endFlags();
        dnsPacket.writeQuestionRecordsCount(1);
        dnsPacket.writeAnswerRecordsCount(1);
        dnsPacket.writeAuthorityRecordsCount(0);
        dnsPacket.writeAdditionalRecordsCount(0);
        dnsPacket.startRecord();
        int wwwGoogleComPos = dnsPacket.byteBuffer().position();
        dnsPacket.writeRecordName("www.google.com.");
        dnsPacket.writeRecordType(Type.A);
        dnsPacket.writeRecordDClass(DClass.IN);
        dnsPacket.endRecord();
        dnsPacket.startRecord();
        dnsPacket.writeRecordNameLabel(wwwGoogleComPos);
        dnsPacket.writeRecordType(Type.A);
        dnsPacket.writeRecordDClass(DClass.IN);
        dnsPacket.writeRecordTTL(60);
        dnsPacket.writeRecordDataLength(4);
        dnsPacket.writeRecordInetAddress(Inet4Address.getByName("1.2.3.4"));
        dnsPacket.endRecord();
        byteBuffer.flip();
        byte[] bytes1 = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes1);

        Record q = Record.newRecord(new Name("www.google.com."), Type.A, DClass.IN);
        Message message = new Message();
        message.getHeader().setID(123);
        message.addRecord(q, Section.QUESTION);
        message.addRecord(new ARecord(new Name("www.google.com."), DClass.IN, 60, Inet4Address.getByName("1.2.3.4")), Section.ANSWER);
        byte[] bytes2 = message.toWire();
        assertThat(bytes1, is(bytes2));
    }
}
