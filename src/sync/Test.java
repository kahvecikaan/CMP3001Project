package sync;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Test {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ReadWriteLock RW = new ReadWriteLock();
        SharedData sharedData = new SharedData(); // Shared data object

        for (int i = 0; i < 4; i++) {
            executorService.execute(new Writer(RW, sharedData));
            executorService.execute(new Reader(RW, sharedData));
        }

        executorService.shutdown();
    }
}

class SharedData {
    private int data = 0;

    public synchronized int read() {
        return data;
    }

    public synchronized void write() {
        data++;
    }
}

class Writer implements Runnable {
    private ReadWriteLock RW_lock;
    private SharedData sharedData;

    public Writer(ReadWriteLock rw, SharedData sharedData) {
        RW_lock = rw;
        this.sharedData = sharedData;
    }

    public void run() {
        while (true) {
            RW_lock.writeLock();
            sharedData.write();
            System.out.println("Writer wrote: " + sharedData.read());
            RW_lock.writeUnLock();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

class ReadWriteLock {
    private Semaphore readLock = new Semaphore(1, true); // Fair semaphore
    private Semaphore writeLock = new Semaphore(1, true); // Fair semaphore
    private Semaphore turnstile = new Semaphore(1, true); // Fair semaphore to prevent reader starvation
    private AtomicInteger readCount = new AtomicInteger(0);
    private AtomicInteger writerWaiting = new AtomicInteger(0);

    public void readLock() {
        try {
            turnstile.acquire(); // First acquire the turnstile semaphore
            turnstile.release(); // Then release it immediately

            readLock.acquire();
            if (readCount.incrementAndGet() == 1) {
                writeLock.acquire(); // Block writers if this is the first reader
            }
            readLock.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void readUnLock() {
        try {
            readLock.acquire();
            if (readCount.decrementAndGet() == 0) {
                writeLock.release(); // Allow writers if this is the last reader
            }
            readLock.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void writeLock() {
        try {
            writerWaiting.incrementAndGet();
            turnstile.acquire(); // Block readers from acquiring the turnstile
            writeLock.acquire(); // Ensure exclusive access for writing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writerWaiting.decrementAndGet();
        }
    }

    public void writeUnLock() {
        writeLock.release(); // Release exclusive writing access
        turnstile.release(); // Allow readers to proceed
    }

    public boolean canRead() {
        return readCount.get() > 0;
    }
}


class Reader implements Runnable {
    private ReadWriteLock RW_lock;
    private SharedData sharedData;

    public Reader(ReadWriteLock rw, SharedData sharedData) {
        RW_lock = rw;
        this.sharedData = sharedData;
    }

    public void run() {
        while (true) {
            RW_lock.readLock();
            if (RW_lock.canRead()) {
                System.out.println("Reader read: " + sharedData.read());
            }
            RW_lock.readUnLock();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
