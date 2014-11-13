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
        METHOD, VERSION, HEADER_KEY, HEADER_VALUE, URL
    }
    private static final byte CR = 0x0d;
    private static final byte LF = 0x0a;
    private static final String DEFAULT_VERSION = "HTTP/0.9";

    public void byteBufferStream(Iterator<ByteBuffer> byteBufferStream) {
        this.byteBufferStream = byteBufferStream;
        currentByteBuffer = byteBufferStream.next();
    }

    @Override
    public void execute() throws Pausable, Exception {
        processRequestLine();
        processHeaders();
    }

    protected void processRequestLine() throws Pausable {
        assert Field.METHOD == currentField;
        if (!processMethod()) {
            return;
        }
        assert Field.URL == currentField;
        if (!processUrl()) {
            return;
        }
        assert Field.VERSION == currentField;
        processVersion();
    }

    protected boolean processMethod() throws Pausable {
        byte b = get();
        if (CR == b || LF == b) {
            consumeLF(b);
            pass();
            return false;
        }
        StringBuilder method = new StringBuilder();
        while (b != ' ') {
            method.append((char)b);
            b = get();
        }
        pass(method.toString());
        return true;
    }

    protected boolean processUrl() throws Pausable {
        byte b = skipEmptySpaces();
        StringBuilder url = new StringBuilder();
        while (b != ' ') {
            if (CR == b || LF == b) {
                consumeLF(b);
                pass(url.toString());
                assert Field.VERSION == currentField;
                pass(DEFAULT_VERSION);
                return false;
            }
            url.append((char)b);
            b = get();
        }
        pass(url.toString());
        return true;
    }

    protected void processVersion() throws Pausable {
        StringBuilder version = new StringBuilder();
        byte b = get();
        while (true) {
            if (CR == b || LF == b) {
                consumeLF(b);
                pass(version.toString());
                return;
            }
            version.append((char)b);
            b = get();
        }
    }

    protected void processHeaders() throws Pausable {
        assert Field.HEADER_KEY == currentField;
        byte b = get();
        StringBuilder buf = new StringBuilder();
        while (':' != b) {
            if (CR == b || LF == b) {
                consumeLF(b);
                if (0 == buf.length()) {
                    pass(null);
                    return;
                }
            }
            buf.append((char)b);
            b = get();
        }
        pass(buf.toString());
        assert Field.HEADER_VALUE == currentField;
        buf.setLength(0);
        b = get();
        while (' ' == b) {
            b = get();
        }
        while (true) {
            if (CR == b || LF == b) {
                consumeLF(b);
                break;
            }
            buf.append((char)b);
            b = get();
        }
        pass(buf.toString());
    }

    private byte skipEmptySpaces() {
        byte b;
        while ((b = get()) == ' ') {
            // skip
        }
        return b;
    }

    private void consumeLF(byte b) {
        if (CR == b) {
            b = get();
            assert LF == b;
        }
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

    public String readVersion() {
        currentField = Field.VERSION;
        run();
        return fieldStringValue;
    }

    public String readHeaderKey() {
        currentField = Field.HEADER_KEY;
        run();
        return fieldStringValue;
    }

    public String readHeaderValue() {
        currentField = Field.HEADER_VALUE;
        run();
        return fieldStringValue;
    }
}
