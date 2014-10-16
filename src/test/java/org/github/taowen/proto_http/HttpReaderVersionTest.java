package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderVersionTest extends TestCase {

    public void test() {
        testVersion("GET http://www.google.com\n", "HTTP/0.9");
        testVersion("GET http://www.google.com HTTP/1.0\n", "HTTP/1.0");
    }

    private void testVersion(String content, String version) {
        HttpReader reader = createReader(content);
        reader.readMethod();
        reader.readUrl();
        assertThat(reader.readVersion(), is(version));
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
