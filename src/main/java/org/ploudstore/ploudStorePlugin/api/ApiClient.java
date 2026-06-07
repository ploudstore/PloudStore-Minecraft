package org.ploudstore.ploudStorePlugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.ploudstore.ploudStorePlugin.model.FetchResult;
import org.ploudstore.ploudStorePlugin.model.PendingResponse;
import org.ploudstore.ploudStorePlugin.model.PinResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

public class ApiClient {

    private final HttpClient httpClient;
    private final Gson gson;
    private final Logger logger;
    private final String baseUrl;
    private final String secretKey;
    private final int timeoutSeconds;
    private final int maxRetries;

    public ApiClient(String baseUrl, String secretKey, int timeoutSeconds, int maxRetries, Logger logger) {
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
                    logger.warning("[PloudStore] Rate limited (HTTP 429).");
                    return FetchResult.rateLimited();
                }

                if (response.statusCode() == 403) {
                    logger.warning("[PloudStore] Forbidden (HTTP 403) — check your secret key.");
                    return FetchResult.forbidden();
                }

                logger.warning("[PloudStore] fetch attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + response.statusCode() + ": " + truncate(response.body(), 300));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return FetchResult.error();
            } catch (Exception e) {
                logger.warning("[PloudStore] fetch attempt " + attempt + "/" + maxRetries
                        + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            if (attempt < maxRetries) sleepBackoff(attempt);
        }

        logger.severe("[PloudStore] All " + maxRetries + " fetch attempts failed.");
        return FetchResult.error();
    }

    /**
     * Confirms a batch of executed commands in a single API call.
     * Endpoint: POST /commands/complete  Body: {"ids": ["id1","id2",...]}
     */
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

                logger.warning("[PloudStore] deleteCommands attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + response.statusCode() + ": " + truncate(response.body(), 200));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                logger.warning("[PloudStore] deleteCommands attempt " + attempt + "/" + maxRetries
                        + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            if (attempt < maxRetries) sleepBackoff(attempt);
        }

        logger.severe("[PloudStore] Failed to confirm " + ids.size() + " command(s) after " + maxRetries + " attempts!");
        return false;
    }

    /**
     * Fetches a one-time PIN for the given player name.
     * Endpoint: GET /auth/pin/{name}
     * Returns a PinResponse (with pin == null if the player has no active session),
     * or null if the request failed entirely.
     */
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

                logger.warning("[PloudStore] fetchPlayerPin attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + response.statusCode() + ": " + truncate(response.body(), 200));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                logger.warning("[PloudStore] fetchPlayerPin attempt " + attempt + "/" + maxRetries
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
