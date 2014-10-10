package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Section;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class DnsPacket extends Task {

    public static enum Field {
        OPCODE, FLAG_QR, FLAG_AA, FLAG_TC,
        QUESTION_RECORDS_COUNT,
        ANSWER_RECORDS_COUNT,
        AUTHORITY_RECORDS_COUNT,
        ADDITIONAL_RECORDS_COUNT,
        START_FLAGS, END_FLGAS,
        SKIP_HEADER, // FIXME: remove this
        START_RECORD, RECORD_NAME, RECORD_TYPE, RECORD_DCLASS, END_RECORD,
        RECORD_DATA_LENGTH, RECORD_TTL, ID
    }

    private ByteBuffer byteBuffer;
    private Field readingField;
    private long fieldLongValue;
    private int fieldIntValue;
    private String fieldStringValue;
    private boolean fieldBooleanValue;
    private boolean skipping;

    @Override
    public void execute() throws Pausable, Exception {
        int questionRecordsCount = doReadHeader();
        for (int i = 0; i < questionRecordsCount; i++) {
            doReadRecord(true);
        }
        while(true) {
            doReadRecord(false);
        }
    }

    private void doReadRecord(boolean isQuestionRecord) throws Pausable, IOException {
        assert Field.START_RECORD == readingField;
        pass();
        assert Field.RECORD_NAME == readingField;
        doReadRecordName();
        assert Field.RECORD_TYPE == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        assert Field.RECORD_DCLASS == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        if (!isQuestionRecord) {
            assert Field.RECORD_TTL == readingField;
            pass(byteBuffer.getInt() & 0xFFFFFFFFL);
            assert Field.RECORD_DATA_LENGTH == readingField;
            pass(byteBuffer.getShort() & 0xFFFF);
        }
        assert Field.END_RECORD == readingField;
        pass();
    }

    private void doReadRecordName() throws IOException, Pausable {
        StringBuffer buf = new StringBuffer();
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
                    readUTF(buf, len);
                    buf.append('.');
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
                    throw new IOException("bad domain name: '" + buf + "' at " + byteBuffer.position());
            }
        }
        if (savePoint >= 0) {
            byteBuffer.position(savePoint);
        }
        pass(buf.toString());
    }

    private void readUTF(StringBuffer buf, int len)
    {
        for (int end = byteBuffer.position() + len; byteBuffer.position() < end;)
        {
            int ch = byteBuffer.get();
            switch (ch >> 4)
            {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    // 0xxxxxxx
                    break;
                case 12:
                case 13:
                    // 110x xxxx   10xx xxxx
                    ch = ((ch & 0x1F) << 6) | (byteBuffer.get() & 0x3F);
                    break;
                case 14:
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    ch = ((ch & 0x0f) << 12) | ((byteBuffer.get() & 0x3F) << 6) | (byteBuffer.get() & 0x3F);
                    break;
                default:
                    // 10xx xxxx,  1111 xxxx
                    ch = ((ch & 0x3F) << 4) | (byteBuffer.get() & 0x0f);
                    break;
            }
            buf.append((char) ch);
        }
    }

    private int doReadHeader() throws Pausable {
        assert Field.ID == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        readFlags();
        assert Field.QUESTION_RECORDS_COUNT == readingField;
        int questionRecordsCount = byteBuffer.getShort() & 0xFFFF;
        pass(questionRecordsCount);
        assert Field.ANSWER_RECORDS_COUNT == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        assert Field.AUTHORITY_RECORDS_COUNT == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        assert Field.ADDITIONAL_RECORDS_COUNT == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        return questionRecordsCount;
    }

    private void readFlags() throws Pausable {
        assert Field.START_FLAGS == readingField;
        pass();
        int flags = byteBuffer.getShort() & 0xFFFF;
        while (Field.END_FLGAS != readingField) {
            switch (readingField) {
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
                default:
                    throw new RuntimeException("unexpected field: " + readingField);
            }
        }
        pass();
    }

    private void pass() throws Pausable {
        if (!skipping) {
            yield();
        }
    }

    private void pass(long val) throws Pausable {
        fieldLongValue = val;
        if (!skipping) {
            yield();
        }
    }

    private void pass(int val) throws Pausable {
        fieldIntValue = val;
        if (!skipping) {
            yield();
        }
    }

    private void pass(boolean val) throws Pausable {
        fieldBooleanValue = val;
        if (!skipping) {
            yield();
        }
    }

    private void pass(String val) throws Pausable {
        fieldStringValue = val;
        if (!skipping) {
            yield();
        }
    }

    private boolean getFlag(int flags, int bit) {
        return (flags & (1 << (15 - bit))) != 0;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
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
        readingField = Field.ID;
        resume();
        return fieldIntValue;
    }

    public void startFlags() {
        readingField = Field.START_FLAGS;
        resume();
    }

    public void endFlags() {
        readingField = Field.END_FLGAS;
        resume();
    }

    public int readOpcode() {
        readingField = Field.OPCODE;
        resume();
        return fieldIntValue;
    }

    public boolean readFlagQR() {
        readingField = Field.FLAG_QR;
        resume();
        return fieldBooleanValue;
    }

    public boolean readFlagAA() {
        readingField = Field.FLAG_AA;
        resume();
        return fieldBooleanValue;
    }

    public boolean readFlagTC() {
        readingField = Field.FLAG_TC;
        resume();
        return fieldBooleanValue;
    }

    public int readQuestionRecordsCount() {
        readingField = Field.QUESTION_RECORDS_COUNT;
        resume();
        return fieldIntValue;
    }

    public int readAnswerRecordsCount() {
        readingField = Field.ANSWER_RECORDS_COUNT;
        resume();
        return fieldIntValue;
    }

    public int readAuthorityRecordsCount() {
        readingField = Field.AUTHORITY_RECORDS_COUNT;
        resume();
        return fieldIntValue;
    }

    public int readAdditionalRecordsCount() {
        readingField = Field.ADDITIONAL_RECORDS_COUNT;
        resume();
        return fieldIntValue;
    }

    public void startRecord() throws EOFException {
        checkEOF();
        readingField = Field.START_RECORD;
        resume();
    }

    public void endRecord() throws EOFException {
        readingField = Field.END_RECORD;
        resume();
    }

    public String readRecordName() {
        readingField = Field.RECORD_NAME;
        resume();
        return fieldStringValue;
    }

    public int readRecordType() {
        readingField = Field.RECORD_TYPE;
        resume();
        return fieldIntValue;
    }

    public int readRecordDClass() {
        readingField = Field.RECORD_DCLASS;
        resume();
        return fieldIntValue;
    }

    public long readRecordTTL() {
        readingField = Field.RECORD_TTL;
        resume();
        return fieldLongValue;
    }

    public int readRecordDataLength() {
        readingField = Field.RECORD_DATA_LENGTH;
        resume();
        return fieldIntValue;
    }

    private void checkEOF() throws EOFException {
        if (!byteBuffer.hasRemaining()) {
            throw new EOFException();
        }
    }
}
