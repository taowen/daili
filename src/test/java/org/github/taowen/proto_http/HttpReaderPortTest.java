package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderPortTest extends TestCase {

    public void test() {
        testPort("GET http://www.google.com:8080/", "8080");
        testPort("GET http://www.google.com:8080?", "8080");
        testPort("GET http://www.google.com:8080 ", "8080");
        testPort("GET http://www.google.com:8080\n", "8080");
    }

    private void testPort(String content, String port) {
        HttpReader reader = createReader(content);
        reader.readMethod();
        reader.readSchema();
        reader.readHost();
        assertThat(reader.readPort(), is(port));
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
