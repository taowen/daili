package org.github.taowen.proto_dns;

import kilim.Pausable;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

public class DnsWriter extends DnsProcessor {

    protected Queue<Object> input;

    public DnsWriter() {
        processingFields = new LinkedList<Field>();
        input = new LinkedList<Object>();
    }

    @Override
    protected void processRecordInetAddress() throws Pausable {
        byte[] fieldBytesValue = (byte[])input.remove();
        for (byte b : fieldBytesValue) {
            byteBuffer.put(b);
        }
    }

    @Override
    protected void processRecordDataLength() throws Pausable {
        int fieldIntValue = (Integer)input.remove();
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
    }

    @Override
    protected void skipRecordData() throws Pausable {
        throw new UnsupportedOperationException("writer can not skip");
    }

    @Override
    protected void processRecordTTL() throws Pausable {
        long fieldLongValue = (Long)input.remove();
        byteBuffer.putInt((int) (fieldLongValue & 0xFFFFFFFFL));
    }

    @Override
    protected void processRecordDClass() throws Pausable {
        int fieldIntValue = (Integer)input.remove();
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
    }

    @Override
    protected void processRecordType() throws Pausable {
        int fieldIntValue = (Integer)input.remove();
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
    }

    @Override
    protected void processRecordName() throws IOException, Pausable {
        while (true) {
            assert Field.RECORD_NAME_LABEL == nextField();
            int fieldIntValue = (Integer)input.remove();
            String fieldStringValue = (String)input.remove();
            if (fieldIntValue > 0) {
                int pos = fieldIntValue;
                pos |= (0xC0 << 8);
                byteBuffer.putShort((short) (pos & 0xFFFF));
                return;
            }
            if (null == fieldStringValue) {
                byteBuffer.put((byte) 0);
                return;
            }
            byte[] bytes = fieldStringValue.getBytes("UTF8");
            byteBuffer.put((byte)bytes.length);
            byteBuffer.put(bytes);
        }
    }

    @Override
    protected void processAdditionalRecordsCount() throws Pausable {
        int fieldIntValue = (Integer)input.remove();
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
    }

    @Override
    protected void processAuthorityRecordsCount() throws Pausable {
        int fieldIntValue = (Integer)input.remove();
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
    }

    @Override
    protected void processAnswerRecordsCount() throws Pausable {
        int fieldIntValue = (Integer)input.remove();
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
    }

    @Override
    protected int processQuestionRecordsCount() throws Pausable {
        int fieldIntValue = (Integer)input.remove();
        int questionRecordsCount = fieldIntValue;
        byteBuffer.putShort((short) (questionRecordsCount & 0xFFFF));
        return questionRecordsCount;
    }

    @Override
    protected void processId() throws Pausable {
        int fieldIntValue = (Integer)input.remove();
        byteBuffer.putShort((short) (fieldIntValue & 0xFFFF));
    }

    @Override
    protected void processFlags() throws Pausable {
        int flags = 0;
        Field nextField = nextField();
        while (Field.END_FLGAS != nextField) {
            switch (nextField) {
                case OPCODE:
                    flags &= 0x87FF;
                    flags |= ((Integer)input.remove() << 11);
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
                    throw new RuntimeException("unexpected field: " + nextField);
            }
            nextField = nextField();
        }
        byteBuffer.putShort((short) (flags & 0xFFFF));
    }

    private int setFlag(int flags, int bit) throws Pausable {
        if ((Boolean)input.remove()) {
            flags |= (1 << (15 - bit));
        } else {
            flags &= ~(1 << (15 - bit));
        }
        return flags;
    }

    public void writeId(int id) {
        processingFields.offer(Field.ID);
        input.offer(id);
        run();
    }

    public void writeFlagRD(boolean val) {
        processingFields.offer(Field.FLAG_RD);
        input.offer(val);
        run();
    }

    public void writeOpcode(int opcode) {
        processingFields.offer(Field.OPCODE);
        input.offer(opcode);
        run();
    }

    public void writeQuestionRecordsCount(int count) {
        processingFields.offer(Field.QUESTION_RECORDS_COUNT);
        input.offer(count);
        run();
    }

    public void writeAnswerRecordsCount(int count) {
        processingFields.offer(Field.ANSWER_RECORDS_COUNT);
        input.offer(count);
        run();

    }

    public void writeAuthorityRecordsCount(int count) {
        processingFields.offer(Field.AUTHORITY_RECORDS_COUNT);
        input.offer(count);
        run();

    }

    public void writeAdditionalRecordsCount(int count) {
        processingFields.offer(Field.ADDITIONAL_RECORDS_COUNT);
        input.offer(count);
        run();
    }

    public void writeRecordName(String recordName) {
        processingFields.offer(Field.RECORD_NAME);
        run();
        for (String label : recordName.split("\\.")) {
            writeRecordNameLabel(label);
        }
        writeRecordNameLabel(null);
    }

    public void writeRecordNameLabel(int position) {
        processingFields.offer(Field.RECORD_NAME_LABEL);
        input.offer(position);
        input.offer(null);
        run();

    }

    public void writeRecordNameLabel(String label) {
        processingFields.offer(Field.RECORD_NAME_LABEL);
        input.offer(0);
        input.offer(label);
        run();
    }

    public void writeRecordType(int type) {
        processingFields.offer(Field.RECORD_TYPE);
        input.offer(type);
        run();
    }

    public void writeRecordDClass(int dclass) {
        processingFields.offer(Field.RECORD_DCLASS);
        input.offer(dclass);
        run();
    }

    public void writeRecordTTL(long ttl) {
        processingFields.offer(Field.RECORD_TTL);
        input.offer(ttl);
        run();
    }

    public void writeRecordDataLength(int dataLength) {
        processingFields.offer(Field.RECORD_DATA_LENGTH);
        input.offer(dataLength);
        run();
    }

    public void writeRecordInetAddress(InetAddress address) {
        processingFields.offer(Field.RECORD_INET_ADDRESS);
        input.offer(address.getAddress());
        run();
    }
}
