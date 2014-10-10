package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

public class TestSocket {
    public static ServerSocketChannel openServerSocketChannel(UsingFixture testCase) throws IOException {
        final ServerSocketChannel channel = ServerSocketChannel.open();
        new UsingFixture.Fixture(testCase) {
            @Override
            public void execute() throws Pausable, Exception {
                yield();
                channel.close();
            }
        };
        return channel;
    }
}
