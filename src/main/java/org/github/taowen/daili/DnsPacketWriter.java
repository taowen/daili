package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;

public class DnsPacketWriter extends DnsPacketProcessor {

    protected int fieldIntValue;
    protected boolean fieldBooleanValue;
    protected String fieldStringValue;

    @Override
    protected void processRecordInetAddress() throws Pausable {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void processRecordDataLength() throws Pausable {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void skipRecordData() throws Pausable {
        throw new UnsupportedOperationException("writer can not skip");
    }

    @Override
    protected void processRecordTTL() throws Pausable {
        throw new UnsupportedOperationException();
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
            int bit = -1;
            switch (currentField) {
                case OPCODE:
                    flags &= 0x87FF;
                    flags |= (fieldIntValue << 11);
                    pass();
                    break;
                case FLAG_QR:
                    bit = 0;
                    break;
                case FLAG_AA:
                    bit = 5;
                    break;
                case FLAG_TC:
                    bit = 6;
                    break;
                case FLAG_RD:
                    bit = 7;
                    break;
                default:
                    throw new RuntimeException("unexpected field: " + currentField);
            }
            if (bit >= 0) {
                if (fieldBooleanValue) {
                    flags |= (1 << (15 - bit));
                } else {
                    flags &= ~(1 << (15 - bit));
                }
                pass();
            }
        }
        byteBuffer.putShort((short) (flags & 0xFFFF));
        pass();
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

    public void writeRecordNameLabel(String label) {
        currentField = Field.RECORD_NAME;
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
}
