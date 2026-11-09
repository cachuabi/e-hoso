package vn.ehoso.dossier.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Blocks an address for 10 minutes after more than 10 failures in 10 minutes (UC-07). */
@Component
public class LookupThrottle {
  private static final Duration WINDOW = Duration.ofMinutes(10);
  private static final int LIMIT = 10;

  private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
  private final Map<String, Instant> blocked = new ConcurrentHashMap<>();
  private final Clock clock;

  /** Creates the throttle. */
  public LookupThrottle(Clock clock) {
    this.clock = clock;
  }

  /** Tells whether the address is blocked now. */
  public boolean isBlocked(String address) {
    Instant until = blocked.get(address);
    if (until == null) {
      return false;
    }
    if (clock.instant().isAfter(until)) {
      blocked.remove(address);
      failures.remove(address);
      return false;
    }
    return true;
  }

  /** Counts one failed lookup. */
  public void recordFailure(String address) {
    Instant now = clock.instant();
    Deque<Instant> recent = failures.computeIfAbsent(address, a -> new ArrayDeque<>());
    synchronized (recent) {
      recent.addLast(now);
      while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(WINDOW))) {
        recent.removeFirst();
      }
      if (recent.size() > LIMIT) {
        blocked.put(address, now.plus(WINDOW));
      }
    }
  }
}
