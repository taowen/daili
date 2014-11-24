package org.github.taowen.proto_http;

import kilim.Pausable;
import kilim.Task;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class HttpReader extends Task {

    private Iterator<ByteBuffer> byteBufferStream;
    private ByteBuffer currentByteBuffer;
    protected Queue<Field> processingFields;
    protected Queue<Object> output;

    private static enum Field {
        METHOD, VERSION, HEADER_KEY, HEADER_VALUE, URL
    }
    private static final byte CR = 0x0d;
    private static final byte LF = 0x0a;
    private static final String DEFAULT_VERSION = "HTTP/0.9";

    public HttpReader() {
        output = new LinkedList<Object>();
        processingFields = new LinkedList<Field>();
    }

    public void byteBufferStream(Iterator<ByteBuffer> byteBufferStream) {
        this.byteBufferStream = byteBufferStream;
        currentByteBuffer = byteBufferStream.next();
    }

    protected Field nextField() throws Pausable {
        Field nextField = processingFields.poll();
        if (null != nextField) {
            return nextField;
        }
        yield();
        return processingFields.remove();
    }

    protected void assertNextField(Field expected) throws Pausable {
        Field nextField = nextField();
        assert expected == nextField;
    }

    @Override
    public void execute() throws Pausable, Exception {
        processRequestLine();
        processHeaders();
    }

    protected void processRequestLine() throws Pausable {
        assertNextField(Field.METHOD);
        if (!processMethod()) {
            return;
        }
        assertNextField(Field.URL);
        if (!processUrl()) {
            return;
        }
        assertNextField(Field.VERSION);
        processVersion();
    }

    protected boolean processMethod() throws Pausable {
        byte b = get();
        if (CR == b || LF == b) {
            consumeLF(b);
            output.offer(null);
            return false;
        }
        StringBuilder method = new StringBuilder();
        while (b != ' ') {
            method.append((char)b);
            b = get();
        }
        output.offer(method.toString());
        return true;
    }

    protected boolean processUrl() throws Pausable {
        byte b = skipEmptySpaces();
        StringBuilder url = new StringBuilder();
        while (b != ' ') {
            if (CR == b || LF == b) {
                consumeLF(b);
                output.offer(url.toString());
                assertNextField(Field.VERSION);
                output.offer(DEFAULT_VERSION);
                return false;
            }
            url.append((char)b);
            b = get();
        }
        output.offer(url.toString());
        return true;
    }

    protected void processVersion() throws Pausable {
        StringBuilder version = new StringBuilder();
        byte b = get();
        while (true) {
            if (CR == b || LF == b) {
                consumeLF(b);
                output.offer(version.toString());
                return;
            }
            version.append((char)b);
            b = get();
        }
    }

    protected void processHeaders() throws Pausable {
        assertNextField(Field.HEADER_KEY);
        byte b = get();
        StringBuilder buf = new StringBuilder();
        while (':' != b) {
            if (CR == b || LF == b) {
                consumeLF(b);
                if (0 == buf.length()) {
                    output.offer(null);
                    return;
                }
            }
            buf.append((char)b);
            b = get();
        }
        output.offer(buf.toString());
        assertNextField(Field.HEADER_VALUE);
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
        output.offer(buf.toString());
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

    public String readMethod() {
        processingFields.offer(Field.METHOD);
        run();
        return (String)output.remove();
    }

    public String readUrl() {
        processingFields.offer(Field.URL);
        run();
        return (String)output.remove();
    }

    public String readVersion() {
        processingFields.offer(Field.VERSION);
        run();
        return (String)output.remove();
    }

    public String readHeaderKey() {
        processingFields.offer(Field.HEADER_KEY);
        run();
        return (String)output.remove();
    }

    public String readHeaderValue() {
        processingFields.offer(Field.HEADER_VALUE);
        run();
        return (String)output.remove();
    }
}
