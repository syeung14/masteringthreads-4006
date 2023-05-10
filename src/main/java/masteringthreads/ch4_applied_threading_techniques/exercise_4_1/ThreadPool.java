package masteringthreads.ch4_applied_threading_techniques.exercise_4_1;

import java.util.*;
import java.util.concurrent.locks.*;

// TODO: Replace LinkedList with LinkedBlockingQueue and ArrayList with
// TODO: ConcurrentLinkedQueue
public class ThreadPool {
    // @GuardedBy("tasks")
    private final Queue<Runnable> tasks = new LinkedList<>();
    // @GuardedBy("workers")
    private final Collection<Worker> workers = new ArrayList<>();
    private volatile boolean running = true;

    public ThreadPool(int poolSize) {
        for (int i = 0; i < poolSize; i++) {
            var worker = new Worker("worker-" + i);
            worker.start();
            synchronized (workers) {
                workers.add(worker);
            }
        }
    }

    private Runnable take() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        synchronized (tasks) {
            // if the LinkedList is empty, we wait
            while (tasks.isEmpty()) tasks.wait();
            // remove the first task from the LinkedList and return it
            return tasks.remove();
        }
    }

    public void submit(Runnable job) {
        // Add the task to the LinkedList and notifyAll
        synchronized (tasks) {
            tasks.add(job);
            tasks.notifyAll();
        }
    }

    public int getRunQueueLength() {
        // return the length of the LinkedList
        // remember to also synchronize!
        synchronized (tasks) {
            return tasks.size();
        }
    }

    public void shutdown() {
        running = false;
        synchronized (workers) {
            workers.forEach(Thread::interrupt);
        }
    }

    private class Worker extends Thread {
        public Worker(String name) {
            super(name);
        }

        public void run() {
            // we run in an infinite loop:
            while(running) {
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

