package com.zetaplugins.netwatchz.common.iplist;

import com.zetaplugins.netwatchz.common.config.IpListConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads and updates multiple IP lists from remote URLs at specified intervals.
 */
public final class IpListFetcher {
    private final Logger logger;

    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;

    public IpListFetcher(Logger logger) {
        this.logger = logger;
        this.scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Creates and starts an IpListFetcher from the given configuration.
     * @param cfg configuration containing fetch jobs
     * @param logger logger for logging fetch results
     * @return started IpListFetcher instance
     */
    public static IpListFetcher fromConfig(IpListConfig cfg, Logger logger) {
        var fetcher = new IpListFetcher(logger);
        if (!cfg.fetchJobs().isEmpty()) fetcher.start(cfg.fetchJobs());
        return fetcher;
    }

    /**
     * Starts fetching and updating the given jobs.
     * @param jobs list of fetch jobs to schedule
     */
    public void start(List<IpListFetchJob> jobs) {
        for (IpListFetchJob job : jobs) {
            fetchAndSave(job);

            Runnable task = () -> fetchAndSave(job);
            scheduler.scheduleAtFixedRate(
                    task,
                    job.updateInterval().toSeconds(),
                    job.updateInterval().toSeconds(),
                    TimeUnit.SECONDS
            );
        }
    }

    /**
     * Stops all scheduled fetches. Safe to call on plugin shutdown.
     */
    public void stop() {
        scheduler.shutdownNow();
    }

    private void fetchAndSave(IpListFetchJob job) {
        try {
            logger.log(Level.INFO, "Fetching IP list from " + job.url() + " ...");
            var start = System.currentTimeMillis();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(job.url()))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                Path destination = job.destination();
                Files.createDirectories(destination.getParent());
                Path tempFile = Files.createTempFile(destination.getParent(), "tmp", ".download");
                try (InputStream in = response.body()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

                var duration = System.currentTimeMillis() - start;
                logger.log(Level.INFO, "Successfully updated IP list from " + job.url() + " to " + job.destination() + " in " + duration + " ms");
            } else {
                logger.log(Level.WARNING, "Failed to fetch " + job.url() + ": HTTP " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Error fetching " + job.url(), e);
        }
    }
}