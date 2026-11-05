package vn.ehoso.file.infra;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import vn.ehoso.file.service.FileStoragePort;

/** AWS SDK v2 adapter: internal endpoint for the backend, public one for signed links. */
@Component
class S3FileStorage implements FileStoragePort {
  private final S3Client client;
  private final S3Presigner presigner;
  private final String bucket;

  S3FileStorage(
      @Value("${ehoso.storage.endpoint-internal}") String internal,
      @Value("${ehoso.storage.endpoint-public}") String external,
      @Value("${ehoso.storage.region}") String region,
      @Value("${ehoso.storage.bucket}") String bucket,
      @Value("${ehoso.storage.access-key}") String accessKey,
      @Value("${ehoso.storage.secret-key}") String secretKey) {
    this.bucket = bucket;
    StaticCredentialsProvider credentials =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    this.client =
        S3Client.builder()
            .endpointOverride(URI.create(internal))
            .region(Region.of(region))
            .credentialsProvider(credentials)
            .forcePathStyle(true)
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build();
    this.presigner =
        S3Presigner.builder()
            .endpointOverride(URI.create(external))
            .region(Region.of(region))
            .credentialsProvider(credentials)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
  }

  @Override
  public void put(String key, InputStream content, long size, String contentType) {
    client.putObject(
        r -> r.bucket(bucket).key(key).contentType(contentType),
        RequestBody.fromInputStream(content, size));
  }

  @Override
  public void delete(String key) {
    client.deleteObject(r -> r.bucket(bucket).key(key));
  }

  @Override
  public URI presignGet(String key, Duration ttl, String fileName) {
    return URI.create(
        presigner
            .presignGetObject(
                p -> p.signatureDuration(ttl).getObjectRequest(g -> g.bucket(bucket).key(key)))
            .url()
            .toString());
  }
}
