package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class DnsPacket extends Task {

    public static enum Field {
        OPCODE, FLAG_QR, FLAG_AA, FLAG_TC, QUESTION_RECORDS_COUNT, ANSWER_RECORDS_COUNT, AUTHORITY_RECORDS_COUNT, ADDITIONAL_RECORDS_COUNT, START_FLAGS, END_FLGAS, SKIP_HEADER, START_QUESTION_RECORD, RECORD_NAME, ID
    }
    public static Set<Field> FLAG_FIELDS = new HashSet<Field>(){{
        add(Field.OPCODE);
        add(Field.FLAG_QR);
        add(Field.FLAG_AA);
        add(Field.FLAG_TC);
    }};

    private ByteBuffer byteBuffer;
    private Field readingField;
    private int fieldIntValue;
    private String fieldStringValue;
    private boolean fieldBooleanValue;
    private boolean skipping;

    @Override
    public void execute() throws Pausable, Exception {
        assert Field.SKIP_HEADER == readingField || Field.ID == readingField;
        if (Field.SKIP_HEADER == readingField) {
            skipping = true;
        }
        pass(byteBuffer.getShort() & 0xFFFF);
        readFlags();
        assert Field.SKIP_HEADER == readingField || Field.QUESTION_RECORDS_COUNT == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        assert Field.SKIP_HEADER == readingField || Field.ANSWER_RECORDS_COUNT == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        assert Field.SKIP_HEADER == readingField || Field.AUTHORITY_RECORDS_COUNT == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        assert Field.SKIP_HEADER == readingField || Field.ADDITIONAL_RECORDS_COUNT == readingField;
        pass(byteBuffer.getShort() & 0xFFFF);
        if (Field.SKIP_HEADER == readingField) {
            skipping = false;
            pass();
        }
        assert Field.START_QUESTION_RECORD == readingField;
        pass();
        assert Field.RECORD_NAME == readingField;
        System.out.println(byteBuffer.get());
//        while (true)
//        {
//            int len = byteBuffer.get();
//            if (len == 0)
//            {
//                break;
//            }
//            switch (len & 0xC0)
//            {
//                case 0x00:
//                    //buf.append("[" + off + "]");
//                    readUTF(buf, off, len);
//                    off += len;
//                    buf.append('.');
//                    break;
//                case 0xC0:
//                    //buf.append("<" + (off - 1) + ">");
//                    if (next < 0)
//                    {
//                        next = off + 1;
//                    }
//                    off = ((len & 0x3F) << 8) | get(off++);
//                    if (off >= first)
//                    {
//                        throw new IOException("bad domain name: possible circular name detected");
//                    }
//                    first = off;
//                    break;
//                default:
//                    throw new IOException("bad domain name: '" + buf + "' at " + off);
//            }
//        }
    }

    private void readFlags() throws Pausable {
        assert Field.SKIP_HEADER == readingField || Field.START_FLAGS == readingField;
        pass();
        int flags = byteBuffer.getShort() & 0xFFFF;
        if (Field.SKIP_HEADER == readingField) {
            return;
        }
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

    private boolean getFlag(int flags, int bit) {
        return (flags & (1 << (15 - bit))) != 0;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public void skipHeader() {
        readingField = Field.SKIP_HEADER;
        resume();
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

    public void enterQuestionRecord() throws EOFException {
        checkEOF();
        readingField = Field.START_QUESTION_RECORD;
        resume();
    }

    public String readName() {
        readingField = Field.RECORD_NAME;
        resume();
        return fieldStringValue;
    }

    private void checkEOF() throws EOFException {
        if (!byteBuffer.hasRemaining()) {
            throw new EOFException();
        }
    }
}
