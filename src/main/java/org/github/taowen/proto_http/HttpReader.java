package org.github.taowen.proto_http;

import kilim.Pausable;
import kilim.Task;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class HttpReader extends Task {

    private Iterator<ByteBuffer> byteBufferStream;
    protected String fieldStringValue;

    public void byteBufferStream(Iterator<ByteBuffer> byteBufferStream) {
        this.byteBufferStream = byteBufferStream;
    }

    @Override
    public void execute() throws Pausable, Exception {
        ByteBuffer byteBuffer = byteBufferStream.next();
        byte b = byteBuffer.get();
        if ('G' == b) {
            assert 'E' == byteBuffer.get();
            assert 'T' == byteBuffer.get();
            fieldStringValue = "GET";
            pass();
        } else {
            throw new UnsupportedOperationException("not implemented");
        }
    }

    protected void pass() throws Pausable {
        yield();
    }

    public String readMethod() {
        run();
        return fieldStringValue;
    }
}
