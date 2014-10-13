package org.github.taowen.proto_dns;

import kilim.Pausable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsPacketReader extends DnsPacketProcessor {

    protected long fieldLongValue;
    protected int fieldIntValue;
    protected byte[] fieldBytesValue;
    protected String fieldStringValue;
    protected boolean fieldBooleanValue;

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
        currentField = Field.ID;
        run();
        return fieldIntValue;
    }

    public int readOpcode() {
        currentField = Field.OPCODE;
        run();
        return fieldIntValue;
    }

    public boolean readFlagQR() {
        currentField = Field.FLAG_QR;
        run();
        return fieldBooleanValue;
    }

    public boolean readFlagAA() {
        currentField = Field.FLAG_AA;
        run();
        return fieldBooleanValue;
    }

    public boolean readFlagTC() {
        currentField = Field.FLAG_TC;
        run();
        return fieldBooleanValue;
    }

    public int readQuestionRecordsCount() {
        currentField = Field.QUESTION_RECORDS_COUNT;
        run();
        return fieldIntValue;
    }

    public int readAnswerRecordsCount() {
        currentField = Field.ANSWER_RECORDS_COUNT;
        run();
        return fieldIntValue;
    }

    public int readAuthorityRecordsCount() {
        currentField = Field.AUTHORITY_RECORDS_COUNT;
        run();
        return fieldIntValue;
    }

    public int readAdditionalRecordsCount() {
        currentField = Field.ADDITIONAL_RECORDS_COUNT;
        run();
        return fieldIntValue;
    }

    public String readRecordName() {
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
        currentField = Field.RECORD_NAME;
        run();
        return fieldStringValue;
    }

    public int readRecordType() {
        currentField = Field.RECORD_TYPE;
        run();
        return fieldIntValue;
    }

    public int readRecordDClass() {
        currentField = Field.RECORD_DCLASS;
        run();
        return fieldIntValue;
    }

    public long readRecordTTL() {
        currentField = Field.RECORD_TTL;
        run();
        return fieldLongValue;
    }

    public int readRecordDataLength() {
        currentField = Field.RECORD_DATA_LENGTH;
        run();
        return fieldIntValue;
    }

    public InetAddress readRecordInetAddress() throws UnknownHostException {
        currentField = Field.RECORD_INET_ADDRESS;
        run();
        return InetAddress.getByAddress(fieldBytesValue);
    }

    @Override
    protected void processAdditionalRecordsCount() throws Pausable {
        pass(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void processAuthorityRecordsCount() throws Pausable {
        pass(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void processAnswerRecordsCount() throws Pausable {
        pass(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected int processQuestionRecordsCount() throws Pausable {
        int questionRecordsCount = byteBuffer.getShort() & 0xFFFF;
        pass(questionRecordsCount);
        return questionRecordsCount;
    }

    @Override
    protected void processId() throws Pausable {
        pass(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void processFlags() throws Pausable {
        int flags = byteBuffer.getShort() & 0xFFFF;
        while (Field.END_FLGAS != currentField) {
            switch (currentField) {
                case OPCODE:
                    pass((flags >> 11) & 0xF);
                    break;
                case FLAG_QR:
                    pass(getFlag(flags, 0));
                    break;
                case FLAG_AA:
                    pass(getFlag(flags, 5));
                    break;
                case FLAG_TC:
                    pass(getFlag(flags, 6));
                    break;
                case FLAG_RD:
                    pass(getFlag(flags, 7));
                    break;
                default:
                    throw new RuntimeException("unexpected field: " + currentField);
            }
        }
    }

    private boolean getFlag(int flags, int bit) {
        return (flags & (1 << (15 - bit))) != 0;
    }

    @Override
    protected void processRecordInetAddress() throws Pausable {
        fieldBytesValue = new byte[4];
        byteBuffer.get(fieldBytesValue);
        pass();
    }

    @Override
    protected void processRecordDataLength() throws Pausable {
        pass(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void skipRecordData() throws Pausable {
        int dataLength = byteBuffer.getShort() & 0xFFFF;
        byteBuffer.position(byteBuffer.position() + dataLength);
        pass();
    }

    @Override
    protected void processRecordTTL() throws Pausable {
        pass(byteBuffer.getInt() & 0xFFFFFFFFL);
    }

    @Override
    protected void processRecordDClass() throws Pausable {
        pass(byteBuffer.getShort() & 0xFFFF);
    }

    @Override
    protected void processRecordType() throws Pausable {
        pass(byteBuffer.getShort() & 0xFFFF);

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
                fieldStringValue = null;
                pass();
                break;
            }
            switch (len & 0xC0)
            {
                case 0x00:
                    //buf.append("[" + off + "]");
                    pass(readUTF(len));
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
        if (savePoint >= 0) {
            byteBuffer.position(savePoint);
        }
    }

    private String readUTF(int len) throws UnsupportedEncodingException {
        byte[] bytes = new byte[len];
        byteBuffer.get(bytes);
        return new String(bytes, "UTF8");
    }

    protected void pass(long val) throws Pausable {
        fieldLongValue = val;
        pass();
    }

    protected void pass(int val) throws Pausable {
        fieldIntValue = val;
        pass();
    }

    protected void pass(boolean val) throws Pausable {
        fieldBooleanValue = val;
        pass();
    }

    protected void pass(String val) throws Pausable {
        fieldStringValue = val;
        pass();
    }
}
