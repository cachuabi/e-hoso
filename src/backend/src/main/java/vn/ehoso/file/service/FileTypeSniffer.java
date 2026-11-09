package vn.ehoso.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Detects the type from the leading bytes, never from the file name (FR-08). */
@Component
public class FileTypeSniffer {
  private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

  /** Returns the MIME type, or empty when the content is not allowed. */
  public Optional<String> detect(InputStream content) throws IOException {
    byte[] head = content.readNBytes(8);
    if (startsWith(head, PDF)) {
      return Optional.of("application/pdf");
    }
    if (startsWith(head, JPEG)) {
      return Optional.of("image/jpeg");
    }
    if (startsWith(head, PNG)) {
      return Optional.of("image/png");
    }
    return Optional.empty();
  }

  private boolean startsWith(byte[] head, byte[] signature) {
    return head.length >= signature.length
        && Arrays.equals(Arrays.copyOf(head, signature.length), signature);
  }
}
