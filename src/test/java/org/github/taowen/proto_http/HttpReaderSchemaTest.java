package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderSchemaTest extends TestCase {

    public void testHttp() {
        testSchema("GET http:");
        testSchema("GET   http:");
    }

    private void testSchema(String content) {
        HttpReader reader = createReader(content);
        reader.readMethod();
        assertThat(reader.readSchema(), is("http"));
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
