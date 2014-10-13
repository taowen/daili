package org.github.taowen.proto_dns;

import junit.framework.TestCase;
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
public class DnsReaderTest extends TestCase {

    @Test
    public void emptyMessage() throws EOFException {
        Message message = new Message();
        Header header = message.getHeader();
        byte[] bytes = message.toWire();
        DnsReader reader = new DnsReader();
        reader.byteBuffer(ByteBuffer.wrap(bytes));
        assertThat(header.getID(), is(reader.readId()));
        reader.startFlags();
        assertThat(header.getOpcode(), is(reader.readOpcode()));
        assertThat(header.getFlag(Flags.QR), is(reader.readFlagQR()));
        assertThat(header.getFlag(Flags.AA), is(reader.readFlagAA()));
        assertThat(header.getFlag(Flags.TC), is(reader.readFlagTC()));
        assertThat(header.getFlag(Flags.QR), is(reader.readFlagQR()));
        reader.endFlags();
        assertThat(header.getCount(Section.QUESTION),
                is(reader.readQuestionRecordsCount()));
        assertThat(header.getCount(Section.ANSWER),
                is(reader.readAnswerRecordsCount()));
        assertThat(header.getCount(Section.AUTHORITY),
                is(reader.readAuthorityRecordsCount()));
        assertThat(header.getCount(Section.ADDITIONAL),
                is(reader.readAdditionalRecordsCount()));
    }

    @Test
    public void oneQuestion() throws TextParseException, EOFException {
        Record record = Record.newRecord(new Name("www.google.com."), Type.A, DClass.IN);
        Message message = Message.newQuery(record);
        byte[] bytes = message.toWire();
        DnsReader reader = new DnsReader();
        reader.byteBuffer(ByteBuffer.wrap(bytes));
        reader.skipHeader();
        reader.startRecord();
        assertThat("www.google.com.", is(reader.readRecordName()));
        assertThat(Type.A, is(reader.readRecordType()));
        assertThat(DClass.IN, is(reader.readRecordDClass()));
        reader.endRecord();
    }

    @Test
    public void oneA() throws Exception {
        Record q = Record.newRecord(new Name("www.google.com."), Type.A, DClass.IN);
        Message message = new Message();
        message.addRecord(q, Section.QUESTION);
        message.addRecord(new ARecord(new Name("www.google.com."), DClass.IN, 60, Inet4Address.getByName("1.2.3.4")), Section.ANSWER);
        byte[] bytes = message.toWire();
        DnsReader reader = new DnsReader();
        reader.byteBuffer(ByteBuffer.wrap(bytes));
        reader.skipHeader();
        reader.startRecord();
        assertThat("www.google.com.", is(reader.readRecordName()));
        assertThat(Type.A, is(reader.readRecordType()));
        assertThat(DClass.IN, is(reader.readRecordDClass()));
        reader.endRecord();
        reader.startRecord();
        assertThat("www.google.com.", is(reader.readRecordName()));
        assertThat(Type.A, is(reader.readRecordType()));
        assertThat(DClass.IN, is(reader.readRecordDClass()));
        assertThat((long)60, is(reader.readRecordTTL()));
        assertThat(4, is(reader.readRecordDataLength()));
        assertThat(InetAddress.getByName("1.2.3.4"), is(reader.readRecordInetAddress()));
        reader.endRecord();
    }

    @Test
    public void oneAButSkipData() throws Exception {
        Record q = Record.newRecord(new Name("www.google.com."), Type.A, DClass.IN);
        Message message = new Message();
        message.addRecord(q, Section.QUESTION);
        message.addRecord(new ARecord(new Name("www.google.com."), DClass.IN, 60, Inet4Address.getByName("1.2.3.4")), Section.ANSWER);
        byte[] bytes = message.toWire();
        DnsReader reader = new DnsReader();
        reader.byteBuffer(ByteBuffer.wrap(bytes));
        reader.skipHeader();
        reader.startRecord();
        assertThat("www.google.com.", is(reader.readRecordName()));
        assertThat(Type.A, is(reader.readRecordType()));
        assertThat(DClass.IN, is(reader.readRecordDClass()));
        reader.endRecord();
        reader.startRecord();
        assertThat("www.google.com.", is(reader.readRecordName()));
        assertThat(Type.A, is(reader.readRecordType()));
        assertThat(DClass.IN, is(reader.readRecordDClass()));
        assertThat((long)60, is(reader.readRecordTTL()));
        reader.endRecord();
    }
}
