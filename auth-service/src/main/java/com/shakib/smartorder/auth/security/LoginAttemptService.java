package com.shakib.smartorder.auth.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
  Slows down password guessing: after {@code maxFailures} wrong passwords for the same email,
  further attempts are refused until the lock window ends.
 
  In-memory on purpose (no extra infrastructure), so it is per instance. With several
  auth-service instances, move this state to Redis.
 */
@Component
public class LoginAttemptService {

    private record Entry(int failures, Instant lockedUntil, Instant lastFailure) { }

    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginAttemptService(@Value("${app.login.max-failures:5}") int maxFailures,
            @Value("${app.login.lock-minutes:15}") long lockMinutes) 
    {
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public boolean isLocked(String key)
    {
        Entry entry = attempts.get(key);
        if (entry == null || entry.lockedUntil() == null) 
        {
            return false;
        }
        if (Instant.now().isAfter(entry.lockedUntil())) 
        {
            attempts.remove(key);
            return false;
        }
        return true;
    }

    public void recordFailure(String key)
    {
        Instant now = Instant.now();
        attempts.merge(key, new Entry(1, null, now), (old, ignored) -> 
        {
            int failures = old.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? now.plus(lockDuration) : old.lockedUntil();
            return new Entry(failures, lockedUntil, now);
        });
        evictStale(now);
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    // Keeps the map from growing forever with emails that guessed once and never came back.
    private void evictStale(Instant now) {
        if (attempts.size() < 10_000) {
            return;
        }
        attempts.entrySet().removeIf(e -> now.isAfter(e.getValue().lastFailure().plus(lockDuration)));
    }
}
