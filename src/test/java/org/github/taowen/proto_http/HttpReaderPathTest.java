package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderPathTest extends TestCase {

    public void test() {
        testPath("GET http://www.google.com ", "");
        testPath("GET http://www.google.com\n", "");
        testPath("GET http://www.google.com?", "");
        testPath("GET http://www.google.com:8080?", "");
        testPath("GET http://www.google.com:8080 ", "");
        testPath("GET http://www.google.com:8080\n", "");
        testPath("GET http://www.google.com/abc ", "abc");
        testPath("GET http://www.google.com/abc?", "abc");
        testPath("GET http://www.google.com/abc\n", "abc");
        testPath("GET http://www.google.com:8080/abc ", "abc");
        testPath("GET http://www.google.com:8080/abc?", "abc");
        testPath("GET http://www.google.com:8080/abc\n", "abc");
    }

    private void testPath(String content, String path) {
        HttpReader reader = createReader(content);
        reader.readMethod();
        reader.readSchema();
        reader.readHost();
        reader.readPort();
        assertThat(reader.readPath(), is(path));
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
