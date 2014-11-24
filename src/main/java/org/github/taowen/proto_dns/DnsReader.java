package org.github.taowen.proto_dns;

import kilim.Pausable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

public class DnsReader extends DnsProcessor {

    protected Queue<Object> output;

    public DnsReader() {
        processingFields = new LinkedList<Field>();
        output = new LinkedList<Object>();
    }

    public void skipHeader() {
        readId();
        startFlags();
        endFlags();
        readQuestionRecordsCount();
        readAnswerRecordsCount();
        readAuthorityRecordsCount();
        readAdditionalRecordsCount();
    }

    public int readId() {
        processingFields.offer(Field.ID);
        run();
        return (Integer)output.remove();
    }

    public int readOpcode() {
        processingFields.offer(Field.OPCODE);
        run();
        return (Integer)output.remove();
    }

    public boolean readFlagQR() {
        processingFields.offer(Field.FLAG_QR);
        run();
        return (Boolean)output.remove();
    }

    public boolean readFlagAA() {
        processingFields.offer(Field.FLAG_AA);
        run();
        return (Boolean)output.remove();
    }

    public boolean readFlagTC() {
        processingFields.offer(Field.FLAG_TC);
        run();
        return (Boolean)output.remove();
    }

    public int readQuestionRecordsCount() {
        processingFields.offer(Field.QUESTION_RECORDS_COUNT);
        run();
        return (Integer)output.remove();
    }

    public int readAnswerRecordsCount() {
        processingFields.offer(Field.ANSWER_RECORDS_COUNT);
        run();
        return (Integer)output.remove();
    }

    public int readAuthorityRecordsCount() {
        processingFields.offer(Field.AUTHORITY_RECORDS_COUNT);
        run();
        return (Integer)output.remove();
    }

    public int readAdditionalRecordsCount() {
        processingFields.offer(Field.ADDITIONAL_RECORDS_COUNT);
        run();
        return (Integer)output.remove();
    }

    public String readRecordName() {
        processingFields.offer(Field.RECORD_NAME);
        run();
        StringBuilder buf = new StringBuilder();
        while(true) {
            String label = readRecordNameLabel();
            if (null == label) {
                return buf.toString();
            }
            buf.append(label);
            buf.append(".");
        }
    }

    private String readRecordNameLabel() {
        processingFields.offer(Field.RECORD_NAME_LABEL);
        run();
        return (String)output.remove();
    }

    public int readRecordType() {
        processingFields.offer(Field.RECORD_TYPE);
        run();
        return (Integer)output.remove();
    }

    public int readRecordDClass() {
        processingFields.offer(Field.RECORD_DCLASS);
        run();
        return (Integer)output.remove();
    }

    public long readRecordTTL() {
        processingFields.offer(Field.RECORD_TTL);
        run();
        return (Long)output.remove();
    }

    public int readRecordDataLength() {
        processingFields.offer(Field.RECORD_DATA_LENGTH);
        run();
        return (Integer)output.remove();
    }

    public InetAddress readRecordInetAddress() throws UnknownHostException {
        processingFields.offer(Field.RECORD_INET_ADDRESS);
        run();
        return InetAddress.getByAddress((byte[])output.remove());
    }

    @Override
    protected void processAdditionalRecordsCount() throws Pausable {
        output.offer(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void processAuthorityRecordsCount() throws Pausable {
        output.offer(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void processAnswerRecordsCount() throws Pausable {
        output.offer(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected int processQuestionRecordsCount() throws Pausable {
        int questionRecordsCount = byteBuffer.getShort() & 0xFFFF;
        output.offer(questionRecordsCount);
        return questionRecordsCount;
    }

    @Override
    protected void processId() throws Pausable {
        output.offer(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void processFlags() throws Pausable {
        int flags = byteBuffer.getShort() & 0xFFFF;
        Field nextField = nextField();
        while (Field.END_FLGAS != nextField) {
            switch (nextField) {
                case OPCODE:
                    output.offer((flags >> 11) & 0xF);
                    break;
                case FLAG_QR:
                    output.offer(getFlag(flags, 0));
                    break;
                case FLAG_AA:
                    output.offer(getFlag(flags, 5));
                    break;
                case FLAG_TC:
                    output.offer(getFlag(flags, 6));
                    break;
                case FLAG_RD:
                    output.offer(getFlag(flags, 7));
                    break;
                default:
                    throw new RuntimeException("unexpected field: " + nextField);
            }
            nextField = nextField();
        }
    }

    private boolean getFlag(int flags, int bit) {
        return (flags & (1 << (15 - bit))) != 0;
    }

    @Override
    protected void processRecordInetAddress() throws Pausable {
        byte[] fieldBytesValue = new byte[4];
        byteBuffer.get(fieldBytesValue);
        output.offer(fieldBytesValue);
    }

    @Override
    protected void processRecordDataLength() throws Pausable {
        output.offer(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void skipRecordData() throws Pausable {
        int dataLength = byteBuffer.getShort() & 0xFFFF;
        byteBuffer.position(byteBuffer.position() + dataLength);
    }

    @Override
    protected void processRecordTTL() throws Pausable {
        output.offer(byteBuffer.getInt() & 0xFFFFFFFFL);
    }

    @Override
    protected void processRecordDClass() throws Pausable {
        output.offer(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void processRecordType() throws Pausable {
        output.offer(byteBuffer.getShort() & 0xFFFF);

    }

    @Override
    protected void processRecordName() throws IOException, Pausable {
        int savePoint = -1;
        int first = byteBuffer.position();
        while (true)
        {
            int len = byteBuffer.get();
            if (len == 0)
            {
                break;
            }
            switch (len & 0xC0)
            {
                case 0x00:
                    //buf.append("[" + off + "]");
                    assertNextField(Field.RECORD_NAME_LABEL);
                    output.offer(readUTF(len));
                    break;
                case 0xC0:
                    //buf.append("<" + (off - 1) + ">");
                    if (savePoint < 0) {
                        savePoint = byteBuffer.position() + 1;
                    }
                    int pointer = ((len & 0x3F) << 8) | byteBuffer.get();
                    if (pointer >= first)
                    {
                        throw new IOException("bad domain name: possible circular name detected");
                    }
                    byteBuffer.position(pointer);
                    first = byteBuffer.position();
                    break;
                default:
                    throw new IOException("bad domain name: at " + byteBuffer.position());
            }
        }
        assertNextField(Field.RECORD_NAME_LABEL);
        output.offer(null);
        if (savePoint >= 0) {
            byteBuffer.position(savePoint);
        }
    }

    private String readUTF(int len) throws UnsupportedEncodingException {
        byte[] bytes = new byte[len];
        byteBuffer.get(bytes);
        return new String(bytes, "UTF8");
    }
}
