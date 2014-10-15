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
        METHOD, HOST, PORT, SCHEMA
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
        byte b = skipEmptySpaces();
        if ("CONNECT".equals(method)) {
            throw new UnsupportedOperationException("not implemented yet");
        } else {
            assert Field.SCHEMA == currentField;
            StringBuilder buf = new StringBuilder();
            while (':' != b) {
                buf.append((char)b);
                b = get();
            }
            fieldStringValue = buf.toString();
            pass();
            assert Field.HOST == currentField;
            buf.setLength(0);
            b = get();
            assert '/' == b;
            b = get();
            assert '/' == b;
            b = get();
            String port = null;
            while (':' != b) {
                if ('/' == b) {
                    port = "";
                    break;
                }
                if ('?' == b) {
                    port = "";
                    break;
                }
                if (' ' == b) {
                    port = "";
                    break;
                }
                if (CR == b || LF == b) {
                    port = "";
                    break;
                }
                buf.append((char)b);
                b = get();
            }
            fieldStringValue = buf.toString();
            pass();
            assert Field.PORT == currentField;
            buf.setLength(0);
            if (null == port) {
                b = get();
                while ('/' != b) {
                    if ('?' == b) {
                        break;
                    }
                    if (' ' == b) {
                        break;
                    }
                    if (CR == b || LF == b) {
                        break;
                    }
                    buf.append((char)b);
                    b = get();
                }
                port = buf.toString();
            }
            fieldStringValue = port;
            pass();
        }
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

    private byte get() {
        if (!currentByteBuffer.hasRemaining()) {
            currentByteBuffer = byteBufferStream.next();
        }
        return currentByteBuffer.get();
    }

    protected void pass() throws Pausable {
        yield();
    }

    public String readMethod() {
        currentField = Field.METHOD;
        run();
        return fieldStringValue;
    }

    public String readSchema() {
        currentField = Field.SCHEMA;
        run();
        return fieldStringValue;
    }

    public String readHost() {
        currentField = Field.HOST;
        run();
        return fieldStringValue;
    }

    public String readPort() {
        currentField = Field.PORT;
        run();
        return fieldStringValue;
    }
}
