package org.github.taowen.proto_dns;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetAddress;

public class DnsPacketWriter extends DnsPacketProcessor {

    protected long fieldLongValue;
    protected int fieldIntValue;
    protected boolean fieldBooleanValue;
    protected byte[] fieldBytesValue;
    protected String fieldStringValue;

    @Override
    protected void processRecordInetAddress() throws Pausable {
        for (byte b : fieldBytesValue) {
            byteBuffer.put(b);
        }
        pass();
    }

    @Override
    protected void processRecordDataLength() throws Pausable {
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
        pass();
    }

    @Override
    protected void skipRecordData() throws Pausable {
        throw new UnsupportedOperationException("writer can not skip");
    }

    @Override
    protected void processRecordTTL() throws Pausable {
        byteBuffer.putInt((int) (fieldLongValue & 0xFFFFFFFFL));
        pass();
    }

    @Override
    protected void processRecordDClass() throws Pausable {
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
        pass();
    }

    @Override
    protected void processRecordType() throws Pausable {
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
        pass();
    }

    @Override
    protected void processRecordName() throws IOException, Pausable {
        while (true) {
            assert Field.RECORD_NAME == currentField;
            if (fieldIntValue > 0) {
                int pos = fieldIntValue;
                pos |= (0xC0 << 8);
                byteBuffer.putShort((short) (pos & 0xFFFF));
                pass();
                return;
            }
            if (null == fieldStringValue) {
                byteBuffer.put((byte) 0);
                pass();
                return;
            }
            byte[] bytes = fieldStringValue.getBytes("UTF8");
            byteBuffer.put((byte)bytes.length);
            byteBuffer.put(bytes);
            pass();
        }
    }

    @Override
    protected void processAdditionalRecordsCount() throws Pausable {
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
        pass();
    }

    @Override
    protected void processAuthorityRecordsCount() throws Pausable {
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
        pass();
    }

    @Override
    protected void processAnswerRecordsCount() throws Pausable {
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
        pass();
    }

    @Override
    protected int processQuestionRecordsCount() throws Pausable {
        int questionRecordsCount = fieldIntValue;
        byteBuffer.putShort((short) (questionRecordsCount & 0xFFFF));
        pass();
        return questionRecordsCount;
    }

    @Override
    protected void processId() throws Pausable {
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
        pass();
    }

    @Override
    protected void processFlags() throws Pausable {
        int flags = 0;
        while (Field.END_FLGAS != currentField) {
            switch (currentField) {
                case OPCODE:
                    flags &= 0x87FF;
                    flags |= (fieldIntValue << 11);
                    pass();
                    break;
                case FLAG_QR:
                    flags = setFlag(flags, 0);
                    break;
                case FLAG_AA:
                    flags = setFlag(flags, 5);
                    break;
                case FLAG_TC:
                    flags = setFlag(flags, 6);
                    break;
                case FLAG_RD:
                    flags = setFlag(flags, 7);
                    break;
                default:
                    throw new RuntimeException("unexpected field: " + currentField);
            }
        }
        byteBuffer.putShort((short) (flags & 0xFFFF));
    }

    private int setFlag(int flags, int bit) throws Pausable {
        if (fieldBooleanValue) {
            flags |= (1 << (15 - bit));
        } else {
            flags &= ~(1 << (15 - bit));
        }
        pass();
        return flags;
    }

    public void writeId(int id) {
        currentField = Field.ID;
        fieldIntValue = id;
        run();
    }

    public void writeFlagRD(boolean val) {
        currentField = Field.FLAG_RD;
        fieldBooleanValue = val;
        run();
    }

    public void writeOpcode(int opcode) {
        currentField = Field.OPCODE;
        fieldIntValue = opcode;
        run();
    }

    public void writeQuestionRecordsCount(int count) {
        currentField = Field.QUESTION_RECORDS_COUNT;
        fieldIntValue = count;
        run();
    }

    public void writeAnswerRecordsCount(int count) {
        currentField = Field.ANSWER_RECORDS_COUNT;
        fieldIntValue = count;
        run();

    }

    public void writeAuthorityRecordsCount(int count) {
        currentField = Field.AUTHORITY_RECORDS_COUNT;
        fieldIntValue = count;
        run();

    }

    public void writeAdditionalRecordsCount(int count) {
        currentField = Field.ADDITIONAL_RECORDS_COUNT;
        fieldIntValue = count;
        run();
    }

    public void writeRecordName(String recordName) {
        for (String label : recordName.split("\\.")) {
            writeRecordNameLabel(label);
        }
        writeRecordNameLabel(null);
    }

    public void writeRecordNameLabel(int position) {
        currentField = Field.RECORD_NAME;
        fieldIntValue = position;
        fieldStringValue = null;
        run();

    }

    public void writeRecordNameLabel(String label) {
        currentField = Field.RECORD_NAME;
        fieldIntValue = 0;
        fieldStringValue = label;
        run();
    }

    public void writeRecordType(int type) {
        currentField = Field.RECORD_TYPE;
        fieldIntValue = type;
        run();
    }

    public void writeRecordDClass(int dclass) {
        currentField = Field.RECORD_DCLASS;
        fieldIntValue = dclass;
        run();
    }

    public void writeRecordTTL(long ttl) {
        currentField = Field.RECORD_TTL;
        fieldLongValue = ttl;
        run();
    }

    public void writeRecordDataLength(int dataLength) {
        currentField = Field.RECORD_DATA_LENGTH;
        fieldIntValue = dataLength;
        run();
    }

    public void writeRecordInetAddress(InetAddress address) {
        currentField = Field.RECORD_INET_ADDRESS;
        fieldBytesValue = address.getAddress();
        run();
    }
}
