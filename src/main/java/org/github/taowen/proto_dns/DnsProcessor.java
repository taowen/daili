package org.github.taowen.proto_dns;

import kilim.Pausable;
import kilim.Task;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;

public abstract class DnsProcessor extends Task {

    /** A standard query */
    public static final int OPCODE_QUERY		= 0;

    /** Address */
    public static final int TYPE_A		= 1;

    /** Internet */
    public static final int DCLASS_IN		= 1;

    protected static enum Field {
        OPCODE, FLAG_QR, FLAG_AA, FLAG_TC, FLAG_RD,
        QUESTION_RECORDS_COUNT,
        ANSWER_RECORDS_COUNT,
        AUTHORITY_RECORDS_COUNT,
        ADDITIONAL_RECORDS_COUNT,
        START_FLAGS, END_FLGAS,
        START_RECORD, RECORD_NAME, RECORD_NAME_LABEL, RECORD_TYPE, RECORD_DCLASS, END_RECORD,
        RECORD_DATA_LENGTH, RECORD_TTL, RECORD_INET_ADDRESS, ID
    }

    protected ByteBuffer byteBuffer;
    protected Queue<Field> processingFields;

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

    protected Field nextField() throws Pausable {
        Field nextField = processingFields.poll();
        if (null != nextField) {
            return nextField;
        }
        yield();
        return processingFields.remove();
    }

    private int processHeader() throws Pausable {
        assert Field.ID == nextField();
        processId();
        assert Field.START_FLAGS == nextField();
        processFlags();
        assert Field.QUESTION_RECORDS_COUNT == nextField();
        int questionRecordsCount = processQuestionRecordsCount();
        assert Field.ANSWER_RECORDS_COUNT == nextField();
        processAnswerRecordsCount();
        assert Field.AUTHORITY_RECORDS_COUNT == nextField();
        processAuthorityRecordsCount();
        assert Field.ADDITIONAL_RECORDS_COUNT == nextField();
        processAdditionalRecordsCount();
        return questionRecordsCount;
    }

    private void processRecord(boolean isQuestionRecord) throws Pausable, IOException {
        assert Field.START_RECORD == nextField();
        assert Field.RECORD_NAME == nextField();
        processRecordName();
        assert Field.RECORD_TYPE == nextField();
        processRecordType();
        assert Field.RECORD_DCLASS == nextField();
        processRecordDClass();
        if (!isQuestionRecord) {
            assert Field.RECORD_TTL == nextField();
            processRecordTTL();
            Field nextField = nextField();
            if (Field.END_RECORD == nextField) {
                skipRecordData();
                return;
            }
            assert Field.RECORD_DATA_LENGTH == nextField;
            processRecordDataLength();
            assert Field.RECORD_INET_ADDRESS == nextField();
            processRecordInetAddress();
        }
        assert Field.END_RECORD == nextField();
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

    public void byteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public ByteBuffer byteBuffer() {
        return byteBuffer;
    }

    public void startFlags() {
        processingFields.offer(Field.START_FLAGS);
        run();
    }

    public void endFlags() {
        processingFields.offer(Field.END_FLGAS);
        run();
    }

    public void startRecord() throws EOFException {
        processingFields.offer(Field.START_RECORD);
        run();
    }

    public void endRecord() throws EOFException {
        processingFields.offer(Field.END_RECORD);
        run();
    }
}
