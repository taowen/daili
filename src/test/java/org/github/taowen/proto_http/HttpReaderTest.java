package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderTest extends TestCase {
    public void test() {
        HttpReader reader = createReader("GET");
        assertThat(reader.readMethod(), is("GET"));
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
