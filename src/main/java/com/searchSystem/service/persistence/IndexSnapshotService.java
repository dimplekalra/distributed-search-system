package com.searchSystem.service.persistence;

import com.searchSystem.index.Shard;
import com.searchSystem.service.ShardRouter;
import com.searchSystem.model.SnapshotData;
import com.searchSystem.service.wal.WriteAheadLogService;
import com.searchSystem.model.Document;
import com.searchSystem.repository.DocumentRepository;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

@Service
public class IndexSnapshotService {
    private static final String SNAPSHOT_DIR = "snapshots/";

    private final ShardRouter shardRouter;
    private final WriteAheadLogService walService;
    private final DocumentRepository documentRepository;

    public IndexSnapshotService(ShardRouter shardRouter, WriteAheadLogService walService,
            DocumentRepository documentRepository) {
        this.shardRouter = shardRouter;
        this.walService = walService;
        this.documentRepository = documentRepository;
    }

    @PostConstruct
    public void loadSnapshotsOnStartup() {

        System.out.println("Loading shard snapshots...");

        List<Shard> shards = shardRouter.getAllShards();

        for (int i = 0; i < shards.size(); i++) {
            loadSnapshot(i, shards.get(i));
        }

        System.out.println("Replaying WAL...");

        List<Document> walDocuments = walService.replay();

        for (Document document : walDocuments) {

            this.documentRepository.save(document);

            shardRouter.routeDocument(document);
        }

        System.out.println(
                "Recovered "
                        + walDocuments.size()
                        + " documents from WAL");
    }

    public boolean saveAllSnapshots() {
        List<Shard> shards = shardRouter.getAllShards();

        for (int i = 0; i < shards.size(); i++) {
            boolean success = saveSnapshot(i, shards.get(i));
            if (!success) {
                return false;
            }
        }
        return true;
    }

    private boolean saveSnapshot(int shardId, Shard shard) {
        try {

            File directory = new File(SNAPSHOT_DIR);

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = SNAPSHOT_DIR + "shard_" + shardId + ".dat";

            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(fileName));

            outputStream.writeObject(shard.exportSnapshot());

            outputStream.close();

            System.out.println(
                    "Saved snapshot for shard: " + shardId);

            return true;

        } catch (Exception e) {
            System.err.println("Failed saving snapshot for shard " + shardId);
            e.printStackTrace();

            return false;
        }
    }

    private void loadSnapshot(int shardId, Shard shard) {
        try {

            String fileName = SNAPSHOT_DIR + "shard_" + shardId + ".dat";

            File snapshotFile = new File(fileName);

            if (!snapshotFile.exists()) {
                System.out.println(
                        "No snapshot found for shard: " + shardId);
                return;
            }

            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(snapshotFile));

            SnapshotData snapshotData = (SnapshotData) inputStream.readObject();

            inputStream.close();

            shard.loadSnapshot(snapshotData);
            System.out.println(
                    "Loaded snapshot for shard: " + shardId);

        } catch (Exception e) {

            System.out.println(
                    "Failed loading snapshot for shard: " + shardId);

            e.printStackTrace();
        }
    }
}
