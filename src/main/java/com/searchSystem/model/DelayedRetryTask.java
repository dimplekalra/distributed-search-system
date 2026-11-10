package com.searchSystem.model;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedRetryTask implements Delayed {
    private final RetryableDocument retryableDocument;
    private final long retryTimeMillis;

    public DelayedRetryTask(RetryableDocument retryableDocument, long delayMillis) {
        this.retryableDocument = retryableDocument;
        this.retryTimeMillis = System.currentTimeMillis() + delayMillis;
    }

    public RetryableDocument getRetryableDocument() {
        return retryableDocument;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = retryTimeMillis - System.currentTimeMillis();

        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.retryTimeMillis, ((DelayedRetryTask) other).retryTimeMillis);
    }
}
