package sa.urtv.home.pair;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A call that cannot hang for good (review F96): the libadb stream read and the provider call have no timeout of their own, so a Wi-Fi link that stalls mid-setup would hold
 * the setup thread and an add-on binder thread forever. {@code call} runs the work on a thread of its own and waits at most {@code ms}; late, it runs {@code onLate}
 * (the caller closes the connection, which ends the stuck read) and throws IOException("timeout"). No Android in here: tests/DeadlineRun.java runs it.
 */
final class Deadline {
    private Deadline() { }

    static <T> T call(Callable<T> work, long ms, Runnable onLate) throws IOException {
        ExecutorService ex = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "pair-call"); t.setDaemon(true); return t; });
        Future<T> f = ex.submit(work);
        try {
            return f.get(ms, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            if (onLate != null) onLate.run();
            throw new IOException("timeout");
        } catch (InterruptedException e) {
            f.cancel(true);
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            if (c instanceof IOException) throw (IOException) c;
            if (c instanceof RuntimeException) throw (RuntimeException) c;
            throw new IOException(String.valueOf(c));
        } finally {
            ex.shutdown();
        }
    }
}
