package masteringthreads.ch3_the_secrets_of_concurrency.exercise_3_1;

import java.util.*;

public class ThreadPool {
    // Create a LinkedList field containing Runnable. This is our "tasks" queue.
    // @GuardedBy("tasks")
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

    @SuppressWarnings("deprecation")
    public void shutdown() {
        // this should call interrupt() on the worker threads.
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
            while(true) {
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
