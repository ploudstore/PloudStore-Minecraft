package org.ploudstore.ploudStorePlugin.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;
import org.ploudstore.ploudStorePlugin.PluginLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class UpdateChecker {

    private static final String GITHUB_API =
            "https://api.github.com/repos/ploudstore/PloudStore-Minecraft/releases/latest";

    private final JavaPlugin plugin;
    private final PluginLogger logger;
    private volatile boolean updateAvailable = false;
    private volatile String latestVersion = null;

    public UpdateChecker(JavaPlugin plugin, PluginLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void checkAsync() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                check();
            }
        }, "PloudStore-UpdateChecker");
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
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String tag = json.get("tag_name").getAsString().replaceFirst("^v", "");
            String current = plugin.getDescription().getVersion();

            if (compareVersions(tag, current) <= 0) {
                logger.debug("[PloudStore] Plugin atualizado (v" + current + ").");
                return;
            }

            String downloadUrl = findJarAsset(json.getAsJsonArray("assets"));
            if (downloadUrl == null) {
                logger.warning("[PloudStore] Update v" + tag + " disponível mas sem JAR para download.");
                return;
            }

            downloadJar(downloadUrl, current, tag);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warning("[PloudStore] Erro ao verificar update: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
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

    private void downloadJar(String url, String current, String newVersion) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "PloudStore-Minecraft");
        conn.setInstanceFollowRedirects(true);

        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warning("[PloudStore] Download falhou — HTTP " + code);
                return;
            }

            Path currentJar = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            Path updateFolder = plugin.getServer().getUpdateFolderFile().toPath();
            Files.createDirectories(updateFolder);
            Path dest = updateFolder.resolve(currentJar.getFileName());

            InputStream in = conn.getInputStream();
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            in.close();

            latestVersion = newVersion;
            updateAvailable = true;

            logger.info("[PloudStore] Update v" + current + " → v" + newVersion
                    + " descarregado. Reinicia o servidor para aplicar.");
        } finally {
            conn.disconnect();
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

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
