package org.ploudstore.ploudStorePlugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.ploudstore.ploudStorePlugin.PluginLogger;
import org.ploudstore.ploudStorePlugin.model.FetchResult;
import org.ploudstore.ploudStorePlugin.model.PendingResponse;
import org.ploudstore.ploudStorePlugin.model.PinResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ApiClient {

    private final Gson gson;
    private final PluginLogger logger;
    private final String baseUrl;
    private final String secretKey;
    private final int timeoutMs;
    private final int maxRetries;

    public ApiClient(String baseUrl, String secretKey, int timeoutSeconds, int maxRetries, PluginLogger logger) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.secretKey = secretKey;
        this.timeoutMs = timeoutSeconds * 1000;
        this.maxRetries = maxRetries;
        this.logger = logger;
        this.gson = new Gson();
    }

    public FetchResult fetchPendingCommands() {
        String url = baseUrl + "/commands/pending";

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResult result = get(url);

                if (result.status == 200) {
                    logger.debug("[PloudStore] Raw pending response: " + result.body);
                    PendingResponse parsed = gson.fromJson(result.body, PendingResponse.class);
                    if (parsed == null) {
                        logger.warning("[PloudStore] Gson returned null for fetch response.");
                        return FetchResult.error();
                    }
                    return FetchResult.ok(parsed);
                }

                if (result.status == 429) {
                    return FetchResult.rateLimited();
                }

                if (result.status == 403) {
                    logger.warning("[PloudStore] Forbidden (HTTP 403) — check your secret key.");
                    return FetchResult.forbidden();
                }

                logger.debug("[PloudStore] fetch attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + result.status + ": " + truncate(result.body, 300));

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
        for (String id : ids) idArray.add(id);
        JsonObject body = new JsonObject();
        body.add("ids", idArray);
        String bodyStr = gson.toJson(body);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResult result = post(url, bodyStr);

                if (result.status >= 200 && result.status < 300) {
                    return true;
                }

                logger.debug("[PloudStore] deleteCommands attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + result.status + ": " + truncate(result.body, 200));

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
                HttpResult result = get(url);

                if (result.status == 200) {
                    PinResponse parsed = gson.fromJson(result.body, PinResponse.class);
                    return parsed != null ? parsed : new PinResponse();
                }

                if (result.status == 403) {
                    logger.warning("[PloudStore] fetchPlayerPin: Forbidden (HTTP 403) — check your secret key.");
                    return null;
                }

                logger.debug("[PloudStore] fetchPlayerPin attempt " + attempt + "/" + maxRetries
                        + " — HTTP " + result.status + ": " + truncate(result.body, 200));

            } catch (Exception e) {
                logger.debug("[PloudStore] fetchPlayerPin attempt " + attempt + "/" + maxRetries
                        + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            if (attempt < maxRetries) sleepBackoff(attempt);
        }

        return null;
    }

    private HttpResult get(String urlStr) throws IOException {
        HttpURLConnection conn = openConnection(urlStr, "GET");
        try {
            conn.setRequestProperty("X-store-Key", secretKey);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            return new HttpResult(code, stream != null ? readStream(stream) : "");
        } finally {
            conn.disconnect();
        }
    }

    private HttpResult post(String urlStr, String bodyStr) throws IOException {
        HttpURLConnection conn = openConnection(urlStr, "POST");
        try {
            conn.setRequestProperty("X-store-Key", secretKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(bodyStr.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            return new HttpResult(code, stream != null ? readStream(stream) : "");
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection openConnection(String urlStr, String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestMethod(method);
        return conn;
    }

    private String readStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private static class HttpResult {
        final int status;
        final String body;
        HttpResult(int status, String body) {
            this.status = status;
            this.body = body;
        }
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
