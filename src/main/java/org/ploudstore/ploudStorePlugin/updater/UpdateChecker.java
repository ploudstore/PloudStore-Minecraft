package org.ploudstore.ploudStorePlugin.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class UpdateChecker {

    private static final String GITHUB_API =
            "https://api.github.com/repos/ploudstore/PloudStore-Minecraft/releases/latest";

    private final JavaPlugin plugin;
    private volatile boolean updateAvailable = false;
    private volatile String latestVersion = null;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAsync() {
        Thread t = new Thread(this::check, "PloudStore-UpdateChecker");
        t.setDaemon(true);
        t.start();
    }

    private void check() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "PloudStore-Minecraft")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                plugin.getLogger().warning("[PloudStore] Update check failed — HTTP " + response.statusCode());
                return;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String tag = json.get("tag_name").getAsString().replaceFirst("^v", "");
            String current = plugin.getDescription().getVersion();

            if (compareVersions(tag, current) <= 0) {
                plugin.getLogger().info("[PloudStore] Plugin atualizado (v" + current + ").");
                return;
            }

            String downloadUrl = findJarAsset(json.getAsJsonArray("assets"));
            if (downloadUrl == null) {
                plugin.getLogger().warning("[PloudStore] Update v" + tag + " disponível mas sem JAR para download.");
                return;
            }

            downloadJar(client, downloadUrl, current, tag);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            plugin.getLogger().warning("[PloudStore] Erro ao verificar update: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String findJarAsset(JsonArray assets) {
        for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.get(i).getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (name.endsWith(".jar") && !name.contains("-sources") && !name.contains("-javadoc")) {
                return asset.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    private void downloadJar(HttpClient client, String url, String current, String newVersion) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "PloudStore-Minecraft")
                .GET()
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            plugin.getLogger().warning("[PloudStore] Download falhou — HTTP " + response.statusCode());
            return;
        }

        Path currentJar = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        Path updateFolder = plugin.getServer().getUpdateFolderFile().toPath();
        Files.createDirectories(updateFolder);
        Path dest = updateFolder.resolve(currentJar.getFileName());

        try (InputStream in = response.body()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        latestVersion = newVersion;
        updateAvailable = true;

        plugin.getLogger().info("[PloudStore] Update v" + current + " → v" + newVersion
                + " descarregado. Reinicia o servidor para aplicar.");
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

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
