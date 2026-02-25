package com.gihub.products.config;

import com.gihub.products.dto.AwsSecrets;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.jdbc.DataSourceBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.sql.DataSource;

@Configuration
@Profile("test")
public class ApplicationConfig {

    @Value("${cloud.aws.region:ap-south-1}")
    private String region;

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${app.secret.name:product-db-credentials}")
    private String secretName;

    private final Gson gson = new Gson();

    @Bean
    public DataSource dataSource() {
        AwsSecrets secrets = fetchSecret();
        if (secrets == null) {
            throw new IllegalStateException("Database secrets not available from AWS Secrets Manager");
        }

        String dbName = secrets.getDbName() == null ? "productdb" : secrets.getDbName();

        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:" + secrets.getEngine() + "://" + secrets.getHost() + ":" + secrets.getPort() + "/" + dbName)
                .username(secrets.getUsername())
                .password(secrets.getPassword())
                .build();
    }

    private AwsSecrets fetchSecret() {
        Region awsRegion = Region.of(region);
        SecretsManagerClient client;

        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
            client = SecretsManagerClient.builder()
                    .region(awsRegion)
                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                    .build();
        } else {
            client = SecretsManagerClient.builder()
                    .region(awsRegion)
                    .build();
        }

        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        try {
            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();
            if (secretString != null) {
                return gson.fromJson(secretString, AwsSecrets.class);
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to retrieve secret from AWS Secrets Manager", e);
        } finally {
            client.close();
        }
    }
}