package org.github.taowen.proto_http;

import kilim.Pausable;
import kilim.Task;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class HttpReader extends Task {

    private Iterator<ByteBuffer> byteBufferStream;
    protected String fieldStringValue;
    private ByteBuffer currentByteBuffer;
    private static final byte CR = 0x0d;
    private static final byte LF = 0x0a;

    public void byteBufferStream(Iterator<ByteBuffer> byteBufferStream) {
        this.byteBufferStream = byteBufferStream;
        currentByteBuffer = byteBufferStream.next();
    }

    @Override
    public void execute() throws Pausable, Exception {
        if (!processMethod()) {
            return; // empty request
        }
    }

    private boolean processMethod() throws Pausable {
        byte b = get();
        if (CR == b || LF == b) {
            return false;
        }
        switch (b) {
            case 'C':
                b = get();
                if ('O' == b) {
                    b = get();
                    if ('N' == b) {
                        assertFollowingBytes('N', 'E', 'C', 'T');
                        fieldStringValue = "CONNECT";
                        pass();
                        return true;
                    } else {
                        assert 'P' == b;
                        assertFollowingBytes('Y');
                        fieldStringValue = "COPY";
                        pass();
                        return true;
                    }
                } else {
                    assert 'H' == b;
                    assertFollowingBytes('E', 'C', 'K', 'O', 'U', 'T');
                    fieldStringValue = "CHECKOUT";
                    pass();
                    return true;
                }
            case 'D':
                assertFollowingBytes('E', 'L', 'E', 'T', 'E');
                fieldStringValue = "DELETE";
                pass();
                return true;
            case 'G':
                assertFollowingBytes('E', 'T');
                fieldStringValue = "GET";
                pass();
                return true;
            case 'H':
                assertFollowingBytes('E', 'A', 'D');
                fieldStringValue = "HEAD";
                pass();
                return true;
            case 'L':
                assertFollowingBytes('O', 'C', 'K');
                fieldStringValue = "LOCK";
                pass();
                return true;
            case 'M':
                b = get();
                switch (b) {
                    case 'K':
                        b = get();
                        if (b == 'C') {
                            assertFollowingBytes('O', 'L');
                            fieldStringValue = "MKCOL";
                            pass();
                            return true;
                        } else {
                            assert 'A' == b;
                            assertFollowingBytes('C', 'T', 'I', 'V', 'I', 'T', 'Y');
                            fieldStringValue = "MKACTIVITY";
                            pass();
                            return true;
                        }
                    case 'O':
                        assertFollowingBytes('V', 'E');
                        fieldStringValue = "MOVE";
                        pass();
                        return true;
                    case 'E':
                        assertFollowingBytes('R', 'G', 'E');
                        fieldStringValue = "MERGE";
                        pass();
                        return true;
                    case '-':
                        assertFollowingBytes('S', 'E', 'A', 'R', 'C', 'H');
                        fieldStringValue = "M-SEARCH";
                        pass();
                        return true;
                    default:
                        throw new RuntimeException("unknown http method");
                }
            case 'N':
                assertFollowingBytes('O', 'T', 'I', 'F', 'Y');
                fieldStringValue = "NOTIFY";
                pass();
                return true;
            case 'O':
                assertFollowingBytes('P', 'T', 'I', 'O', 'N', 'S');
                fieldStringValue = "OPTIONS";
                pass();
                return true;
            case 'P':
                b = get();
                switch (b) {
                    case 'R':
                        b = get();
                        assert 'O' == b;
                        b = get();
                        assert 'P' == b;
                        b = get();
                        if (b == 'F') {
                            assertFollowingBytes('I', 'N', 'D');
                            fieldStringValue = "PROPFIND";
                            pass();
                            return true;
                        } else {
                            assert 'P' == b;
                            assertFollowingBytes('A', 'T', 'C', 'H');
                            fieldStringValue = "PROPPATCH";
                            pass();
                            return true;
                        }
                    case 'U':
                        b = get();
                        if ('T' == b) {
                            fieldStringValue = "PUT";
                            pass();
                            return true;
                        } else {
                            assert 'R' == b;
                            assertFollowingBytes('G', 'E');
                            fieldStringValue = "PURGE";
                            pass();
                            return true;
                        }
                    case 'O':
                        assertFollowingBytes('S', 'T');
                        fieldStringValue = "POST";
                        pass();
                        return true;
                    case 'A':
                        assertFollowingBytes('T', 'C', 'H');
                        fieldStringValue = "PATCH";
                        pass();
                        return true;
                    default:
                        throw new RuntimeException("unknown http method");
                }
            case 'R':
                assertFollowingBytes('E', 'P', 'O', 'R', 'T');
                fieldStringValue = "REPORT";
                pass();
                return true;
            case 'S':
                assertFollowingBytes('U', 'B', 'S', 'C', 'R', 'I', 'B', 'E');
                fieldStringValue = "SUBSCRIBE";
                pass();
                return true;
            case 'T':
                assertFollowingBytes('R', 'A', 'C', 'E');
                fieldStringValue = "TRACE";
                pass();
                return true;
            case 'U':
                b = get();
                assert 'N' == b;
                b = get();
                if ('L' == b) {
                    assertFollowingBytes('O', 'C', 'K');
                    fieldStringValue = "UNLOCK";
                    pass();
                    return true;
                } else {
                    assert 'S' == b;
                    assertFollowingBytes('U', 'B', 'S', 'C', 'R', 'I', 'B', 'E');
                    fieldStringValue = "UNSUBSCRIBE";
                    pass();
                    return true;
                }
            default:
                throw new RuntimeException("unknown http method");
        }
    }

    private void assertFollowingBytes(char... bytes) {
        for (char expected : bytes) {
            byte b = get();
            assert expected == b;
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
