package org.github.taowen.daili;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xbill.DNS.*;

import java.io.EOFException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4.class)
public class DnsPacketReaderTest extends UsingFixture {

    @Test
    public void emptyMessage() throws EOFException {
        Message message = new Message();
        Header header = message.getHeader();
        byte[] bytes = message.toWire();
        DnsPacketReader dnsPacket = new DnsPacketReader();
        dnsPacket.byteBuffer(ByteBuffer.wrap(bytes));
        assertThat(header.getID(), is(dnsPacket.readId()));
        dnsPacket.startFlags();
        assertThat(header.getOpcode(), is(dnsPacket.readOpcode()));
        assertThat(header.getFlag(Flags.QR), is(dnsPacket.readFlagQR()));
        assertThat(header.getFlag(Flags.AA), is(dnsPacket.readFlagAA()));
        assertThat(header.getFlag(Flags.TC), is(dnsPacket.readFlagTC()));
        assertThat(header.getFlag(Flags.QR), is(dnsPacket.readFlagQR()));
        dnsPacket.endFlags();
        assertThat(header.getCount(Section.QUESTION),
                is(dnsPacket.readQuestionRecordsCount()));
        assertThat(header.getCount(Section.ANSWER),
                is(dnsPacket.readAnswerRecordsCount()));
        assertThat(header.getCount(Section.AUTHORITY),
                is(dnsPacket.readAuthorityRecordsCount()));
        assertThat(header.getCount(Section.ADDITIONAL),
                is(dnsPacket.readAdditionalRecordsCount()));
    }

    @Test
    public void oneQuestion() throws TextParseException, EOFException {
        Record record = Record.newRecord(new Name("www.google.com."), Type.A, DClass.IN);
        Message message = Message.newQuery(record);
        byte[] bytes = message.toWire();
        DnsPacketReader dnsPacket = new DnsPacketReader();
        dnsPacket.byteBuffer(ByteBuffer.wrap(bytes));
        dnsPacket.skipHeader();
        dnsPacket.startRecord();
        assertThat("www.google.com.", is(dnsPacket.readRecordName()));
        assertThat(Type.A, is(dnsPacket.readRecordType()));
        assertThat(DClass.IN, is(dnsPacket.readRecordDClass()));
        dnsPacket.endRecord();
    }

    @Test
    public void oneA() throws Exception {
        Record q = Record.newRecord(new Name("www.google.com."), Type.A, DClass.IN);
        Message message = new Message();
        message.addRecord(q, Section.QUESTION);
        message.addRecord(new ARecord(new Name("www.google.com."), DClass.IN, 60, Inet4Address.getByName("1.2.3.4")), Section.ANSWER);
        byte[] bytes = message.toWire();
        DnsPacketReader dnsPacket = new DnsPacketReader();
        dnsPacket.byteBuffer(ByteBuffer.wrap(bytes));
        dnsPacket.skipHeader();
        dnsPacket.startRecord();
        assertThat("www.google.com.", is(dnsPacket.readRecordName()));
        assertThat(Type.A, is(dnsPacket.readRecordType()));
        assertThat(DClass.IN, is(dnsPacket.readRecordDClass()));
        dnsPacket.endRecord();
        dnsPacket.startRecord();
        assertThat("www.google.com.", is(dnsPacket.readRecordName()));
        assertThat(Type.A, is(dnsPacket.readRecordType()));
        assertThat(DClass.IN, is(dnsPacket.readRecordDClass()));
        assertThat((long)60, is(dnsPacket.readRecordTTL()));
        assertThat(4, is(dnsPacket.readRecordDataLength()));
        assertThat(InetAddress.getByName("1.2.3.4"), is(dnsPacket.readRecordInetAddress()));
        dnsPacket.endRecord();
    }

    @Test
    public void oneAButSkipData() throws Exception {
        Record q = Record.newRecord(new Name("www.google.com."), Type.A, DClass.IN);
        Message message = new Message();
        message.addRecord(q, Section.QUESTION);
        message.addRecord(new ARecord(new Name("www.google.com."), DClass.IN, 60, Inet4Address.getByName("1.2.3.4")), Section.ANSWER);
        byte[] bytes = message.toWire();
        DnsPacketReader dnsPacket = new DnsPacketReader();
        dnsPacket.byteBuffer(ByteBuffer.wrap(bytes));
        dnsPacket.skipHeader();
        dnsPacket.startRecord();
        assertThat("www.google.com.", is(dnsPacket.readRecordName()));
        assertThat(Type.A, is(dnsPacket.readRecordType()));
        assertThat(DClass.IN, is(dnsPacket.readRecordDClass()));
        dnsPacket.endRecord();
        dnsPacket.startRecord();
        assertThat("www.google.com.", is(dnsPacket.readRecordName()));
        assertThat(Type.A, is(dnsPacket.readRecordType()));
        assertThat(DClass.IN, is(dnsPacket.readRecordDClass()));
        assertThat((long)60, is(dnsPacket.readRecordTTL()));
        dnsPacket.endRecord();
    }
}
