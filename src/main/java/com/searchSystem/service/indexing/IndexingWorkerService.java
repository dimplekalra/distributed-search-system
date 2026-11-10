package com.searchSystem.service.indexing;

import com.searchSystem.model.Document;
import com.searchSystem.model.RetryableDocument;
import com.searchSystem.model.DelayedRetryTask;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class IndexingWorkerService {

    private static final int QUEUE_CAPACITY = 10_000;
    private static final int MAX_RETRIES = 3;

    private final BlockingQueue<RetryableDocument> indexingQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private final BatchIndexProcessor batchIndexProcessor;

    private final DeadLetterQueueService dlqService;

    private final DelayQueue<DelayedRetryTask> retryQueue = new DelayQueue<>();

    /*
     * Thread pool for multiple workers
     */
    private final ExecutorService workerPool = Executors.newFixedThreadPool(4);

    private static final int BATCH_SIZE = 100;

    private static final int WORKER_COUNT = 4;

    public IndexingWorkerService(BatchIndexProcessor batchIndexProcessor, DeadLetterQueueService dlqService) {
        this.batchIndexProcessor = batchIndexProcessor;
        this.dlqService = dlqService;
    }

    public void submit(Document document) {

        RetryableDocument retryableDocument = new RetryableDocument(document, 0);

        boolean added = indexingQueue.offer(retryableDocument);

        if (!added) {
            System.out.println(
                    "Indexing queue FULL. Rejecting document: "
                            + document.getId());

            /*
             * In production:
             * - send to retry topic
             * - send to DLQ
             * - throttle Kafka
             * - alert monitoring
             */
            return;
        }

        int currentSize = indexingQueue.size();

        if (currentSize > QUEUE_CAPACITY * 0.8) {
            System.out.println(
                    "WARNING: Index queue is "
                            + currentSize
                            + "/"
                            + QUEUE_CAPACITY
                            + " full");
        }

        System.out.println("Document submitted for indexing: " + document.getId());
    }

    @PostConstruct
    public void startWorker() {

        for (int i = 0; i < WORKER_COUNT; i++) {
            int workerId = i + 1;

            workerPool.submit(() -> runWorker(workerId));
        }

        Thread retryScheduler = new Thread(this::processRetries);

        retryScheduler.setDaemon(true);

        retryScheduler.start();

        System.out.println(
                WORKER_COUNT +
                        " indexing workers started");
    }

    private void processRetries() {
        while (true) {
            try {
                DelayedRetryTask delayedTask = retryQueue.take();

                RetryableDocument retryableDocument = delayedTask.getRetryableDocument();

                indexingQueue.offer(retryableDocument);

                System.out.println(
                        "Retry re-submitted for document: "
                                + retryableDocument
                                        .getDocument()
                                        .getId());

            } catch (Exception e) {

                System.out.println(
                        "Retry scheduler failed");

                e.printStackTrace();
            }
        }
    }

    private void runWorker(int workerId) {
        while (true) {
            try {
                List<RetryableDocument> retryBatch = new ArrayList<>();

                List<Document> batch = new ArrayList<>();

                RetryableDocument firstDocument = indexingQueue.take();

                retryBatch.add(firstDocument);

                batch.add(firstDocument.getDocument());

                List<RetryableDocument> drained = new ArrayList<>();

                indexingQueue.drainTo(drained, BATCH_SIZE - 1);

                for (RetryableDocument retryable : drained) {
                    retryBatch.add(retryable);

                    batch.add(retryable.getDocument());
                }

                System.out.println(
                        "Worker-" + workerId +
                                " processing batch of size: " + batch.size());

                try {
                    batchIndexProcessor.processBatch(batch);
                } catch (Exception e) {
                    System.out.println("Batch failed. Retrying...");

                    for (RetryableDocument retryable : retryBatch) {
                        int retryCount = retryable.getRetryCount();

                        if (retryCount >= MAX_RETRIES) {
                            dlqService.sendToDLQ(retryable.getDocument(), e);
                            continue;
                        }

                        retryable.setRetryCount(retryCount + 1);

                        indexingQueue.offer(retryable);

                        long delayMillis = calculateRetryDelay(retryable.getRetryCount());

                        retryQueue.offer(new DelayedRetryTask(retryable, delayMillis));

                        System.out.println(
                                "Retrying document: "
                                        + retryable.getDocument().getId()
                                        + " retryCount="
                                        + retryable.getRetryCount());
                    }

                    e.printStackTrace();
                }

            } catch (Exception e) {
                System.out.println(
                        "Worker-" + workerId +
                                " failed");

                e.printStackTrace();
            }
        }
    }

    private long calculateRetryDelay(int retryCount) {
        /*
         * Exponential backoff:
         * 
         * Retry 1 -> 1 sec
         * Retry 2 -> 2 sec
         * Retry 3 -> 4 sec
         */

        return (long) Math.pow(2, retryCount) * 1000;
    }

    @PreDestroy
    public void shutdown() {
        workerPool.shutdownNow();

        try {

            if (!workerPool.awaitTermination(
                    10,
                    TimeUnit.SECONDS)) {

                workerPool.shutdownNow();
            }

        } catch (InterruptedException e) {

            workerPool.shutdownNow();
        }

        System.out.println(
                "Indexing worker pool shutdown complete");
    }

}
