package com.ruskserver.deepwither_V2.modules.ai.api;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiter {

    private int globalRpm;
    private int userRpm;
    private int dailyLimit;

    private final Object lock = new Object();
    private CircularBuffer globalTimestamps;
    private final Map<UUID, CircularBuffer> userTimestamps = new ConcurrentHashMap<>();
    private int todayCount = 0;
    private int todayResetDay = -1;

    @Inject
    public RateLimiter() {
        this.globalRpm = 1;
        this.userRpm = 1;
        this.dailyLimit = 100;
        this.globalTimestamps = new CircularBuffer(1);
    }

    public synchronized void configure(int globalRpm, int userRpm, int dailyLimit) {
        this.globalRpm = globalRpm;
        this.userRpm = userRpm;
        this.dailyLimit = dailyLimit;
        this.globalTimestamps = new CircularBuffer(globalRpm);
        this.userTimestamps.clear();
        this.todayCount = 0;
        this.todayResetDay = -1;
    }

    public enum Result {
        ALLOWED,
        RATE_LIMITED_GLOBAL,
        RATE_LIMITED_USER,
        DAILY_LIMIT_EXCEEDED
    }

    public Result tryAcquire(UUID userId) {
        synchronized (lock) {
            long now = System.currentTimeMillis();

            int currentDay = Instant.now().atZone(java.time.ZoneOffset.UTC).getDayOfYear();
            if (currentDay != todayResetDay) {
                todayCount = 0;
                todayResetDay = currentDay;
            }
            if (todayCount >= dailyLimit) {
                return Result.DAILY_LIMIT_EXCEEDED;
            }

            if (!globalTimestamps.tryAdd(now)) {
                return Result.RATE_LIMITED_GLOBAL;
            }

            CircularBuffer userBuf = userTimestamps.computeIfAbsent(userId, k -> new CircularBuffer(userRpm));
            if (!userBuf.tryAdd(now)) {
                return Result.RATE_LIMITED_USER;
            }

            todayCount++;
            return Result.ALLOWED;
        }
    }

    public int getRemainingDaily() {
        synchronized (lock) {
            return Math.max(0, dailyLimit - todayCount);
        }
    }

    public long getGlobalWaitMillis() {
        synchronized (lock) {
            return globalTimestamps.getWaitMillis(System.currentTimeMillis());
        }
    }

    public long getUserWaitMillis(UUID userId) {
        CircularBuffer buf = userTimestamps.get(userId);
        if (buf == null) return 0;
        synchronized (lock) {
            return buf.getWaitMillis(System.currentTimeMillis());
        }
    }

    private static class CircularBuffer {
        private final long[] timestamps;
        private int head = 0;
        private int count = 0;
        private final int capacity;

        CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.timestamps = new long[capacity];
        }

        boolean tryAdd(long now) {
            long windowStart = now - 60_000;
            int valid = 0;
            for (int i = 0; i < count; i++) {
                int idx = (head - count + i + capacity) % capacity;
                if (timestamps[idx] >= windowStart) {
                    timestamps[(head - valid - 1 + capacity) % capacity] = timestamps[idx];
                    valid++;
                }
            }
            count = valid;
            head = (head + 1) % capacity;

            if (count >= capacity) {
                return false;
            }
            timestamps[(head - 1 + capacity) % capacity] = now;
            count++;
            return true;
        }

        long getWaitMillis(long now) {
            if (count < capacity) return 0;
            long oldest = timestamps[(head - count + capacity) % capacity];
            long elapsed = now - oldest;
            return elapsed < 60_000 ? (60_000 - elapsed) : 0;
        }
    }
}
