package io.github.cloudemulators.gcsemulator.lifecycle;

import io.github.cloudemulators.gcsemulator.GCSServer;
import io.github.cloudemulators.gcsemulator.store.FileStore;
import io.github.cloudemulators.gcsemulator.store.MemoryStore;
import io.github.cloudemulators.gcsemulator.store.NioStore;
import io.github.cloudemulators.gcsemulator.store.helper.FileStoreConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Starter {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private static final List<GCSServer> servers = new ArrayList<>();
    private static CountDownLatch countDownLatch = new CountDownLatch(3);

    public static void main(String[] args) throws IOException, InterruptedException {
        executorService.submit(() -> {
            servers.add(new GCSServer(8080, new MemoryStore()).start());
            System.out.println("Memory Store Server started on port 8080");
            countDownLatch.countDown();
        });

        executorService.submit(() -> {
            servers.add(new GCSServer(8081, new NioStore()).start());
            System.out.println("Nio Store Server started on port 8081");
            countDownLatch.countDown();
        });

        executorService.submit(() -> {
            servers.add(new GCSServer(8082, new FileStore(new FileStoreConfig("storage", true))).start());
            System.out.println("File Store Server started on port 8082");
            countDownLatch.countDown();
        });

        countDownLatch.await();
        ControlChannel controlChannel = new ControlChannel(executorService, servers);
        controlChannel.start();
    }
}
