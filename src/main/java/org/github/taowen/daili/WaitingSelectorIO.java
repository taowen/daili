package org.github.taowen.daili;

import kilim.PauseReason;
import kilim.Task;

class WaitingSelectorIO implements PauseReason {
    long readBlockedAt;
    Task readTask;
    long writeBlockedAt;
    Task writeTask;
    long acceptBlockedAt;
    Task acceptTask;
    long connectBlockedAt;
    Task connectTask;
}
