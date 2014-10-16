package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderHeaderTest extends TestCase {

    public void testNormal() {
        HttpReader reader = createReader(
                "GET /",
                "Host: www.google.com");
        reader.readMethod();
        reader.readUrl();
        reader.readVersion();
        assertThat(reader.readHeaderKey(), is("Host"));
    }
    public void testEnd() {
        HttpReader reader = createReader(
                "GET /",
                "");
        reader.readMethod();
        reader.readUrl();
        reader.readVersion();
        assertThat(reader.readHeaderKey(), nullValue());
    }

    private HttpReader createReader(String... content) {
        ArrayList<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>();
        for (String c : content) {
            byteBuffers.add(ByteBuffer.wrap((c + "\n").getBytes()));
        }
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(byteBuffers.iterator());
        return reader;
    }
}
