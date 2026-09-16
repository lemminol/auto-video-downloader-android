package com.lemminol.avd;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class EndpointManager {
    public static final String PREFS = "connection";
    private final Context context;
    private final SharedPreferences prefs;

    private static final String PREF_MANUAL_URL_MIGRATED = "manual_url_v115";

    public EndpointManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateToManualUrlEntry();
    }

    private void migrateToManualUrlEntry() {
        // v1.1.5 removes every bundled/default LAN address. On the first launch
        // after updating, require the user to enter the server URL again so no
        // private endpoint is shipped or retained as an application default.
        if (!prefs.getBoolean(PREF_MANUAL_URL_MIGRATED, false)) {
            prefs.edit()
                    .remove("local")
                    .remove("last")
                    .putBoolean(PREF_MANUAL_URL_MIGRATED, true)
                    .apply();
        }
    }

    public boolean isAutoSwitch() { return prefs.getBoolean("auto", true); }
    public void setAutoSwitch(boolean value) { prefs.edit().putBoolean("auto", value).apply(); }
    public String getPreferredSsid() { return prefs.getString("ssid", ""); }
    public void setPreferredSsid(String value) { prefs.edit().putString("ssid", safe(value)).apply(); }
    public String getLocalEndpoint() { return prefs.getString("local", ""); }
    public void setLocalEndpoint(String value) { prefs.edit().putString("local", normalizeEndpoint(value, true)).apply(); }
    public boolean hasConfiguredEndpoints() { return !getLocalEndpoint().isEmpty() || !getExternalEndpoints().isEmpty(); }
    public String getLastEndpoint() { return prefs.getString("last", ""); }
    public void setLastEndpoint(String value) { prefs.edit().putString("last", safe(value)).apply(); }

    public List<String> getExternalEndpoints() {
        String packed = prefs.getString("external", "");
        List<String> out = new ArrayList<>();
        if (!TextUtils.isEmpty(packed)) {
            for (String line : packed.split("\\n")) if (!line.trim().isEmpty()) out.add(line.trim());
        }
        return out;
    }

    public void setExternalEndpoints(List<String> values) {
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String endpoint = normalizeEndpoint(value, false);
            if (!endpoint.isEmpty() && !normalized.contains(endpoint)) normalized.add(endpoint);
        }
        prefs.edit().putString("external", TextUtils.join("\n", normalized)).apply();
    }

    public String chooseEndpoint() {
        List<String> candidates = new ArrayList<>();
        String local = getLocalEndpoint();
        String ssid = currentSsid();
        boolean preferred = !getPreferredSsid().isEmpty() && getPreferredSsid().equals(ssid);
        if (isAutoSwitch() && preferred && !local.isEmpty()) candidates.add(local);
        if (!isAutoSwitch() && !getLastEndpoint().isEmpty()) candidates.add(getLastEndpoint());
        candidates.addAll(getExternalEndpoints());
        if (!local.isEmpty() && !candidates.contains(local)) candidates.add(local);
        if (!getLastEndpoint().isEmpty() && !candidates.contains(getLastEndpoint())) candidates.add(getLastEndpoint());
        for (String endpoint : candidates) {
            if (probe(endpoint)) {
                setLastEndpoint(endpoint);
                return endpoint;
            }
        }
        return "";
    }

    public boolean probe(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) return false;
        if (!isEndpointAllowed(endpoint)) return false;
        if (probePath(endpoint, "/api/health", true)) return true;
        return probePath(endpoint, "/healthz", false);
    }

    private boolean probePath(String endpoint, String path, boolean requireIdentity) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(trimSlash(endpoint) + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1800);
            conn.setReadTimeout(2200);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "AVD-Android/1.1.8");
            int status = conn.getResponseCode();
            if (status != 200) return false;
            if (!requireIdentity) return true;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null && body.length() < 8192) body.append(line);
                return body.toString().contains("\"app\": \"auto-video-downloader\"") || body.toString().contains("\"app\":\"auto-video-downloader\"");
            }
        } catch (Exception ignored) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public String currentSsid() {
        try {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return "";
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = cm.getActiveNetwork();
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && Build.VERSION.SDK_INT >= 31) {
                Object info = caps.getTransportInfo();
                if (info instanceof WifiInfo) return cleanSsid(((WifiInfo) info).getSSID());
            }
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wm.getConnectionInfo();
            return cleanSsid(info == null ? "" : info.getSSID());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String cleanSsid(String value) {
        if (value == null || "<unknown ssid>".equalsIgnoreCase(value)) return "";
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) return value.substring(1, value.length() - 1);
        return value;
    }

    /**
     * Resolves a user-entered server address. When the scheme is omitted, HTTPS is
     * tested first and HTTP is tested as a fallback for private/LAN hosts.
     */
    public String resolveUserEndpoint(String raw) {
        List<String> candidates = userEndpointCandidates(raw);
        for (String endpoint : candidates) {
            if (probe(endpoint)) return endpoint;
        }
        return "";
    }

    public static List<String> userEndpointCandidates(String raw) {
        List<String> out = new ArrayList<>();
        String value = safe(raw).trim();
        if (value.isEmpty()) return out;

        if (value.matches("^[A-Za-z][A-Za-z0-9+.-]*://.*$")) {
            String normalized = normalizeEndpoint(value, true);
            if (!normalized.isEmpty()) out.add(normalized);
            return out;
        }

        String https = normalizeEndpoint("https://" + value, true);
        if (!https.isEmpty()) out.add(https);

        String http = normalizeEndpoint("http://" + value, true);
        if (!http.isEmpty() && isPrivateHost(URI.create(http).getHost()) && !out.contains(http)) out.add(http);
        return out;
    }

    public static String normalizeEndpoint(String raw, boolean localAllowed) {
        String value = safe(raw).trim();
        if (value.isEmpty()) return "";
        if (!value.matches("^[A-Za-z][A-Za-z0-9+.-]*://.*$")) value = "https://" + value;
        try {
            URI uri = URI.create(value);
            String scheme = safe(uri.getScheme()).toLowerCase();
            String host = safe(uri.getHost()).toLowerCase();
            if (host.isEmpty() || !(scheme.equals("http") || scheme.equals("https"))) return "";
            if (uri.getRawUserInfo() != null) return "";
            int port = uri.getPort();
            if (port == 0 || port > 65535) return "";
            String authority = host + (port > 0 ? ":" + port : "");
            return scheme + "://" + authority;
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isEndpointAllowed(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String scheme = safe(uri.getScheme()).toLowerCase();
            String host = safe(uri.getHost()).toLowerCase();
            if (scheme.equals("https")) return !host.isEmpty();
            return scheme.equals("http") && !host.isEmpty() && !normalizeEndpoint(endpoint, true).isEmpty();
        } catch (Exception e) { return false; }
    }

    public static boolean isPrivateHost(String host) {
        if (host == null) return false;
        host = host.toLowerCase();
        if (host.equals("localhost") || host.endsWith(".local")) return true;
        if (host.startsWith("10.") || host.startsWith("192.168.")) return true;
        if (host.startsWith("127.")) return true;
        if (host.startsWith("172.")) {
            String[] p = host.split("\\.");
            if (p.length == 4) try { int second = Integer.parseInt(p[1]); return second >= 16 && second <= 31; } catch (Exception ignored) {}
        }
        return host.equals("::1") || host.startsWith("fc") || host.startsWith("fd");
    }

    public static String trimSlash(String value) {
        if (value == null) return "";
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
