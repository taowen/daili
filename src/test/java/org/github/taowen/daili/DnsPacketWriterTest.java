package org.github.taowen.daili;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xbill.DNS.*;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4.class)
public class DnsPacketWriterTest extends UsingFixture {

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
        header.setOpcode(Opcode.QUERY);
        byte[] bytes2 = message.toWire();
        assertThat(bytes1, is(bytes2));
    }
}
