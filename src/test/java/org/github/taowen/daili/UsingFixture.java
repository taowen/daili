package org.github.taowen.daili;

import junit.framework.TestCase;
import kilim.Task;

import java.util.LinkedList;
import java.util.Queue;

public abstract class UsingFixture extends TestCase {
    private Queue<Fixture> fixtures = new LinkedList<Fixture>();

    @Override
    protected void tearDown() throws Exception {
        Fixture fixture;
        while ((fixture = fixtures.poll()) != null) {
            fixture.run();
        }
    }

    public static abstract class Fixture extends Task {
        public Fixture(UsingFixture testCase) {
            testCase.fixtures.offer(this);
            run();
        }
    }
}
