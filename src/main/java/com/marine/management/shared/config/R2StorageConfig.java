package com.marine.management.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Cloudflare R2 configuration.
 *
 * R2 is S3-compatible, so we use the AWS SDK with a custom endpoint.
 * Endpoint format: https://{accountId}.r2.cloudflarestorage.com
 *
 * Beans are only created when app.r2.enabled=true.
 * In local dev (app.r2.enabled=false) these beans are skipped entirely
 * and FileStorageService runs in no-op mode.
 *
 * Required env vars (when enabled):
 *   R2_ACCOUNT_ID   — Cloudflare account ID
 *   R2_ACCESS_KEY   — R2 API token (Access Key ID)
 *   R2_SECRET_KEY   — R2 API token (Secret Access Key)
 *   R2_BUCKET       — Bucket name (default: marine-attachments)
 */
@Configuration
@ConditionalOnProperty(name = "app.r2.enabled", havingValue = "true")
public class R2StorageConfig {

    @Value("${app.r2.account-id}")
    private String accountId;

    @Value("${app.r2.access-key}")
    private String accessKey;

    @Value("${app.r2.secret-key}")
    private String secretKey;

    @Bean
    public S3Client r2Client() {
        return S3Client.builder()
                .endpointOverride(r2Endpoint())
                .region(Region.of("auto"))
                .credentialsProvider(credentials())
                .forcePathStyle(true) // Required for R2
                .build();
    }

    @Bean
    public S3Presigner r2Presigner() {
        return S3Presigner.builder()
                .endpointOverride(r2Endpoint())
                .region(Region.of("auto"))
                .credentialsProvider(credentials())
                .build();
    }

    private URI r2Endpoint() {
        return URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
    }

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
    }
}
