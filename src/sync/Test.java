package sync;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


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

    public int read() {
        return data;
    }

    public void write() {
        data++;
    }
}


class Writer implements Runnable
{
    private ReadWriteLock RW_lock;
    private SharedData sharedData;

    public Writer(ReadWriteLock rw, SharedData sharedData) {
        RW_lock = rw;
        this.sharedData = sharedData;
    }

    public void run() {
        while (true){
            RW_lock.writeLock();
            sharedData.write();
            System.out.println("Writer wrote: " + sharedData.read());
            RW_lock.writeUnLock();

            // sleep a bit to stimulate writing delay
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}


class ReadWriteLock {
    private Semaphore readLock = new Semaphore(1);
    private Semaphore writeLock = new Semaphore(1);
    private Semaphore preferWrite = new Semaphore(1); // to give priority to writers
    private int readCount = 0;

    // Method for readers to acquire the lock
    public void readLock() {
        try {
            preferWrite.acquire(); // check if there is a writer waiting
            readLock.acquire();
            readCount++;
            if (readCount == 1) {
                writeLock.acquire(); // if this is the first reader, then block writers
            }
            readLock.release();
            preferWrite.release(); // release to allow other readers or waiting writers
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Method for readers to release the lock
    public void readUnLock() {
        try {
            readLock.acquire();
            readCount--;
            if (readCount == 0) {
                writeLock.release(); // if this is the last reader, then unblock writers
            }
            readLock.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Method for writers to acquire the lock
    public void writeLock() {
        try {
            preferWrite.acquire(); // check if there is a writer waiting
            writeLock.acquire(); // ensure exclusive access for writing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Method for writers to release the lock
    public void writeUnLock() {
        writeLock.release();
        preferWrite.release(); // release to allow other readers or waiting writers
    }
}


class Reader implements Runnable
{
    private ReadWriteLock RW_lock;
    private SharedData sharedData;

    public Reader(ReadWriteLock rw, SharedData sharedData) {
        RW_lock = rw;
        this.sharedData = sharedData;
    }

    public void run() {
        while (true){
            RW_lock.readLock();
            System.out.println("Reader read: " + sharedData.read());
            RW_lock.readUnLock();

            // sleep a bit to stimulate reading delay
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
