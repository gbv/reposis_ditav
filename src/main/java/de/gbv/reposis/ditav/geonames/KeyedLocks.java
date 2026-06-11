package de.gbv.reposis.ditav.geonames;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A table of {@link ReentrantLock}s keyed by an arbitrary string.
 * <p>
 * {@link #acquire(String)} returns an {@link AutoCloseable} handle that releases the lock when
 * closed and removes the lock from the table once no thread holds or waits for it anymore. The
 * table therefore stays bounded without a separate cleanup step, while different keys lock
 * independently and the same key locks mutually exclusively.
 * <p>
 * Intended for try-with-resources:
 * <pre>{@code
 * try (KeyedLocks.LockHandle handle = locks.acquire(id)) {
 *     // critical section for this id
 * }
 * }</pre>
 */
public final class KeyedLocks {

    private final ConcurrentMap<String, CountedLock> locks = new ConcurrentHashMap<>();

    /**
     * Acquires the lock for the given key, blocking until it is available.
     *
     * @param key the key to lock on
     * @return a handle that must be closed (ideally via try-with-resources) to release the lock
     */
    public LockHandle acquire(String key) {
        // register interest before locking so the entry cannot be evicted while we wait for it
        CountedLock counted = locks.compute(key, (k, existing) -> {
            CountedLock current = existing != null ? existing : new CountedLock();
            current.refCount++;
            return current;
        });
        counted.lock.lock();
        return new LockHandle(key, counted);
    }

    /**
     * @return the number of currently registered locks (for testing)
     */
    int size() {
        return locks.size();
    }

    private void release(String key, CountedLock counted) {
        counted.lock.unlock();
        // drop our interest and remove the entry once nobody holds or waits for the lock anymore
        locks.compute(key, (k, existing) -> {
            existing.refCount--;
            return existing.refCount == 0 ? null : existing;
        });
    }

    /**
     * A held lock. Closing it releases the lock and removes the table entry if it was the last
     * holder.
     */
    public final class LockHandle implements AutoCloseable {

        private final String key;

        private final CountedLock counted;

        private LockHandle(String key, CountedLock counted) {
            this.key = key;
            this.counted = counted;
        }

        @Override
        public void close() {
            release(key, counted);
        }
    }

    private static final class CountedLock {

        private final ReentrantLock lock = new ReentrantLock();

        /** Number of threads holding or waiting for the lock; guarded by the map's compute(). */
        private int refCount;
    }
}
