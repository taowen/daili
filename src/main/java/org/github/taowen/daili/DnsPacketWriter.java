package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;

public class DnsPacketWriter extends DnsPacketProcessor {

    protected int fieldIntValue;

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
        throw new UnsupportedOperationException();
    }

    @Override
    protected void processRecordType() throws Pausable {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void processRecordName() throws IOException, Pausable {
        throw new UnsupportedOperationException();
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
        assert Field.END_FLGAS == currentField;
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
        pass();
    }

    public void writeId(int id) {
        currentField = Field.ID;
        fieldIntValue = id;
        resume();
    }

    public void startFlags() {
        super.startFlags();
        fieldIntValue = 0;
    }

    public void writeOpcode(int opcode) {
        fieldIntValue &= 0x87FF;
        fieldIntValue |= (opcode << 11);
    }

    public void writeQuestionRecordsCount(int count) {
        currentField = Field.QUESTION_RECORDS_COUNT;
        fieldIntValue = count;
        resume();
    }

    public void writeAnswerRecordsCount(int count) {
        currentField = Field.ANSWER_RECORDS_COUNT;
        fieldIntValue = count;
        resume();

    }

    public void writeAuthorityRecordsCount(int count) {
        currentField = Field.AUTHORITY_RECORDS_COUNT;
        fieldIntValue = count;
        resume();

    }

    public void writeAdditionalRecordsCount(int count) {
        currentField = Field.ADDITIONAL_RECORDS_COUNT;
        fieldIntValue = count;
        resume();
    }
}
