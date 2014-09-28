package org.github.taowen.daili;

import kilim.PauseReason;

class WaitingOnIO implements PauseReason {
    long readBlockedAt;
    DailiTask readTask;
    long writeBlockedAt;
    DailiTask writeTask;
    long acceptBlockedAt;
    DailiTask acceptTask;
    long connectBlockedAt;
    DailiTask connectTask;
}
