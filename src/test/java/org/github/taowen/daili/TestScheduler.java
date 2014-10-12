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
                exit();
            }
        };
    }

    @Override
    protected int doSelect() throws IOException {
        return selector.selectNow();
    }

    @Override
    protected long currentTimeMillis() {
        if (null == fixedCurrentTimeMillis) {
            return super.currentTimeMillis();
        } else {
            return fixedCurrentTimeMillis;
        }
    }
}
