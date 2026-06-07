package org.ploudstore.ploudStorePlugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.ploudstore.ploudStorePlugin.PluginLogger;
import org.ploudstore.ploudStorePlugin.model.FetchResult;
import org.ploudstore.ploudStorePlugin.model.PendingResponse;
import org.ploudstore.ploudStorePlugin.model.PinResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class ApiClient {

    private final HttpClient httpClient;
    private final Gson gson;
    private final PluginLogger logger;
    private final String baseUrl;
    private final String secretKey;
    private final int timeoutSeconds;
    private final int maxRetries;

    public ApiClient(String baseUrl, String secretKey, int timeoutSeconds, int maxRetries, PluginLogger logger) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.secretKey = secretKey;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.logger = logger;
        this.gson = new Gson();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public FetchResult fetchPendingCommands() {
        String url = baseUrl + "/commands/pending";

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("X-store-Key", secretKey)
                        .header("Accept", "application/json")
                        .GET()
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    PendingResponse parsed = gson.fromJson(response.body(), PendingResponse.class);
                    if (parsed == null) {
                        logger.warning("[PloudStore] Gson returned null for fetch response.");
                        return FetchResult.error();
                    }
                    return FetchResult.ok(parsed);
                }

                if (response.statusCode() == 429) {
                    return FetchResult.rateLimited();
                }

                if (response.statusCode() == 403) {
                    logger.warning("[PloudStore] Forbidden (HTTP 403) — check your secret key.");
                    return FetchResult.forbidden();
                }

                logger.debug("[PloudStore] fetch attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + response.statusCode() + ": " + truncate(response.body(), 300));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return FetchResult.error();
            } catch (Exception e) {
                logger.debug("[PloudStore] fetch attempt " + attempt + "/" + maxRetries
                        + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            if (attempt < maxRetries) sleepBackoff(attempt);
        }

        return FetchResult.error();
    }

    public boolean deleteCommands(List<String> ids) {
        String url = baseUrl + "/commands/complete";

        JsonArray idArray = new JsonArray();
        ids.forEach(idArray::add);
        JsonObject body = new JsonObject();
        body.add("ids", idArray);
        String bodyStr = gson.toJson(body);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("X-store-Key", secretKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return true;
                }

                logger.debug("[PloudStore] deleteCommands attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + response.statusCode() + ": " + truncate(response.body(), 200));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                logger.debug("[PloudStore] deleteCommands attempt " + attempt + "/" + maxRetries
                        + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            if (attempt < maxRetries) sleepBackoff(attempt);
        }

        logger.warning("[PloudStore] Failed to confirm " + ids.size() + " command(s) after " + maxRetries + " attempts!");
        return false;
    }

    public PinResponse fetchPlayerPin(String playerName) {
        String url = baseUrl + "/auth/pin/" + playerName;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("X-store-Key", secretKey)
                        .header("Accept", "application/json")
                        .GET()
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    PinResponse parsed = gson.fromJson(response.body(), PinResponse.class);
                    return parsed != null ? parsed : new PinResponse();
                }

                if (response.statusCode() == 403) {
                    logger.warning("[PloudStore] fetchPlayerPin: Forbidden (HTTP 403) — check your secret key.");
                    return null;
                }

                logger.debug("[PloudStore] fetchPlayerPin attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + response.statusCode() + ": " + truncate(response.body(), 200));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                logger.debug("[PloudStore] fetchPlayerPin attempt " + attempt + "/" + maxRetries
                        + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            if (attempt < maxRetries) sleepBackoff(attempt);
        }

        return null;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(1000L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "(null)";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
