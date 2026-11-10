package com.searchSystem.service.wal;

import com.searchSystem.model.Document;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class WriteAheadLogService {
    private static final String WAL_FILE = "wal.log";

    public synchronized void append(Document document) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WAL_FILE, true))) {
            String logEntry = document.getId() + "|" + document.getContent();

            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Failed writing WAL");

            e.printStackTrace();
        }
    }

    public List<Document> replay() {
        List<Document> documents = new ArrayList<>();

        try {
            if (!Files.exists(Paths.get(WAL_FILE))) {
                return documents;
            }

            BufferedReader reader = new BufferedReader(new FileReader(WAL_FILE));

            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 2);

                if (parts.length < 2) {
                    continue;
                }

                String id = parts[0];
                String content = parts[1];

                documents.add(
                        new Document(id, content));
            }

            reader.close();
        } catch (Exception e) {

            System.out.println("Failed replaying WAL");

            e.printStackTrace();
        }

        return documents;
    }

    public void clear() {
        try {

            PrintWriter writer = new PrintWriter(WAL_FILE);

            writer.close();

            System.out.println("WAL cleared");

        } catch (Exception e) {

            System.out.println("Failed clearing WAL");

            e.printStackTrace();
        }
    }

}
