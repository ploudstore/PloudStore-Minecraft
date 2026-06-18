package org.ploudstore.ploudStorePlugin.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ploudstore.ploudStorePlugin.PluginLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {

    private static final String GITHUB_API =
            "https://api.github.com/repos/ploudstore/PloudStore-Minecraft/releases";

    private final String currentVersion;
    private final PluginLogger logger;
    private volatile boolean updateAvailable = false;
    private volatile String latestVersion = null;

    public UpdateChecker(String currentVersion, PluginLogger logger) {
        this.currentVersion = currentVersion;
        this.logger = logger;
    }

    public void checkAsync() {
        Thread t = new Thread(this::check, "PloudStore-UpdateChecker");
        t.setDaemon(true);
        t.start();
    }

    private void check() {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(GITHUB_API).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "PloudStore-Minecraft");

            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warning("[PloudStore] Update check failed — HTTP " + code);
                return;
            }

            String body = readStream(conn.getInputStream());
            JsonArray releases = JsonParser.parseString(body).getAsJsonArray();
            if (releases.size() == 0) {
                logger.debug("[PloudStore] Nenhuma release encontrada no GitHub.");
                return;
            }

            JsonObject latest = releases.get(0).getAsJsonObject();
            String tag = latest.get("tag_name").getAsString().replaceFirst("^v", "");

            if (compareVersions(tag, currentVersion) <= 0) {
                logger.debug("[PloudStore] Plugin atualizado (v" + currentVersion + ").");
                return;
            }

            latestVersion = tag;
            updateAvailable = true;

            logger.warning("[PloudStore] Update available! v" + currentVersion + " -> v" + tag
                    + ". Download the latest version from your PloudStore dashboard.");

        } catch (Exception e) {
            logger.warning("[PloudStore] Erro ao verificar update: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String readStream(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int na = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int nb = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (na != nb) return Integer.compare(na, nb);
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isUpdateAvailable() { return updateAvailable; }
    public String getLatestVersion()   { return latestVersion; }
}
