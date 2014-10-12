package org.github.taowen.daili;

import kilim.Task;

public interface Worker {

    void loop();

    void submit(Task task);
}
