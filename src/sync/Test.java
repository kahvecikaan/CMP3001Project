package sync;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


public class Test {
    public static void main(String [] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ReadWriteLock RW = new ReadWriteLock();


        executorService.execute(new Writer(RW));
        executorService.execute(new Writer(RW));
        executorService.execute(new Writer(RW));
        executorService.execute(new Writer(RW));

        executorService.execute(new Reader(RW));
        executorService.execute(new Reader(RW));
        executorService.execute(new Reader(RW));
        executorService.execute(new Reader(RW));


    }
}

class Writer implements Runnable
{
    private ReadWriteLock RW_lock;


    public Writer(ReadWriteLock rw) {
        RW_lock = rw;
    }

    public void run() {
        while (true){
            RW_lock.writeLock();

            RW_lock.writeUnLock();

        }
    }
}

class ReadWriteLock {
    private Semaphore mutex;
    private Semaphore writeLock;
    private int readCount;

    public ReadWriteLock() {
        mutex = new Semaphore(1);
        writeLock = new Semaphore(1);
        readCount = 0;
    }

    public void readLock() {
        try {
            mutex.acquire();
            readCount++;
            if(readCount == 1) {
                writeLock.acquire();
            }
            mutex.release();
        }catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void writeLock() {
        try {
            writeLock.acquire();
        }catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void readUnLock() {
        try {
            mutex.acquire();
            readCount--;
            if(readCount == 0) {
                writeLock.release();
            }
            mutex.release();
        }catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void writeUnLock() {
        writeLock.release();
    }
}


class Reader implements Runnable
{
    private ReadWriteLock RW_lock;


    public Reader(ReadWriteLock rw) {
        RW_lock = rw;
    }
    public void run() {
        while (true){
            RW_lock.readLock();


            RW_lock.readUnLock();

        }
    }
}