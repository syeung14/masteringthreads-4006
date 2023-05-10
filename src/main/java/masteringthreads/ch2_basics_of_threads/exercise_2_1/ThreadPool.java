package masteringthreads.ch2_basics_of_threads.exercise_2_1;

public class ThreadPool {
    // Create a LinkedList field containing Runnable. This is our "tasks" queue.
    // Hint: Since LinkedList is not thread-safe, we need to synchronize it.
    // Create an ArrayList containing all the Worker threads.
    // Hint: ArrayList is also not thread-safe, so we need to synchronize it.

    public ThreadPool(int poolSize) {
        // create several Worker threads and add them to workers list
        // Hint: Worker is an inner class defined at the bottom of this class
    }

    private Runnable take() throws InterruptedException {
        // if the LinkedList is empty, we wait
        //
        // remove the first task from the LinkedList and return it
        throw new UnsupportedOperationException("not implemented");
    }

    public void submit(Runnable task) {
        // Add the task to the LinkedList and notifyAll
    }

    public int getRunQueueLength() {
        // return the length of the LinkedList
        // remember to also synchronize!
        throw new UnsupportedOperationException("not implemented");
    }

    @SuppressWarnings("deprecation")
    public void shutdown() {
        // this should call stop() on the worker threads.
    }

    private class Worker extends Thread {
        public Worker(String name) {
            super(name);
        }

        public void run() {
            // we run in an infinite loop:
            // remove the next task from the linked list using take()
            // we then call the run() method on the task
        }
    }
}
