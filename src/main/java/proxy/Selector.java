package proxy;

import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;

public class Selector {
    final int NUM_THREADS = 10;

    /**
     * Does not alter the default selector, but allows for more than one user by using threads
     *
     * @param uri is the URI requested by the client
     * @throws InterruptedException  if any thread has interrupted the current thread.
             * The interrupted status of the current thread is cleared when this exception is thrown.
     */
    public Selector(URI uri) throws InterruptedException {
        System.setProperty("java.net.useSystemProxies", "true");
        final ProxySelector proxySelector = ProxySelector.getDefault();

        // Creating array of threads
        Thread[] threads = new Thread[NUM_THREADS];

        // Setting threads in a runnable state
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                try {
                    proxySelector.select(uri);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        // Now each thread has been created, are in a runnable state, and it is time to start them
        for (Thread thread : threads) {
            thread.start();
            System.out.println(" thread id: " + thread.getId());
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    public static void main(String[] args) {
        try {
            Selector selector = new Selector(new URI("https://youtube.com"));
        } catch (InterruptedException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
//todo more methods?

