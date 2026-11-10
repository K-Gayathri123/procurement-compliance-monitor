package com.procurement.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;

@Service
public class LocalOriginalStorage {

    private static final Logger log =
        LoggerFactory.getLogger(LocalOriginalStorage.class);

    private final Path root;

    public LocalOriginalStorage(
        @Value("${app.upload-directory}") String directory
    ) {
        this.root = Path.of(directory)
            .toAbsolutePath().normalize();
    }

    public void store(String hash, byte[] bytes) {
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            throw new IllegalStateException(
                "Original storage requires a transaction"
            );
        }

        Path destination = root.resolve(hash + ".csv");
        boolean created = false;

        try {
            Files.createDirectories(root);

            if (Files.exists(destination)) {
                if (!Arrays.equals(
                        Files.readAllBytes(destination),
                        bytes)) {
                    throw new IllegalStateException(
                        "Original-file integrity conflict"
                    );
                }
                return;
            }

            // Register cleanup before the write, including partial writes.
            TransactionSynchronizationManager
                .registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                try {
                                    Files.deleteIfExists(destination);
                                } catch (IOException exception) {
                                    log.warn(
                                        "Rollback original-file cleanup failed"
                                    );
                                }
                            }
                        }
                    }
                );

            Files.write(
                destination,
                bytes,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );

            created = true;

        } catch (IOException exception) {
            throw new IllegalStateException(
                "Cannot store original upload", exception
            );
        }

        if (!created) {
            throw new IllegalStateException(
                "Original upload was not stored"
            );
        }
    }
}
