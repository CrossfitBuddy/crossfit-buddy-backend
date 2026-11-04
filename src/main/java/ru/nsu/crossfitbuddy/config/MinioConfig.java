package ru.nsu.crossfitbuddy.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
            .endpoint(minioProperties.getUrl())
            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
            .build();

        try {
            boolean found = client.bucketExists(
                io.minio.BucketExistsArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .build()
            );
            if (!found) {
                client.makeBucket(
                    io.minio.MakeBucketArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO bucket: "
                + minioProperties.getBucketName(), e);
        }

        return client;
    }
}
