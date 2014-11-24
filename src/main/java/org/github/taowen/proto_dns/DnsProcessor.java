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

    protected Field nextField() throws Pausable {
        Field nextField = processingFields.poll();
        if (null != nextField) {
            return nextField;
        }
        yield();
        nextField = processingFields.remove();
        return nextField;
    }

    protected void assertNextField(Field expected) throws Pausable {
        Field nextField = nextField();
        assert expected == nextField;
    }

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
        assertNextField(Field.ID);
        processId();
        assertNextField(Field.START_FLAGS);
        processFlags();
        assertNextField(Field.QUESTION_RECORDS_COUNT);
        int questionRecordsCount = processQuestionRecordsCount();
        assertNextField(Field.ANSWER_RECORDS_COUNT);
        processAnswerRecordsCount();
        assertNextField(Field.AUTHORITY_RECORDS_COUNT);
        processAuthorityRecordsCount();
        assertNextField(Field.ADDITIONAL_RECORDS_COUNT);
        processAdditionalRecordsCount();
        return questionRecordsCount;
    }

    private void processRecord(boolean isQuestionRecord) throws Pausable, IOException {
        assertNextField(Field.START_RECORD);
        assertNextField(Field.RECORD_NAME);
        processRecordName();
        assertNextField(Field.RECORD_TYPE);
        processRecordType();
        assertNextField(Field.RECORD_DCLASS);
        processRecordDClass();
        if (!isQuestionRecord) {
            assertNextField(Field.RECORD_TTL);
            processRecordTTL();
            Field nextField = nextField();
            if (Field.END_RECORD == nextField) {
                skipRecordData();
                return;
            }
            assertNextField(Field.RECORD_DATA_LENGTH);
            processRecordDataLength();
            assertNextField(Field.RECORD_INET_ADDRESS);
            processRecordInetAddress();
        }
        assertNextField(Field.END_RECORD);
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
