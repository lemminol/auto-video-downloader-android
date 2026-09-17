package com.lemminol.avd;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQ_CONNECTION = 1001;
    private static final int REQ_FILE = 1002;
    private static final int REQ_WIFI_PERMISSION = 1003;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private android.app.AlertDialog exitDialog;
    private android.window.OnBackInvokedCallback backCallback;
    private EndpointManager endpoints;
    private WebView web;
    private TextView endpointText;
    private ProgressBar progress;
    private String activeEndpoint = "";
    private String pendingShare = "";
    private ValueCallback<Uri[]> fileCallback;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Runnable networkReconnect = () -> selectEndpoint(true);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        endpoints = new EndpointManager(this);
        buildUi();
        configureWebView();
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            backCallback = this::navigateBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
        captureIncoming(getIntent());
        requestWifiPermissionIfNeeded();
        registerNetworkCallback();
        if (endpoints.hasConfiguredEndpoints()) {
            selectEndpoint(false);
        } else {
            progress.setVisibility(View.GONE);
            endpointText.setText("서버 주소를 입력하세요");
            endpointText.setTextColor(Color.rgb(80,80,90));
            main.post(() -> startActivityForResult(new Intent(this, ConnectionActivity.class), REQ_CONNECTION));
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248,250,252));
        getWindow().setStatusBarColor(Color.rgb(248,250,252));
        getWindow().setNavigationBarColor(Color.rgb(248,250,252));
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        applySystemInsets(root, 0, 0, 0, 0);
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.rgb(248,250,252));
        bar.setPadding(dp(14), dp(5), dp(8), dp(5));
        endpointText = new TextView(this);
        endpointText.setText("서버 연결 확인 중…");
        endpointText.setTextColor(Color.rgb(54,54,61));
        endpointText.setTextSize(12);
        endpointText.setSingleLine(true);
        endpointText.setVisibility(View.GONE);
        View spacer = new View(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        android.widget.ImageButton reconnect = iconButton(R.drawable.ic_refresh, "재연결");
        reconnect.setOnClickListener(v -> forceEndpointSelection());
        bar.addView(reconnect);
        android.widget.ImageButton settingsButton = iconButton(R.drawable.ic_settings, "서버 설정");
        settingsButton.setOnClickListener(v -> startActivityForResult(new Intent(this, ConnectionActivity.class), REQ_CONNECTION));
        bar.addView(settingsButton);
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private android.widget.ImageButton iconButton(int drawable, String description) {
        android.widget.ImageButton b = new android.widget.ImageButton(this);
        b.setImageResource(drawable);
        b.setImageTintList(android.content.res.ColorStateList.valueOf(Color.rgb(99,102,241)));
        b.setContentDescription(description); b.setTooltipText(description);
        b.setPadding(dp(10),dp(10),dp(10),dp(10));
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(Color.rgb(238,242,255));shape.setCornerRadius(dp(12));
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x226366F1),shape,null));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44),dp(44));
        lp.setMargins(dp(8),0,0,0);b.setLayoutParams(lp);
        return b;
    }

    private Button smallButton(String text) {
        Button b = new Button(this); styleButton(b);
        b.setText(text);
        b.setTextSize(11);
        b.setMinHeight(0); b.setMinWidth(0);
        b.setPadding(dp(9), 0, dp(9), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        lp.setMargins(dp(5), 0, 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void configureWebView() {
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, true);
        cookies.flush();
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setSupportMultipleWindows(true);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        web.getSettings().setBuiltInZoomControls(false);
        web.getSettings().setDisplayZoomControls(false);
        web.setWebViewClient(new AppWebViewClient());
        web.setWebChromeClient(new AppChromeClient());
        web.setDownloadListener(downloadListener);
    }

    private final DownloadListener downloadListener = (url, userAgent, contentDisposition, mimetype, contentLength) -> {
        try {
            if (!belongsToActiveEndpoint(url)) { openExternal(Uri.parse(url)); return; }
            String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(filename);
            request.setMimeType(mimetype);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "다운로드를 시작했습니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "다운로드 시작 실패: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    };

    private class AppWebViewClient extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleNavigation(request.getUrl());
        }
        @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return handleNavigation(Uri.parse(url)); }
        @Override public void onPageFinished(WebView view, String url) {
            // WebView stores Set-Cookie asynchronously.  Flush after every page so a
            // successful /api/auth/login session is committed before navigation,
            // process suspension or an endpoint health check can occur.
            CookieManager.getInstance().flush();
            progress.setVisibility(View.GONE);
        }
        @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
            if (request != null && request.isForMainFrame() && response != null && response.getStatusCode() == 401) {
                CookieManager.getInstance().flush();
            }
        }
        @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) {
            handler.cancel();
            Toast.makeText(MainActivity.this, "HTTPS 인증서를 확인할 수 없어 연결을 차단했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean handleNavigation(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (scheme.equals("intent")) { launchIntentUri(uri.toString()); return true; }
        if (scheme.equals("http") || scheme.equals("https")) {
            if (belongsToActiveEndpoint(uri.toString())) return false;
            openExternal(uri); return true;
        }
        openExternal(uri); return true;
    }

    private class AppChromeClient extends WebChromeClient {
        @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            try { startActivityForResult(params.createIntent(), REQ_FILE); }
            catch (ActivityNotFoundException e) { fileCallback = null; return false; }
            return true;
        }
        @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
            WebView temp = new WebView(MainActivity.this);
            temp.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) { openExternal(request.getUrl()); return true; }
                @Override public boolean shouldOverrideUrlLoading(WebView v, String url) { openExternal(Uri.parse(url)); return true; }
            });
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(temp); resultMsg.sendToTarget(); return true;
        }
    }

    private void captureIncoming(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(intent.getType())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            pendingShare = firstHttpUrl(text);
        } else if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            Uri data = intent.getData();
            if ("avd".equalsIgnoreCase(data.getScheme()) && "share".equalsIgnoreCase(data.getHost())) {
                String value = data.getQueryParameter("url");
                if (value != null) pendingShare = value;
            }
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); captureIncoming(intent);
        if (!activeEndpoint.isEmpty() && !pendingShare.isEmpty()) loadShared();
    }

    private String firstHttpUrl(String text) {
        if (text == null) return "";
        for (String token : text.split("\\s+")) if (token.startsWith("https://") || token.startsWith("http://")) return token.trim();
        return "";
    }

    private void forceEndpointSelection() {
        // A manual reconnect is an explicit request to re-evaluate LAN/VPN/external
        // priorities, even if the current endpoint is still reachable.
        activeEndpoint = "";
        selectEndpoint(true);
    }

    private void selectEndpoint(boolean preservePath) {
        main.removeCallbacks(networkReconnect);
        progress.setVisibility(View.VISIBLE);
        endpointText.setText("서버 연결 확인 중…");
        String relative = preservePath ? currentRelativePath() : "/jobs";
        io.execute(() -> {
            String endpoint = "";
            // Do not jump between the external hostname and the LAN IP just because
            // Wi-Fi/VPN capabilities changed while the user is logged in. Cookies
            // are host-scoped, so that would look like an unexpected logout.
            // Keep the current endpoint while it is healthy; the explicit
            // "재연결" button still re-evaluates endpoints via forceEndpointSelection().
            if (preservePath && !activeEndpoint.isEmpty() && endpoints.probe(activeEndpoint)) {
                endpoint = activeEndpoint;
            }
            if (endpoint.isEmpty()) endpoint = endpoints.chooseEndpoint();
            final String selectedEndpoint = endpoint;
            main.post(() -> {
                if (isFinishing()) return;
                progress.setVisibility(View.GONE);
                if (selectedEndpoint.isEmpty()) { showNoServer(); return; }
                boolean changed = !selectedEndpoint.equals(activeEndpoint);
                activeEndpoint = selectedEndpoint;
                endpointText.setText("● " + selectedEndpoint);
                endpointText.setTextColor(Color.rgb(42,130,68));
                if (!pendingShare.isEmpty()) loadShared();
                else if (changed || web.getUrl() == null) web.loadUrl(selectedEndpoint + relative);
            });
        });
    }

    private void loadShared() {
        if (activeEndpoint.isEmpty() || pendingShare.isEmpty()) return;
        String share = pendingShare; pendingShare = "";
        String encoded;
        try { encoded = URLEncoder.encode(share, "UTF-8"); } catch (Exception e) { encoded = share; }
        web.loadUrl(activeEndpoint + "/jobs?share=" + encoded);
    }

    private String currentRelativePath() {
        try {
            String current = web.getUrl();
            if (current == null || activeEndpoint.isEmpty() || !current.startsWith(activeEndpoint)) return "/jobs";
            String relative = current.substring(activeEndpoint.length());
            return relative.startsWith("/") ? relative : "/jobs";
        } catch (Exception e) { return "/jobs"; }
    }

    private void showNoServer() {
        activeEndpoint = "";
        endpointText.setText("● 연결 가능한 서버 없음");
        endpointText.setTextColor(Color.rgb(180,45,45));
        String html = "<html><meta name='viewport' content='width=device-width,initial-scale=1'><body style='font-family:sans-serif;background:#101418;color:#eee;padding:28px'><h2>서버에 연결할 수 없습니다</h2><p>연결 설정에서 서버 주소를 직접 입력하세요. http:// 또는 https://는 생략할 수 있습니다.</p><button style='padding:12px 18px' onclick=\"location.href='avd://settings'\">서버 주소 입력</button></body></html>";
        web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private boolean belongsToActiveEndpoint(String url) {
        if (activeEndpoint.isEmpty()) return false;
        try {
            URI a = URI.create(activeEndpoint), b = URI.create(url);
            int pa = a.getPort() == -1 ? ("https".equals(a.getScheme()) ? 443 : 80) : a.getPort();
            int pb = b.getPort() == -1 ? ("https".equals(b.getScheme()) ? 443 : 80) : b.getPort();
            return a.getScheme().equalsIgnoreCase(b.getScheme()) && a.getHost().equalsIgnoreCase(b.getHost()) && pa == pb;
        } catch (Exception e) { return false; }
    }

    private boolean belongsToConfiguredEndpoint(String url) {
        if (EndpointManager.trimSlash(endpoints.getLocalEndpoint()).length() > 0 && url.startsWith(EndpointManager.trimSlash(endpoints.getLocalEndpoint()))) return true;
        for (String e : endpoints.getExternalEndpoints()) if (url.startsWith(EndpointManager.trimSlash(e))) return true;
        return false;
    }

    private void launchIntentUri(String value) {
        try {
            Intent intent = Intent.parseUri(value, Intent.URI_INTENT_SCHEME);
            if (intent.resolveActivity(getPackageManager()) != null) { startActivity(intent); return; }
            String fallback = intent.getStringExtra("browser_fallback_url");
            if (fallback != null) openExternal(Uri.parse(fallback));
        } catch (Exception e) { Toast.makeText(this, "외부 플레이어를 열 수 없습니다.", Toast.LENGTH_SHORT).show(); }
    }

    private void openExternal(Uri uri) {
        if ("avd".equalsIgnoreCase(uri.getScheme()) && "settings".equalsIgnoreCase(uri.getHost())) {
            startActivityForResult(new Intent(this, ConnectionActivity.class), REQ_CONNECTION); return;
        }
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (Exception e) { Toast.makeText(this, "이 주소를 열 앱이 없습니다.", Toast.LENGTH_SHORT).show(); }
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) { scheduleReconnect(); }
            @Override public void onLost(Network network) { scheduleReconnect(); }
            @Override public void onCapabilitiesChanged(Network network, android.net.NetworkCapabilities caps) { scheduleReconnect(); }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }
    private void scheduleReconnect() { main.removeCallbacks(networkReconnect); main.postDelayed(networkReconnect, 1200); }

    private void requestWifiPermissionIfNeeded() {
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), REQ_WIFI_PERMISSION);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CONNECTION && resultCode == RESULT_OK) forceEndpointSelection();
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null ? new Uri[]{data.getData()} : null;
                fileCallback.onReceiveValue(result); fileCallback = null;
            }
        }
    }

    // Android 12 and earlier use this entry point; newer versions use the dispatcher.
    @Override public void onBackPressed() {
        navigateBack();
    }

    private void navigateBack() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            if (isFinishing() || isDestroyed() || (exitDialog != null && exitDialog.isShowing())) return;
            exitDialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("앱 종료")
                    .setMessage("앱을 종료할까요?")
                    .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("종료", (dialog, which) -> {
                        CookieManager.getInstance().flush();
                        finishAndRemoveTask();
                    })
                    .create();
            exitDialog.setOnDismissListener(dialog -> exitDialog = null);
            exitDialog.show();
        }
    }

    @Override protected void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override protected void onStop() {
        CookieManager.getInstance().flush();
        super.onStop();
    }

    private void applySystemInsets(View view, int baseLeft, int baseTop, int baseRight, int baseBottom) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                // Insets are delivered to an attached view. Avoid querying PhoneWindow
                // before setContentView(), when its DecorView may still be null.
                android.view.WindowInsetsController controller = v.getWindowInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(baseLeft + bars.left, baseTop + bars.top, baseRight + bars.right, baseBottom + bars.bottom);
            } else {
                // Android 10 and older are not forced edge-to-edge here.
                v.setPadding(baseLeft, baseTop, baseRight, baseBottom);
            }
            return android.os.Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets;
        });
        view.requestApplyInsets();
    }

    @Override protected void onDestroy() {
        if (exitDialog != null) {
            exitDialog.setOnDismissListener(null);
            exitDialog.dismiss();
            exitDialog = null;
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
            backCallback = null;
        }
        try { ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(networkCallback); } catch (Exception ignored) {}
        io.shutdownNow();
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }

    private void styleButton(Button button) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(Color.rgb(99,102,241)); shape.setCornerRadius(dp(12));
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF), shape, null));
        button.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xFFCBD5E1,Color.WHITE}));
        button.setAllCaps(false);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}


