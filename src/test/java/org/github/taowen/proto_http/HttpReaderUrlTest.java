package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderUrlTest extends TestCase {

    public void test() {
        testUrl("GET http://www.google.com ", "http://www.google.com");
        testUrl("GET http://www.google.com\n", "http://www.google.com");
        testUrl("GET http://www.google.com\r\n", "http://www.google.com");
    }

    private void testUrl(String content, String url) {
        HttpReader reader = createReader(content);
        reader.readMethod();
        assertThat(reader.readUrl(), is(url));
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
