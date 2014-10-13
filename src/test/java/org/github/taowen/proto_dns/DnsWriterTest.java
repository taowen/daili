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
public class DnsWriterTest extends TestCase {

    @Test
    public void emptyMessage() throws IOException {
        DnsWriter writer = new DnsWriter();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
        writer.byteBuffer(byteBuffer);
        writer.writeId(123);
        writer.startFlags();
        writer.writeOpcode(Opcode.QUERY);
        writer.endFlags();
        writer.writeQuestionRecordsCount(0);
        writer.writeAnswerRecordsCount(0);
        writer.writeAuthorityRecordsCount(0);
        writer.writeAdditionalRecordsCount(0);
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
        DnsWriter writer = new DnsWriter();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
        writer.byteBuffer(byteBuffer);
        writer.writeId(123);
        writer.startFlags();
        writer.writeFlagRD(true);
        writer.writeOpcode(Opcode.QUERY);
        writer.endFlags();
        writer.writeQuestionRecordsCount(1);
        writer.writeAnswerRecordsCount(0);
        writer.writeAuthorityRecordsCount(0);
        writer.writeAdditionalRecordsCount(0);
        writer.startRecord();
        writer.writeRecordName("www.google.com.");
        writer.writeRecordType(Type.A);
        writer.writeRecordDClass(DClass.IN);
        writer.endRecord();
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
        DnsWriter writer = new DnsWriter();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
        writer.byteBuffer(byteBuffer);
        writer.writeId(123);
        writer.startFlags();
        writer.writeOpcode(Opcode.QUERY);
        writer.endFlags();
        writer.writeQuestionRecordsCount(1);
        writer.writeAnswerRecordsCount(1);
        writer.writeAuthorityRecordsCount(0);
        writer.writeAdditionalRecordsCount(0);
        writer.startRecord();
        int wwwGoogleComPos = writer.byteBuffer().position();
        writer.writeRecordName("www.google.com.");
        writer.writeRecordType(Type.A);
        writer.writeRecordDClass(DClass.IN);
        writer.endRecord();
        writer.startRecord();
        writer.writeRecordNameLabel(wwwGoogleComPos);
        writer.writeRecordType(Type.A);
        writer.writeRecordDClass(DClass.IN);
        writer.writeRecordTTL(60);
        writer.writeRecordDataLength(4);
        writer.writeRecordInetAddress(Inet4Address.getByName("1.2.3.4"));
        writer.endRecord();
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
