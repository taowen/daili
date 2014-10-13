package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderStreamTest extends TestCase {

    public void testOneByteBuffer() {
        HttpReader reader = createReader("GET");
        assertThat(reader.readMethod(), is("GET"));
    }

    public void testTwoByteBuffer() {
        HttpReader reader = new HttpReader();
        List<ByteBuffer> byteBuffers = Arrays.asList(
                ByteBuffer.wrap("G".getBytes()),
                ByteBuffer.wrap("ET".getBytes()));
        reader.byteBufferStream(byteBuffers.iterator());
        assertThat(reader.readMethod(), is("GET"));
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
