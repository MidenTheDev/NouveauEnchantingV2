package us.to.midensthings.nouveauEnchanting.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import us.to.midensthings.nouveauEnchanting.NouveauEnchanting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

public class UpdateTracker {
    private static final String PROJECT_SLUG = "nouveau-enchanting";
    private static final String VERSIONS_URL = "https://api.modrinth.com/v2/project/" + PROJECT_SLUG + "/version?loaders=%5B%22paper%22%5D";

    private final NouveauEnchanting plugin = NouveauEnchanting.getPlugin(NouveauEnchanting.class);
    private final Logger logger = plugin.getLogger();
    private final String currentVersion = plugin.getPluginMeta().getVersion();

    private volatile String latestVersion;

    public void checkForUpdates() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(VERSIONS_URL))
                .header("User-Agent", "MidenTheDev/NouveauEnchantingV2/" + currentVersion)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        // off main thread
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        logger.warning("Update check failed: Modrinth returned HTTP " + response.statusCode());
                        return;
                    }
                    latestVersion = findLatestRelease(JsonParser.parseString(response.body()).getAsJsonArray());
                    if (latestVersion == null) return;

                    if (isNewer(latestVersion, currentVersion)) {
                        logger.warning("A new version of NouveauEnchanting is available: " + latestVersion + " (you are running " + currentVersion + ")");
                        logger.warning("Download it at https://modrinth.com/plugin/" + PROJECT_SLUG + "/versions");
                    } else {
                        logger.info("NouveauEnchanting is up to date");
                    }
                })
                .exceptionally(e -> {
                    logger.warning("Update check failed: " + e.getMessage());
                    return null;
                });
    }

    // Modrinth returns versions newest first, so the first release is the latest
    private String findLatestRelease(JsonArray versions) {
        for (JsonElement element : versions) {
            JsonObject version = element.getAsJsonObject();
            if (version.get("version_type").getAsString().equals("release")) {
                return version.get("version_number").getAsString();
            }
        }
        return null;
    }

    // ignore suffixes, check only numeric parts
    private boolean isNewer(String latest, String current) {
        String[] latestParts = latest.split("[^0-9]+");
        String[] currentParts = current.split("[^0-9]+");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int l = i < latestParts.length ? parse(latestParts[i]) : 0;
            int c = i < currentParts.length ? parse(currentParts[i]) : 0;
            if (l != c) return l > c;
        }
        return false;
    }

    private int parse(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isUpdateAvailable() {
        return latestVersion != null && isNewer(latestVersion, currentVersion);
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
