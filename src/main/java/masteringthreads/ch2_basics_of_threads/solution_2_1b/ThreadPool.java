package masteringthreads.ch2_basics_of_threads.solution_2_1b;

import java.util.*;
import java.util.concurrent.locks.*;

// solution_2_1 #1 - not perfect yet ...
public class ThreadPool {
    // Create a LinkedList field containing Runnable. This is our "tasks" queue.
    private final Lock tasksLock = new ReentrantLock();
    private final Condition tasksNotEmpty = tasksLock.newCondition();
    // @GuardedBy("tasksLock")
    private final Queue<Runnable> tasks = new LinkedList<>();
    // Create an ArrayList containing all the Worker threads.
    // @GuardedBy("workers")
    private final Collection<Worker> workers = new ArrayList<>();

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
        tasksLock.lock();
        try {
            // if the LinkedList is empty, we wait
            while (tasks.isEmpty()) tasksNotEmpty.await();
            // remove the first task from the LinkedList and return it
            return tasks.remove();

        } finally {
            tasksLock.unlock();
        }
    }

    public void submit(Runnable job) {
        // Add the task to the LinkedList and notifyAll
        tasksLock.lock();
        try {
            tasks.add(job);
            tasksNotEmpty.signal();
        } finally {
            tasksLock.unlock();
        }
    }

    public int getRunQueueLength() {
        // return the length of the LinkedList
        // remember to also synchronize!
        tasksLock.lock();
        try {
            return tasks.size();
        } finally {
            tasksLock.unlock();
        }
    }

    @SuppressWarnings("deprecation")
    public void shutdown() {
        // this should call stop() on the worker threads.
        synchronized (workers) {
            workers.forEach(Thread::stop);
        }
    }

    private class Worker extends Thread {
        public Worker(String name) {
            super(name);
        }

        public void run() {
            // we run in an infinite loop:
            while (true) {
                // remove the next task from the linked list using take()
                // we then call the run() method on the job
                try {
                    take().run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
