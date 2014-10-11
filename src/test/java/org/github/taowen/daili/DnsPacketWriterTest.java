package org.github.taowen.daili;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4.class)
public class DnsPacketWriterTest extends UsingFixture {

    @Test
    public void emptyMessage() throws IOException {
        Message message = new Message();
        Header header = message.getHeader();
        DnsPacketWriter dnsPacket = new DnsPacketWriter();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
        dnsPacket.byteBuffer(byteBuffer);
        dnsPacket.writeId(header.getID());
        dnsPacket.startFlags();
        dnsPacket.writeOpcode(header.getOpcode());
        dnsPacket.endFlags();
        dnsPacket.writeQuestionRecordsCount(0);
        dnsPacket.writeAnswerRecordsCount(0);
        dnsPacket.writeAuthorityRecordsCount(0);
        dnsPacket.writeAdditionalRecordsCount(0);
        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes);
        System.out.println(new Message(bytes));
    }
}
