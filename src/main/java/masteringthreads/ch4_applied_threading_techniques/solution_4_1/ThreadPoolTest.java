package masteringthreads.ch4_applied_threading_techniques.solution_4_1;

import org.junit.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

import static org.junit.Assert.*;

public class ThreadPoolTest {
    @Test
    public void testTasksAreStopped() throws InterruptedException {
        var pool = new ThreadPool(1);
        var latch = new CountDownLatch(1);
        var time = System.currentTimeMillis();
        pool.submit(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        Thread.sleep(1000);
        pool.shutdown();
        boolean noTimeout = latch.await(100, TimeUnit.MILLISECONDS);
        assertTrue("timeout occurred - did not shutdown the threads in time?", noTimeout);
        time = System.currentTimeMillis() - time;
    }

    @Test
    public void testThatRunnablesAreExecutedConcurrently() throws InterruptedException {
        checkStandardThreadPoolFunctionality(new ThreadPool(10));
    }

    @Test
    public void testSynchronizingOnListObject() throws ReflectiveOperationException, InterruptedException {
        if (checkListAndBlockingQueue()) return;


        var pool = new ThreadPool(10);

        Consumer<Runnable> locker = findFieldValue(pool, ReentrantLock.class)
            .<Consumer<Runnable>>map(lck -> runnable -> {
                lck.lock();
                try {
                    runnable.run();
                } finally {
                    lck.unlock();
                }
            }).orElseGet(() -> runnable -> {
                var list = findFieldValue(pool, LinkedList.class)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Field of type LinkedList not found"));
                synchronized (list) {
                    runnable.run();
                }
            });

        locker.accept(() -> {
            var thread = new Thread(() -> {
                pool.submit(() -> System.out.println("submit worked"));
            });
            thread.start();
            try {
                thread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertTrue("In submit(), we expected the pool to be synchronizing list access using the list object as a monitor lock or to use a ReentrantLock", thread.isAlive());
        });
        locker.accept(() -> {
            var thread = new Thread(() -> {
                System.out.println("pool.getRunQueueLength() = " + pool.getRunQueueLength());
            });
            thread.start();
            try {
                thread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertTrue("In getRunQueueLength(), we expected the pool to be synchronizing list access using the list object as a monitor lock or to use a ReentrantLock", thread.isAlive());
        });
        pool.shutdown();
    }

    @Test
    public void testSpuriousWakeupsAreHandledCorrectly() throws InterruptedException, IllegalAccessException {
        if (checkListAndBlockingQueue()) return;

        var pool = new ThreadPool(10);
        Thread.sleep(100);

        Runnable notifier = findFieldValue(pool, ReentrantLock.class)
            .<Runnable>map(lck -> () -> {
                Condition condition = findFieldValue(pool, Condition.class)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Field of type Condition should be paired with ReentrantLock"
                    ));
                lck.lock();
                try {
                    condition.signalAll();
                } finally {
                    lck.unlock();
                }
            }).orElseGet(() -> () -> {
                var list = findFieldValue(pool, LinkedList.class)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Field of type LinkedList not found"));
                synchronized (list) {
                    list.notifyAll();
                }
            });

        for (int i = 0; i < 20; i++) {
            notifier.run();
        }
        checkStandardThreadPoolFunctionality(pool);
    }

    private boolean checkListAndBlockingQueue() throws IllegalAccessException {
        ThreadPool sample = new ThreadPool(1);
        try {
            var foundLinkedListField = false;
            var foundBlockingQueueField = false;
            for (var field : ThreadPool.class.getDeclaredFields()) {
                if (Executor.class.isAssignableFrom(field.getType())) {
                    return true;
                } else if (Collection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object collection = field.get(sample);
                    if (collection instanceof LinkedList)
                        foundLinkedListField = true;
                    else if (collection instanceof BlockingQueue)
                        foundBlockingQueueField = true;
                }
            }
            if (foundBlockingQueueField && !foundLinkedListField) return true;
            if (foundBlockingQueueField && foundLinkedListField)
                fail("We don't need a LinkedList for the tasks if we use a BlockingQueue");
            if (!foundLinkedListField)
                fail("We need a LinkedList field for the tasks");
            return false;
        } finally {
            sample.shutdown();
        }
    }

    private void checkStandardThreadPoolFunctionality(ThreadPool pool) throws InterruptedException {
        var latch = new CountDownLatch(19);
        var time = System.currentTimeMillis();
        for (int i = 0; i < 19; i++) {
            pool.submit(() -> {
                try {
                    Thread.sleep(1000);
                    latch.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        boolean noTimeout = latch.await(3, TimeUnit.SECONDS);
        assertTrue("timeout occurred - did you start your threads?", noTimeout);
        time = System.currentTimeMillis() - time;
        pool.shutdown();
        if (pool.getRunQueueLength() != 0) {
            throw new AssertionError("Queue was not empty: "
                + pool.getRunQueueLength());
        }
        assertTrue("Total time exceeded limits", time < 2400);
        assertFalse("Faster than expected", time < 1900);
    }

    private <E> Optional<E> findFieldValue(ThreadPool pool, Class<E> fieldType) {
        try {
            for (var field : pool.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (fieldType.isInstance(field.get(pool)))
                    return Optional.of(fieldType.cast(field.get(pool)));
            }
            return Optional.empty();
        } catch (IllegalAccessException e) {
            return Optional.empty();
        }
    }

    private Thread interrupted = null;

    @Test
    public void testForBackupBoolean() throws InterruptedException {
        var latch = new CountDownLatch(8);
        var pool = new ThreadPool(10);
        for (int i = 0; i < 12; i++) {
            pool.submit(() -> {
                try {
                    Thread.sleep(1000);
                    latch.countDown();
                } catch (InterruptedException e) {
                    interrupted = Thread.currentThread();
                }
            });
        }
        boolean noTimeout = latch.await(2, TimeUnit.SECONDS);
        assertTrue("timeout occurred - did you start your threads?", noTimeout);
        pool.shutdown();
        Thread.sleep(100);
        assertTrue("Did you have a backup boolean?",
            interrupted == null || !interrupted.isAlive());
    }
}
