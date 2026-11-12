
package com.procurement.monitor.config;

import com.procurement.monitor.service.S3UploadMirrorJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "app.s3.enabled",
    havingValue = "true"
)
public class S3ArchiveConfiguration {

    @Bean(destroyMethod = "close")
    S3Client s3Client(
        @Value("${AWS_REGION:us-east-1}") String region
    ) {
        return S3Client.builder()
            .region(Region.of(region))
            .build();
    }

    @Bean
    S3UploadMirrorJob s3UploadMirrorJob(
        S3Client s3,
        @Value("${app.s3.bucket}") String bucket,
        @Value("${UPLOAD_DIRECTORY:./data/uploads}")
        String uploadDirectory
    ) {
        return new S3UploadMirrorJob(
            s3, bucket, uploadDirectory
        );
    }
}
