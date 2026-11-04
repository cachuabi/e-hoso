package vn.ehoso.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import vn.ehoso.file.service.FileStoragePort;
import vn.ehoso.notification.service.MailGateway;

/** Replaces the clock, the S3 store and SMTP during tests. */
@TestConfiguration
public class TestBeans {
  /** A clock the test can move. */
  public static class MutableClock extends Clock {
    private volatile Instant now = Instant.parse("2026-11-05T03:00:00Z");

    public void set(String iso) {
      now = java.time.OffsetDateTime.parse(iso).toInstant();
    }

    public void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("Asia/Ho_Chi_Minh");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  /** Keeps files in memory. */
  public static class MemoryStorage implements FileStoragePort {
    public final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
      try {
        objects.put(key, content.readAllBytes());
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    @Override
    public void delete(String key) {
      objects.remove(key);
    }

    @Override
    public URI presignGet(String key, Duration ttl, String fileName) {
      return URI.create("https://storage.test/" + key + "?ttl=" + ttl.toSeconds());
    }
  }

  /** Records emails; can simulate an SMTP failure. */
  public static class FakeMail implements MailGateway {
    public final List<String> sentTo = new ArrayList<>();
    public final List<String> texts = new ArrayList<>();
    public volatile boolean failing;

    @Override
    public synchronized void send(String to, String subject, String text, String html) {
      if (failing) {
        throw new IllegalStateException("SMTP down");
      }
      sentTo.add(to);
      texts.add(text);
    }
  }

  @Bean
  @Primary
  MutableClock testClock() {
    return new MutableClock();
  }

  @Bean
  @Primary
  MemoryStorage memoryStorage() {
    return new MemoryStorage();
  }

  @Bean
  @Primary
  FakeMail fakeMail() {
    return new FakeMail();
  }
}
