package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;

public class TestScheduler extends DefaultScheduler {

    public Long fixedCurrentTimeMillis;

    public TestScheduler(UsingFixture testCase) {
        new UsingFixture.Fixture(testCase) {
            @Override
            public void execute() throws Pausable, Exception {
                yield();
                close();
            }
        };
    }

    @Override
    protected int doSelect() throws IOException {
        return selector.selectNow();
    }

    @Override
    protected long getCurrentTimeMillis() {
        if (null == fixedCurrentTimeMillis) {
            return super.getCurrentTimeMillis();
        } else {
            return fixedCurrentTimeMillis;
        }
    }
}
