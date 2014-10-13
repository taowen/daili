package org.github.taowen.proto_http;

import kilim.Pausable;
import kilim.Task;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class HttpReader extends Task {

    private Iterator<ByteBuffer> byteBufferStream;
    protected String fieldStringValue;
    private ByteBuffer currentByteBuffer;

    public void byteBufferStream(Iterator<ByteBuffer> byteBufferStream) {
        this.byteBufferStream = byteBufferStream;
        currentByteBuffer = byteBufferStream.next();
    }

    @Override
    public void execute() throws Pausable, Exception {
        byte b = get();
        if ('G' == b) {
            assert 'E' == get();
            assert 'T' == get();
            fieldStringValue = "GET";
            pass();
        } else {
            throw new UnsupportedOperationException("not implemented");
        }
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
        run();
        return fieldStringValue;
    }
}
