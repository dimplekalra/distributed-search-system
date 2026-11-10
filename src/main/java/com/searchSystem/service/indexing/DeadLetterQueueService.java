package com.searchSystem.service.indexing;

import com.searchSystem.model.Document;

import org.springframework.javapoet.ClassName;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DeadLetterQueueService {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueService.class);
    private final BlockingQueue<Document> deadLetterQueue = new LinkedBlockingQueue<>();

    public void sendToDLQ(Document document, Exception exception) {
        deadLetterQueue.offer(document);

        logger.info(
                "DOCUMENT SENT TO DLQ: "
                        + document.getId());

        logger.info(
                "Reason: "
                        + exception.getMessage());
    }

    public int size() {
        return deadLetterQueue.size();
    }
}
