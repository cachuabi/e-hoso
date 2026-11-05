package vn.ehoso.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vn.ehoso.file.service.FileTypeSniffer;

/** Test Plan case CK-F01: the type comes from the content. */
@Tag("FR-08")
class FileTypeSnifferTest {
  private final FileTypeSniffer sniffer = new FileTypeSniffer();

  @Test
  void recognisesAllowedTypes() throws Exception {
    assertThat(detect("%PDF-1.7".getBytes())).contains("application/pdf");
    assertThat(detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}))
        .contains("image/jpeg");
    assertThat(detect(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}))
        .contains("image/png");
  }

  @Test
  void refusesAnythingElse() throws Exception {
    assertThat(detect("MZ executable".getBytes())).isEmpty();
    assertThat(detect(new byte[0])).isEmpty();
  }

  private java.util.Optional<String> detect(byte[] data) throws Exception {
    return sniffer.detect(new ByteArrayInputStream(data));
  }
}
