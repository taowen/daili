package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class DnsPacketProcessor extends Task {

    protected static enum Field {
        OPCODE, FLAG_QR, FLAG_AA, FLAG_TC,
        QUESTION_RECORDS_COUNT,
        ANSWER_RECORDS_COUNT,
        AUTHORITY_RECORDS_COUNT,
        ADDITIONAL_RECORDS_COUNT,
        START_FLAGS, END_FLGAS,
        START_RECORD, RECORD_NAME, RECORD_TYPE, RECORD_DCLASS, END_RECORD,
        RECORD_DATA_LENGTH, RECORD_TTL, RECORD_INET_ADDRESS, ID
    }

    protected ByteBuffer byteBuffer;
    protected Field currentField;
    protected long fieldLongValue;
    protected int fieldIntValue;
    protected byte[] fieldBytesValue;
    protected String fieldStringValue;
    protected boolean fieldBooleanValue;

    @Override
    public void execute() throws Pausable, Exception {
        int questionRecordsCount = processHeader();
        for (int i = 0; i < questionRecordsCount; i++) {
            processRecord(true);
        }
        while(true) {
            processRecord(false);
        }
    }

    private int processHeader() throws Pausable {
        assert Field.ID == currentField;
        processId();
        processFlags();
        assert Field.QUESTION_RECORDS_COUNT == currentField;
        int questionRecordsCount = processQuestionRecordsCount();
        assert Field.ANSWER_RECORDS_COUNT == currentField;
        processAnswerRecordsCount();
        assert Field.AUTHORITY_RECORDS_COUNT == currentField;
        processAuthorityRecordsCount();
        assert Field.ADDITIONAL_RECORDS_COUNT == currentField;
        processAdditionalRecordsCount();
        return questionRecordsCount;
    }

    private void processRecord(boolean isQuestionRecord) throws Pausable, IOException {
        assert Field.START_RECORD == currentField;
        pass();
        assert Field.RECORD_NAME == currentField;
        processRecordName();
        assert Field.RECORD_TYPE == currentField;
        processRecordType();
        assert Field.RECORD_DCLASS == currentField;
        processRecordDClass();
        if (!isQuestionRecord) {
            assert Field.RECORD_TTL == currentField;
            processRecordTTL();
            if (Field.END_RECORD == currentField) {
                skipRecordData();
                return;
            }
            assert Field.RECORD_DATA_LENGTH == currentField;
            processRecordDataLength();
            assert Field.RECORD_INET_ADDRESS == currentField;
            processRecordInetAddress();
        }
        assert Field.END_RECORD == currentField;
        pass();
    }

    protected abstract void processRecordInetAddress() throws Pausable;

    protected abstract void processRecordDataLength() throws Pausable;

    protected abstract void skipRecordData() throws Pausable;

    protected abstract void processRecordTTL() throws Pausable;

    protected abstract void processRecordDClass() throws Pausable;

    protected abstract void processRecordType() throws Pausable;

    protected abstract void processRecordName() throws IOException, Pausable;

    protected abstract void processAdditionalRecordsCount() throws Pausable;

    protected abstract void processAuthorityRecordsCount() throws Pausable;

    protected abstract void processAnswerRecordsCount() throws Pausable;

    protected abstract int processQuestionRecordsCount() throws Pausable;

    protected abstract void processId() throws Pausable;

    protected abstract void processFlags() throws Pausable;

    protected void pass() throws Pausable {
        yield();
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public void startFlags() {
        currentField = Field.START_FLAGS;
        resume();
    }

    public void endFlags() {
        currentField = Field.END_FLGAS;
        resume();
    }

    public void startRecord() throws EOFException {
        currentField = Field.START_RECORD;
        resume();
    }

    public void endRecord() throws EOFException {
        currentField = Field.END_RECORD;
        resume();
    }
}
