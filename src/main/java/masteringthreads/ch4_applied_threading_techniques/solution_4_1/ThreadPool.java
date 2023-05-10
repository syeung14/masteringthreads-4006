package masteringthreads.ch4_applied_threading_techniques.solution_4_1;

import java.util.*;
import java.util.concurrent.*;

// solution #3 - even better ...
public class ThreadPool {
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final Collection<Worker> workers = new ConcurrentLinkedQueue<>();
    private volatile boolean running = true;

    public ThreadPool(int poolSize) {
        for (int i = 0; i < poolSize; i++) {
            var worker = new Worker("worker-" + i);
            worker.start();
            workers.add(worker);
        }
    }

    private Runnable take() throws InterruptedException {
        return tasks.take();
    }

    public void submit(Runnable job) {
        tasks.add(job);
    }

    public int getRunQueueLength() {
        return tasks.size();
    }

    public void shutdown() {
        running = false;
        workers.forEach(Thread::interrupt);
    }

    private class Worker extends Thread {
        public Worker(String name) {
            super(name);
        }

        public void run() {
            // we run in an infinite loop:
            while (running) {
                // remove the next task from the linked list using take()
                // we then call the run() method on the job
                try {
                    take().run();
                } catch (InterruptedException consumeAndExit) {
                    break;
                }
            }
        }
    }
}
