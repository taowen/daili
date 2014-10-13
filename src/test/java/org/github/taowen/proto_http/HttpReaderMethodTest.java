package org.github.taowen.proto_http;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpReaderMethodTest extends TestCase {

    public void testNull() {
        HttpReader reader = createReader("\n");
        assertThat(reader.readMethod(), nullValue());
    }

    public void testMethods() {
        for (String method : new String[]{
                "GET", "POST", "CONNECT", "COPY", "CHECKOUT", "DELETE",
                "HEAD", "LOCK", "MKCOL", "MOVE", "MKACTIVITY",  "MERGE", "M-SEARCH",
                "POST", "PROPFIND", "PROPPATCH", "PUT", "PATCH", "PURGE",
                "REPORT", "SUBSCRIBE", "TRACE", "UNLOCK", "UNSUBSCRIBE"
        }) {
            HttpReader reader = createReader(method + " ");
            assertThat(reader.readMethod(), is(method));
        }
    }

    private HttpReader createReader(String content) {
        HttpReader reader = new HttpReader();
        reader.byteBufferStream(Arrays.asList(ByteBuffer.wrap(content.getBytes())).iterator());
        return reader;
    }
}
