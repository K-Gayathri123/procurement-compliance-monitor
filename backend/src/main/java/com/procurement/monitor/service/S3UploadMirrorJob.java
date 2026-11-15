
package com.procurement.monitor.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3UploadMirrorJob {

    private static final Logger log =
            LoggerFactory.getLogger(S3UploadMirrorJob.class);

    private final S3Client s3;
    private final String bucket;
    private final Path uploadDirectory;

    public S3UploadMirrorJob(
            S3Client s3,
            String bucket,
            String uploadDirectory) {

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException(
                    "APP_S3_BUCKET is required");
        }

        this.s3 = s3;
        this.bucket = bucket;
        this.uploadDirectory = Path.of(uploadDirectory)
                .toAbsolutePath().normalize();
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 30000)
    public void mirrorUploads() {

        if (!Files.isDirectory(uploadDirectory)) {
            return;
        }

        try (Stream<Path> files = Files.walk(uploadDirectory)) {
            files.filter(p -> Files.isRegularFile(
                    p, LinkOption.NOFOLLOW_LINKS))
                 .forEach(this::archiveSafely);

        } catch (IOException e) {
            log.error("Cannot scan upload directory", e);
        }
    }

    private void archiveSafely(Path file) {

        try {
            Instant modified =
                    Files.getLastModifiedTime(file).toInstant();

            if (modified.isAfter(
                    Instant.now().minusSeconds(120))) {
                return;
            }

            String relative = uploadDirectory.relativize(file)
                    .toString().replace('\\', '/');

            String key = "raw/uploads/" + relative;
            String checksum = sha256(file);

            try {
                var existing = s3.headObject(
                        HeadObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build()
                );

                if (checksum.equals(
                        existing.metadata().get("sha256"))) {
                    return;
                }

            } catch (S3Exception e) {
                if (e.statusCode() != 404) {
                    throw e;
                }
            }

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .metadata(Map.of("sha256", checksum))
                            .build(),
                    RequestBody.fromFile(file)
            );

            log.info("Archived upload to S3: {}", key);

        } catch (Exception e) {
            log.error("S3 archive failed for {}", file, e);
        }
    }

    private String sha256(Path file)
            throws IOException, NoSuchAlgorithmException {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;

            while ((count = in.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }
}
