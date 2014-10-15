package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderHostTest extends TestCase {

    public void test() {
        testHost("GET http://www.google.com:8080", "www.google.com");
        testHost("GET http://www.google.com/", "www.google.com");
        testHost("GET http://www.google.com?v=1", "www.google.com");
    }

    private void testHost(String content, String host) {
        HttpReader reader = createReader(content);
        reader.readMethod();
        reader.readSchema();
        assertThat(reader.readHost(), is(host));
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
