package ngrok.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BlockingCell<T> {

    private CountDownLatch latch = new CountDownLatch(1);
    private AtomicReference<T> value = new AtomicReference<>();

    public T get() throws InterruptedException {
        latch.await();
        return value.get();
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException {
        latch.await(timeout, unit);
        return value.get();
    }

    public void set(T value) {
        this.value.set(value);
        latch.countDown();
    }

}
