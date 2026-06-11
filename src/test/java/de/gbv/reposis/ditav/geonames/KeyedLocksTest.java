package de.gbv.reposis.ditav.geonames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class KeyedLocksTest {

    @Test
    void evictsEntryWhenLastHolderReleases() {
        KeyedLocks locks = new KeyedLocks();
        try (KeyedLocks.LockHandle handle = locks.acquire("554234")) {
            assertEquals(1, locks.size(), "lock must be registered while held");
        }
        assertEquals(0, locks.size(), "lock must be evicted once released");
    }

    @Test
    void keepsEntryWhileAnotherThreadWaits() throws InterruptedException {
        KeyedLocks locks = new KeyedLocks();
        CountDownLatch firstHeld = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger sizeWhileContended = new AtomicInteger(-1);

        Thread holder = new Thread(() -> {
            try (KeyedLocks.LockHandle handle = locks.acquire("1")) {
                firstHeld.countDown();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        holder.start();
        assertTrue(firstHeld.await(5, TimeUnit.SECONDS));

        Thread waiter = new Thread(() -> {
            try (KeyedLocks.LockHandle handle = locks.acquire("1")) {
                // got the lock after the holder released it
            }
        });
        waiter.start();
        // both threads reference the same key -> exactly one entry, never evicted mid-flight
        Thread.sleep(100);
        sizeWhileContended.set(locks.size());

        release.countDown();
        holder.join(5000);
        waiter.join(5000);

        assertEquals(1, sizeWhileContended.get(), "entry must survive while a waiter references it");
        assertEquals(0, locks.size(), "entry must be evicted once both threads released");
    }
}
