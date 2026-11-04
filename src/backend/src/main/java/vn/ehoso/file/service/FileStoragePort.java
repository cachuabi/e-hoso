package vn.ehoso.file.service;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/** Port to the S3 compatible store (D17 principle 1). */
public interface FileStoragePort {
  /** Stores content under a key. */
  void put(String key, InputStream content, long size, String contentType);

  /** Removes a key. */
  void delete(String key);

  /** Signs a download link for the browser. */
  URI presignGet(String key, Duration ttl, String fileName);
}
