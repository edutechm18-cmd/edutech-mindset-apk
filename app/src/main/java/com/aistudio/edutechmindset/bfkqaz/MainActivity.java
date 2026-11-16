package com.aistudio.edutechmindset.bfkqaz;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://edutech-mindset.pages.dev";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private WebView webView;
    private LinearLayout splashLayout;
    private FrameLayout rootContainer;
    private WebView popupWebView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableFullscreen();

         rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        webView.setLayoutParams(webParams);
        setupWebView();

        LinearLayout bottomBar = createBottomNavigationBar();

        mainLayout.addView(webView);
        mainLayout.addView(bottomBar);

        splashLayout = createSplashScreen();

        rootContainer.addView(mainLayout);
        rootContainer.addView(splashLayout);
        setContentView(rootContainer);

        requestAppPermissions();

        String initialUrl = HOME_URL;
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            initialUrl = intent.getData().toString();
        }
        webView.loadUrl(initialUrl);

        new Handler(Looper.getMainLooper()).postDelayed(this::hideSplashScreen, 2500);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void enableFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableFullscreen();
        }
    }

    private LinearLayout createSplashScreen() {
        LinearLayout splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(Color.parseColor("#0F172A"));
        splash.setPadding(48, 48, 48, 48);
        splash.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView titleView = new TextView(this);
        titleView.setText("EduTech Mindset");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(28);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);

        TextView subtitleView = new TextView(this);
        subtitleView.setText("EduTech Mindset - عقلية التعليم والتكنولوجيا - إدارة الأستاذ باسل أشرف الفقعاوي");
        subtitleView.setTextColor(Color.parseColor("#38BDF8"));
        subtitleView.setTextSize(16);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setPadding(0, 24, 0, 36);

        ProgressBar progressBar = new ProgressBar(this);

        splash.addView(titleView);
        splash.addView(subtitleView);
        splash.addView(progressBar);
        return splash;
    }

    private void hideSplashScreen() {
        if (splashLayout != null && splashLayout.getVisibility() == View.VISIBLE) {
            splashLayout.setVisibility(View.GONE);
        }
    }

    private LinearLayout createBottomNavigationBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Color.parseColor("#1E293B"));
        bar.setGravity(Gravity.CENTER);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        bar.setPadding(8, 8, 8, 8);

        Button btnBack = createNavButton("◀ رجوع", v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        Button btnHome = createNavButton("🏠 الرئيسية", v -> webView.loadUrl(HOME_URL));
        Button btnRefresh = createNavButton("🔄 تحديث", v -> webView.reload());
        Button btnForward = createNavButton("تقدم ▶", v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });

        bar.addView(btnBack);
        bar.addView(btnHome);
        bar.addView(btnRefresh);
        bar.addView(btnForward);
        return bar;
    }

    private Button createNavButton(String label, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setTextSize(13);
        btn.setAllCaps(false);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btn.setOnClickListener(listener);
        return btn;
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        String ua = settings.getUserAgentString();
        if (ua != null) settings.setUserAgentString(ua.replace("; wv", ""));
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        webView.addJavascriptInterface(new AndroidSessionBridge(this), "AndroidSessionBridge");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
                  
                      }
                  @Override      
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectSessionStorageBridge(view);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                injectSessionStorageBridge(view);
            }
            

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectSessionStorageBridge(view);
                hideSplashScreen();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView childView = new WebView(MainActivity.this);
                WebSettings childSettings = childView.getSettings();
                childSettings.setJavaScriptEnabled(true);
                childSettings.setDomStorageEnabled(true);
                childSettings.setSupportMultipleWindows(true);
                childSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                String ua = childSettings.getUserAgentString();
                if (ua != null) childSettings.setUserAgentString(ua.replace("; wv", ""));
                CookieManager.getInstance().setAcceptCookie(true);
                CookieManager.getInstance().setAcceptThirdPartyCookies(childView, true);
                childView.addJavascriptInterface(new AndroidSessionBridge(MainActivity.this), "AndroidSessionBridge");
                childView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView v, String url, Bitmap favicon) {
                        super.onPageStarted(v, url, favicon);
                        injectSessionStorageBridge(v);
                    }
                    @Override
                    public void onLoadResource(WebView v, String url) {
                        super.onLoadResource(v, url);
                        injectSessionStorageBridge(v);
                    }
                    @Override
                    public void onPageFinished(WebView v, String url) {
                        super.onPageFinished(v, url);
                        injectSessionStorageBridge(v);
                    }
                });
                childView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        closePopupWebView(window);
                    }
                });
                popupWebView = childView;
                rootContainer.addView(childView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(childView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                closePopupWebView(window);
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading file...");
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    request.setTitle(fileName);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(MainActivity.this, "جاري تنزيل الملف: " + fileName, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "تعذر تنزيل الملف", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void closePopupWebView(WebView window) {
        if (window != null) {
            if (window.getParent() instanceof ViewGroup) {
                ((ViewGroup) window.getParent()).removeView(window);
            }
            if (window == popupWebView) {
                popupWebView = null;
            }
            window.destroy();
        }
    }

    private void injectSessionStorageBridge(WebView view) {
        if (view == null) return;
        String js = "(function() {" +
            "try {" +
            "if (window.AndroidSessionBridge) {" +
            "var saved = JSON.parse(window.AndroidSessionBridge.getAllItemsJson() || '{}');" +
            "for (var k in saved) {" +
            "if (Object.prototype.hasOwnProperty.call(saved, k)) {" +
            "try { window.sessionStorage.setItem(k, saved[k]); } catch(e) {}" +
            "}" +
            "}" +
            "if (!window.__sessionBridgeHooked) {" +
            "window.__sessionBridgeHooked = true;" +
            "var origSetItem = Storage.prototype.setItem;" +
            "Storage.prototype.setItem = function(key, val) {" +
            "origSetItem.apply(this, arguments);" +
            "if (this === window.sessionStorage && window.AndroidSessionBridge) {" +
            "try { window.AndroidSessionBridge.setItem(String(key), String(val)); } catch(e) {}" +
            "}" +
            "};" +
            "var origRemoveItem = Storage.prototype.removeItem;" +
            "Storage.prototype.removeItem = function(key) {" +
            "origRemoveItem.apply(this, arguments);" +
            "if (this === window.sessionStorage && window.AndroidSessionBridge) {" +
            "try { window.AndroidSessionBridge.removeItem(String(key)); } catch(e) {}" +
            "}" +
            "};" +
            "var origClear = Storage.prototype.clear;" +
            "Storage.prototype.clear = function() {" +
            "origClear.apply(this);" +
            "if (this === window.sessionStorage && window.AndroidSessionBridge) {" +
            "try { window.AndroidSessionBridge.clear(); } catch(e) {}" +
            "}" +
            "};" +
            "}" +
            "}" +
            "} catch(err) {}" +
            "})();";
        view.evaluateJavascript(js, null);
    }

    private static class AndroidSessionBridge {
        private final SharedPreferences prefs;

        AndroidSessionBridge(Context context) {
            prefs = context.getSharedPreferences("web_session_storage", Context.MODE_PRIVATE);
        }

        @JavascriptInterface
        public void setItem(String key, String value) {
            if (key != null) {
                prefs.edit().putString(key, value != null ? value : "").apply();
            }
        }

        @JavascriptInterface
        public void removeItem(String key) {
            if (key != null) {
                prefs.edit().remove(key).apply();
            }
        }

        @JavascriptInterface
        public void clear() {
            prefs.edit().clear().apply();
        }

        @JavascriptInterface
        public String getAllItemsJson() {
            try {
                JSONObject obj = new JSONObject(prefs.getAll());
                return obj.toString();
            } catch (Exception e) {
                return "{}";
            }
        }
    }
    
    private void requestAppPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        List<String> toRequest = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(perm);
            }
        }
        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }
}
