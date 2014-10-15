package org.github.taowen.proto_http;

import kilim.Pausable;
import kilim.Task;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class HttpReader extends Task {

    private Iterator<ByteBuffer> byteBufferStream;
    protected String fieldStringValue;
    private ByteBuffer currentByteBuffer;
    private Field currentField;

    private static enum Field {
        METHOD, URL
    }
    private static final byte CR = 0x0d;
    private static final byte LF = 0x0a;

    public void byteBufferStream(Iterator<ByteBuffer> byteBufferStream) {
        this.byteBufferStream = byteBufferStream;
        currentByteBuffer = byteBufferStream.next();
    }

    @Override
    public void execute() throws Pausable, Exception {
        assert Field.METHOD == currentField;
        String method = processMethod();
        fieldStringValue = method;
        pass();
        if (null == method) {
            return; // empty request
        }
        assert Field.URL == currentField;
        String url = processUrl();
        pass(url);
    }

    private byte skipEmptySpaces() {
        byte b;
        while ((b = get()) == ' ') {
            // skip
        }
        return b;
    }

    private String processMethod() throws Pausable {
        byte b = get();
        if (CR == b || LF == b) {
            return null;
        }
        StringBuilder method = new StringBuilder();
        while (b != ' ') {
            method.append((char)b);
            b = get();
        }
        return method.toString();
    }

    private String processUrl() throws Pausable {
        byte b = skipEmptySpaces();
        StringBuilder url = new StringBuilder();
        while (b != ' ') {
            if (CR == b || LF == b) {
                return url.toString();
            }
            url.append((char)b);
            b = get();
        }
        return url.toString();
    }

    private byte get() {
        if (!currentByteBuffer.hasRemaining()) {
            currentByteBuffer = byteBufferStream.next();
        }
        return currentByteBuffer.get();
    }

    private void pass(String val) throws Pausable {
        fieldStringValue = val;
        pass();
    }

    protected void pass() throws Pausable {
        yield();
    }

    public String readMethod() {
        currentField = Field.METHOD;
        run();
        return fieldStringValue;
    }

    public String readUrl() {
        currentField = Field.URL;
        run();
        return fieldStringValue;
    }
}
