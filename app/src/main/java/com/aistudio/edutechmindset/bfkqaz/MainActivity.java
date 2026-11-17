package com.aistudio.edutechmindset.bfkqaz;
import android.annotation.SuppressLint; import android.Manifest; import android.animation.ValueAnimator; import android.animation.ObjectAnimator; import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity; import androidx.core.graphics.Insets; import androidx.core.view.ViewCompat; import androidx.core.view.WindowCompat; import androidx.core.view.WindowInsetsCompat; import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://edutech-mindset.pages.dev";
    private static final String PREFS_SESSION = "edutech_session_prefs";
    private static final String KEY_SESSION_JSON = "session_storage_json";

    private static final int REQ_PERMISSIONS = 1001;
    private static final int REQ_FILE_CHOOSER = 1002;
    private static final int REQ_IMPORT_VAULT = 1003;
    private static final int REQ_VOICE_INPUT = 1004;
    private static final int REQ_OVERLAY_PERMISSION = 1005;

    private FrameLayout rootLayout; private LinearLayout mainColumn; private LinearLayout audioPlayerBar; private LinearLayout bottomControlBar;
    private WebView webView;
    private FrameLayout popupContainer;
    private WebView popupWebView;
    private IntroSplashView splashView;
private Uri cameraCaptureUri;
    private PermissionRequest pendingWebPermissionRequest;
    private ValueCallback<Uri[]> fileUploadCallback;
    
    private SharedPreferences sessionPrefs;
    private File vaultDir;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionPrefs = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE);
        vaultDir = new File(getFilesDir(), "EduTechVault");
        if (!vaultDir.exists()) {
            vaultDir.mkdirs();
        }

        setupCleanSystemBars(); rootLayout = new FrameLayout(this); mainColumn = new LinearLayout(this); mainColumn.setOrientation(LinearLayout.VERTICAL); mainColumn.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); rootLayout.addView(mainColumn); ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> { Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(insets.left, insets.top, insets.right, insets.bottom); return WindowInsetsCompat.CONSUMED; });
        rootLayout.setBackgroundColor(Color.parseColor("#0F172A"));
        setContentView(rootLayout);

        setupWebView();
        setupBottomActionBar();
        setupPopupContainer();
        setupIntroMotionGraphics();

        requestAppPermissions();
        webView.loadUrl(HOME_URL);

        handleIncomingIntent(getIntent()); if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) { try { Intent overlaySvc = new Intent(this, OverlayAssistantService.class); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(overlaySvc); else startService(overlaySvc); } catch (Exception ignored) {} }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getBooleanExtra("OPEN_VOICE_ASSISTANT", false)) {
            mainHandler.postDelayed(this::startVoiceRecognition, 600);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            String url = intent.getData().toString();
            if (url.contains("edutech-mindset.pages.dev") && webView != null) {
                webView.loadUrl(url);
            }
        }
    }

    private void setupCleanSystemBars() { WindowCompat.setDecorFitsSystemWindows(getWindow(), false); getWindow().setStatusBarColor(Color.parseColor("#0B1120")); getWindow().setNavigationBarColor(Color.parseColor("#0F172A")); WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()); if (c != null) { c.setAppearanceLightStatusBars(false); c.setAppearanceLightNavigationBars(false); } } @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = new WebView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
         
        webView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        configureWebSettings(webView.getSettings());
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new AndroidSessionBridge(), "AndroidSessionBridge");
        webView.addJavascriptInterface(new AndroidVaultBridge(), "AndroidVaultBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectSessionAndPrintHooks(view);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (url != null && (url.contains("accounts.google.com") || url.contains("firebase") || url.contains("edutech-mindset"))) {
                    injectSessionAndPrintHooks(view);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectSessionAndPrintHooks(view);
                CookieManager.getInstance().flush();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return handleUrlNavigation(uri);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                popupWebView = new WebView(MainActivity.this);
                configureWebSettings(popupWebView.getSettings());
                CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, true);
                popupWebView.addJavascriptInterface(new AndroidSessionBridge(), "AndroidSessionBridge");
                popupWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView v, String url) {
                        injectSessionAndPrintHooks(v);
                        CookieManager.getInstance().flush();
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        return handleUrlNavigation(request.getUrl());
                    }
                });
                popupWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        closePopupWebView();
                    }
                });

                popupContainer.removeAllViews();
                popupContainer.addView(popupWebView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                popupContainer.setVisibility(View.VISIBLE);

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                closePopupWebView();
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> { boolean camOk = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED; boolean micOk = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; if (camOk && micOk) { request.grant(request.getResources()); } else { pendingWebPermissionRequest = request; ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO}, 105); } });
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;
                try {
                    Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT); contentIntent.addCategory(Intent.CATEGORY_OPENABLE); contentIntent.setType("*/*"); contentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); if (fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) { contentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); } Intent takePictureIntent = null; cameraCaptureUri = null; if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { try { java.io.File photoFile = java.io.File.createTempFile("cam_" + System.currentTimeMillis(), ".jpg", getCacheDir()); cameraCaptureUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", photoFile); takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraCaptureUri); takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) { cameraCaptureUri = null; takePictureIntent = null; } } Intent intent = Intent.createChooser(contentIntent, "اختر ملفاً أو التقط صورة بالكاميرا"); if (takePictureIntent != null) { intent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent}); }
                    startActivityForResult(intent, REQ_FILE_CHOOSER);
                } catch (Exception e) {
                    fileUploadCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                triggerHaptic(webView);
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                if (url.startsWith("data:")) {
                    saveDataUrlToDevice(url, fileName, mimeType);
                } else if (url.startsWith("blob:")) {
                    String safeName = fileName.replace("'", ""); String safeMime = (mimeType != null ? mimeType : "application/octet-stream"); String js = "javascript:(function(){var b=(window.__edutechBlobs&&window.__edutechBlobs['" + url + "'])||null;var send=function(bl){var fr=new FileReader();fr.onloadend=function(){(window.AndroidSessionBridge&&window.AndroidSessionBridge.saveBase64File?window.AndroidSessionBridge:window.AndroidVaultBridge).saveBase64File(fr.result,'" + safeName + "','" + safeMime + "');};fr.readAsDataURL(bl);};if(b){send(b);}else{fetch('" + url + "').then(r=>r.blob()).then(send).catch(function(){if(window.AndroidVaultBridge&&window.AndroidVaultBridge.printCurrentPage)window.AndroidVaultBridge.printCurrentPage();});}})();";
                    webView.evaluateJavascript(js, null);
                } else {
                    downloadHttpFileToDevice(url, fileName, mimeType);
                }
            }
        });

        mainColumn.addView(webView);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        String ua = settings.getUserAgentString();
        if (ua != null) {
            ua = ua.replace("; wv", "").replace("wv)", ")");
            settings.setUserAgentString(ua);
        }
    }

    private static final String SESSION_AND_FILE_BRIDGE_JS = "(function(){if(window.__edutechFileBridge)return;window.__edutechFileBridge=true;function sendToApp(u,fn,mt){if(!u)return false;if(u.indexOf('data:')===0){if(window.AndroidSessionBridge)window.AndroidSessionBridge.saveBase64File(u,fn||('file_'+Date.now()),mt||'application/octet-stream');return true;}if(u.indexOf('blob:')===0){fetch(u).then(function(r){return r.blob();}).then(function(b){var fr=new FileReader();fr.onloadend=function(){if(window.AndroidSessionBridge)window.AndroidSessionBridge.saveBase64File(fr.result,fn||('file_'+Date.now()),b.type||mt||'application/octet-stream');};fr.readAsDataURL(b);}).catch(function(){});return true;}return false;}var origClick=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){var u=this.getAttribute('href')||this.href||'';var dl=this.getAttribute('download');if(dl!==null||u.indexOf('blob:')===0||u.indexOf('data:')===0){if(sendToApp(u,dl||this.download||'',this.type||''))return;}return origClick.apply(this,arguments);};document.addEventListener('click',function(e){var a=e.target&&e.target.closest?e.target.closest('a'):null;if(!a)return;var u=a.getAttribute('href')||a.href||'';var dl=a.getAttribute('download');if(dl!==null||u.indexOf('blob:')===0||u.indexOf('data:')===0){if(sendToApp(u,dl||a.download||'',a.type||'')){e.preventDefault();e.stopPropagation();}}},true);var origOpen=window.open;window.open=function(u){if(typeof u==='string'&&(u.indexOf('blob:')===0||u.indexOf('data:')===0)){if(sendToApp(u,'',''))return null;}return origOpen.apply(this,arguments);};})();"; private void injectSessionAndPrintHooks(WebView view) {
        String js = "(function(){" +
                "try{" +
                "if(window.AndroidSessionBridge){" +
                "var saved=window.AndroidSessionBridge.getSession();" +
                "if(saved && saved.length>2){var obj=JSON.parse(saved);for(var k in obj){if(!sessionStorage.getItem(k))sessionStorage.setItem(k,obj[k]);}}" +
                "if(!window.__edutechHooked){window.__edutechHooked=true;" +
                "var sync=function(){try{var d={};for(var i=0;i<sessionStorage.length;i++){var k=sessionStorage.key(i);d[k]=sessionStorage.getItem(k);}window.AndroidSessionBridge.saveSession(JSON.stringify(d));}catch(e){}};" +
                "var origSet=sessionStorage.setItem;sessionStorage.setItem=function(k,v){origSet.apply(this,arguments);sync();};" +
                "var origRem=sessionStorage.removeItem;sessionStorage.removeItem=function(k){origRem.apply(this,arguments);sync();};" +
                "window.addEventListener('beforeunload',sync);" +
                "window.print=function(){if(window.AndroidVaultBridge)window.AndroidVaultBridge.printCurrentPage();};" + "window.__edutechBlobs=window.__edutechBlobs||{};var origCreateObj=URL.createObjectURL;URL.createObjectURL=function(obj){var u=origCreateObj.apply(this,arguments);try{window.__edutechBlobs[u]=obj;}catch(e){}return u;};var origClick=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){try{var h=this.getAttribute('href')||this.href||'';var dl=this.getAttribute('download')||'';if(h.indexOf('blob:')===0||h.indexOf('data:')===0||this.hasAttribute('download')){var fn=dl||('download_'+Date.now());if(h.indexOf('data:')===0){if(window.AndroidSessionBridge&&window.AndroidSessionBridge.saveBase64File){window.AndroidSessionBridge.saveBase64File(h,fn,'');return;}}else if(h.indexOf('blob:')===0){var blob=window.__edutechBlobs[h];var sendBlob=function(b){var r=new FileReader();r.onloadend=function(){if(r.result&&window.AndroidSessionBridge&&window.AndroidSessionBridge.saveBase64File){window.AndroidSessionBridge.saveBase64File(r.result,fn,b.type||'');}};r.readAsDataURL(b);};if(blob){sendBlob(blob);return;}else{fetch(h).then(function(res){return res.blob();}).then(sendBlob);return;}}}}catch(e){}return origClick.apply(this,arguments);};" + "window.__edutechBlobs=window.__edutechBlobs||{};var origCreateObj=URL.createObjectURL;URL.createObjectURL=function(obj){var u=origCreateObj.apply(this,arguments);try{window.__edutechBlobs[u]=obj;}catch(e){}return u;};var origClick=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){try{var h=this.getAttribute('href')||this.href||'';var dl=this.getAttribute('download')||'';if(h.indexOf('blob:')===0||h.indexOf('data:')===0||this.hasAttribute('download')){var fn=dl||('download_'+Date.now());if(h.indexOf('data:')===0){if(window.AndroidSessionBridge&&window.AndroidSessionBridge.saveBase64File){window.AndroidSessionBridge.saveBase64File(h,fn,'');return;}}else if(h.indexOf('blob:')===0){var blob=window.__edutechBlobs[h];var sendBlob=function(b){var r=new FileReader();r.onloadend=function(){if(r.result&&window.AndroidSessionBridge&&window.AndroidSessionBridge.saveBase64File){window.AndroidSessionBridge.saveBase64File(r.result,fn,b.type||'');}};r.readAsDataURL(b);};if(blob){sendBlob(blob);return;}else{fetch(h).then(function(res){return res.blob();}).then(sendBlob);return;}}}}catch(e){}return origClick.apply(this,arguments);};" +
                "}}" +
                "}catch(e){}" +
                "})();";
        view.evaluateJavascript(js + "\n" + 
 SESSION_AND_FILE_BRIDGE_JS, null);
    }

    private boolean handleUrlNavigation(Uri uri) {
        if (uri == null) return false;
        String url = uri.toString();
        String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";

        if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:") || url.startsWith("tg:") || url.startsWith("intent:")) {
            openExternalApp(uri);
            return true;
        }

        List<String> socialHosts = Arrays.asList(
                "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be",
                "facebook.com", "www.facebook.com", "m.facebook.com", "fb.com",
                "instagram.com", "www.instagram.com",
                "t.me", "telegram.me",
                "wa.me", "api.whatsapp.com", "whatsapp.com",
                "tiktok.com", "www.tiktok.com", "vm.tiktok.com"
        );
        for (String sh : socialHosts) {
            if (host.equals(sh) || host.endsWith("." + sh)) {
                openExternalApp(uri);
                return true;
            }
        }
        return false;
    }

    private void openExternalApp(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "لا يوجد تطبيق لفتح الرابط", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupPopupContainer() {
        popupContainer = new FrameLayout(this);
        popupContainer.setBackgroundColor(Color.WHITE);
        popupContainer.setVisibility(View.GONE);
        rootLayout.addView(popupContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void closePopupWebView() {
        if (popupWebView != null) {
            popupWebView.destroy();
            popupWebView = null;
        }
        popupContainer.removeAllViews();
        popupContainer.setVisibility(View.GONE);
    }

    private void setupBottomActionBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Color.parseColor("#0F172A"));
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));

        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(52));
        barLp.gravity = Gravity.BOTTOM;

        Button btnLibrary = createBarButton("📚 مكتبتي", "#1E3A8A");
        btnLibrary.setOnClickListener(v -> {
            triggerHaptic(v);
            showLocalLibraryDialog();
        });

        Button btnVoice = createBarButton("🎙️ تحدث", "#047857");
        btnVoice.setOnClickListener(v -> {
            triggerHaptic(v);
            startVoiceRecognition();
        });

        Button btnOverlay = createBarButton("🤖 عائم", "#6D28D9");
        btnOverlay.setOnClickListener(v -> {
            triggerHaptic(v);
            toggleOverlayAssistant();
        });

        bar.addView(btnLibrary);
        bar.addView(btnVoice);
        bar.addView(btnOverlay);
        Button btnBack = createBarButton("⬅", "#334155"); btnBack.setOnClickListener(v -> { triggerHaptic(v); if (webView.canGoBack()) webView.goBack(); }); Button btnHome = createBarButton("🏠", "#0284C7"); btnHome.setOnClickListener(v -> { triggerHaptic(v); webView.loadUrl(HOME_URL); }); Button btnReload = createBarButton("🔄", "#475569"); btnReload.setOnClickListener(v -> { triggerHaptic(v); webView.reload(); }); bar.addView(btnBack, 0); bar.addView(btnHome, 1); bar.addView(btnReload); bottomControlBar = bar; audioPlayerBar = new LinearLayout(this); audioPlayerBar.setOrientation(LinearLayout.HORIZONTAL); audioPlayerBar.setBackgroundColor(Color.parseColor("#1E293B")); audioPlayerBar.setVisibility(View.GONE); mainColumn.addView(audioPlayerBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); mainColumn.addView(bottomControlBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private Button createBarButton(String text, String hexColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13f);
        btn.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(hexColor));
        bg.setCornerRadius(dpToPx(18));
        btn.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void setupIntroMotionGraphics() {
        splashView = new IntroSplashView(this);
        rootLayout.addView(splashView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); android.widget.ImageView logoImg = new android.widget.ImageView(this); logoImg.setTag("splashLogo"); logoImg.setImageResource(R.mipmap.ic_launcher); rootLayout.addView(logoImg, new FrameLayout.LayoutParams(240, 240, android.view.Gravity.CENTER)); ObjectAnimator pulseAnim = ObjectAnimator.ofPropertyValuesHolder(logoImg, PropertyValuesHolder.ofFloat(View.SCALE_X, 0.92f, 1.08f), PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.92f, 1.08f)); pulseAnim.setDuration(900); pulseAnim.setRepeatCount(ValueAnimator.INFINITE); pulseAnim.setRepeatMode(ValueAnimator.REVERSE); pulseAnim.start();
        splashView.setOnClickListener(v -> {
            triggerHaptic(v);
            dismissSplash();
        });
        mainHandler.postDelayed(this::dismissSplash, 2300);
    }

    private void dismissSplash() { View logo = rootLayout.findViewWithTag("splashLogo"); if (logo != null && logo.getParent() != null) { logo.animate().alpha(0f).setDuration(450).withEndAction(() -> { if (logo.getParent() != null) rootLayout.removeView(logo); }).start(); }
        if (splashView != null && splashView.getParent() != null) {
            splashView.animate().alpha(0f).setDuration(450).withEndAction(() -> {
                if (splashView.getParent() != null) {
                    rootLayout.removeView(splashView);
                }
                splashView.stopAnimation();
            }).start();
        }
    }

    private void triggerHaptic(View v) {
        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(25);
            }
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن مع المساعد التعليمي...");
        try {
            startActivityForResult(intent, REQ_VOICE_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "التعرف الصوتي غير مدعوم على هذا الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleOverlayAssistant() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
            Toast.makeText(this, "يرجى تفعيل صلاحية الظهور فوق التطبيقات", Toast.LENGTH_LONG).show();
            return;
        }
        Intent serviceIntent = new Intent(this, OverlayAssistantService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "تم تفعيل 🤖 مساعدي العائم", Toast.LENGTH_SHORT).show();
    }

    private void showLocalLibraryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        container.setBackgroundColor(Color.parseColor("#0F172A"));

        TextView title = new TextView(this);
        title.setText("📚 مكتبتي المحلية (EduTech Vault)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        container.addView(title);

        Button btnImport = createBarButton("➕ استيراد ملف (PDF / صوت)", "#2563EB");
        LinearLayout.LayoutParams importLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        importLp.topMargin = dpToPx(12);
        importLp.bottomMargin = dpToPx(12);
        btnImport.setLayoutParams(importLp);
        container.addView(btnImport);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listLayout);
        container.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(300)));

        builder.setView(container);
        AlertDialog dialog = builder.create();

        btnImport.setOnClickListener(v -> {
            triggerHaptic(v);
            dialog.dismiss();
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "audio/*"});
            startActivityForResult(intent, REQ_IMPORT_VAULT);
        });

        File[] files = vaultDir.listFiles();
        if (files == null || files.length == 0) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد ملفات محفوظة حالياً. استورد ملف PDF أو كتاباً صوتياً للبدء.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setPadding(0, dpToPx(16), 0, dpToPx(16));
            listLayout.addView(empty);
        } else {
            for (File f : files) {
                Button itemBtn = createBarButton("📄 " + f.getName(), "#1E293B");
                LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
                itemLp.bottomMargin = dpToPx(8);
                itemBtn.setLayoutParams(itemLp);
                itemBtn.setOnClickListener(v -> {
                    triggerHaptic(v);
                    dialog.dismiss();
                    openVaultFile(f);
                });
                LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); rLp.bottomMargin = dpToPx(8); row.setLayoutParams(rLp); itemBtn.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(44), 1f)); row.addView(itemBtn); Button pubBtn = createBarButton("📤 للمنصة", "#0EA5E9"); LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(44)); pLp.leftMargin = dpToPx(6); pubBtn.setLayoutParams(pLp); pubBtn.setOnClickListener(v -> { triggerHaptic(v); dialog.dismiss(); publishFileToWebApp(f); }); row.addView(pubBtn); listLayout.addView(row);
            }
        }
        dialog.show();  
    }
    private void publishFileToWebApp(File file) { new Thread(() -> { try { java.io.FileInputStream fis = new java.io.FileInputStream(file); java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(); byte[] buf = new byte[8192]; int len; while ((len = fis.read(buf)) != -1) bos.write(buf, 0, len); fis.close(); String b64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP); String name = file.getName(); String lower = name.toLowerCase(Locale.ROOT); String mime = lower.endsWith(".pdf") ? "application/pdf" : (lower.endsWith(".mp3") ? "audio/mpeg" : (lower.endsWith(".png") ? "image/png" : (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? "image/jpeg" : "application/octet-stream"))); String js = "(function(){try{var b64='" + b64 + "',name=" + org.json.JSONObject.quote(name) + ",mime='" + mime + "';var bin=atob(b64),arr=new Uint8Array(bin.length);for(var i=0;i<bin.length;i++)arr[i]=bin.charCodeAt(i);var f=new File([arr],name,{type:mime});var inp=document.querySelector('input[type=\"file\"]');if(inp){var dt=new DataTransfer();dt.items.add(f);inp.files=dt.files;inp.dispatchEvent(new Event('change',{bubbles:true}));}window.dispatchEvent(new CustomEvent('edutech-local-file',{detail:{name:name,mime:mime,base64:b64,file:f}}));}catch(e){console.error(e);}})();"; mainHandler.post(() -> { if (webView != null) webView.evaluateJavascript(js, null); }); } catch (Exception ignored) {} }).start(); }
    }

    private void openVaultFile(File file) {
        String lower = file.getName().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            openBuiltInPdfViewer(file);
        } else if (lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") || lower.endsWith(".ogg")) {
            openBuiltInAudioPlayer(file);
        } else {
            Toast.makeText(this, "تم حفظ الملف في: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private void openBuiltInPdfViewer(File pdfFile) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0F172A"));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        Button btnPrev = createBarButton("السابق", "#1E3A8A");
        Button btnNext = createBarButton("التالي", "#1E3A8A");
        Button btnClose = createBarButton("إغلاق", "#DC2626");
        TextView pageInfo = new TextView(this);
        pageInfo.setTextColor(Color.WHITE);
        pageInfo.setGravity(Gravity.CENTER);
        pageInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        topBar.addView(btnPrev);
        topBar.addView(pageInfo);
        topBar.addView(btnNext);
        topBar.addView(btnClose);
        layout.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(52)));

        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        layout.addView(imageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);
            final int[] currentIndex = {0};
            final int totalPages = renderer.getPageCount();

            Runnable renderPage = () -> {
                if (totalPages <= 0) return;
                PdfRenderer.Page page = renderer.openPage(currentIndex[0]);
                Bitmap bmp = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                bmp.eraseColor(Color.WHITE);
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                imageView.setImageBitmap(bmp);
                pageInfo.setText((currentIndex[0] + 1) + " / " + totalPages);
            };

            btnPrev.setOnClickListener(v -> {
                triggerHaptic(v);
                if (currentIndex[0] > 0) {
                    currentIndex[0]--;
                    renderPage.run();
                }
            });
            btnNext.setOnClickListener(v -> {
                triggerHaptic(v);
                if (currentIndex[0] < totalPages - 1) {
                    currentIndex[0]++;
                    renderPage.run();
                }
            });
            btnClose.setOnClickListener(v -> {
                triggerHaptic(v);
                try {
                    renderer.close();
                    pfd.close();
                } catch (Exception ignored) {}
                dialog.dismiss();
            });
            dialog.setOnDismissListener(d -> {
                try {
                    renderer.close();
                    pfd.close();
                } catch (Exception ignored) {}
            });

            renderPage.run();
            dialog.setContentView(layout);
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح ملف PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void openBuiltInAudioPlayer(File audioFile) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        layout.setBackgroundColor(Color.parseColor("#0F172A"));

        TextView title = new TextView(this);
        title.setText("🎧 " + audioFile.getName());
        title.setTextColor(Color.WHITE);
        title.setTextSize(16f);
        layout.addView(title);

        SeekBar seekBar = new SeekBar(this);
        LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seekLp.topMargin = dpToPx(16);
        seekLp.bottomMargin = dpToPx(16);
        layout.addView(seekBar, seekLp);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button btnPlayPause = createBarButton("⏸️ إيقاف مؤقت", "#047857");
        Button btnSpeed = createBarButton("السرعة: 1.0x", "#1D4ED8");
        controls.addView(btnPlayPause);
        controls.addView(btnSpeed);
        layout.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(46)));

        MediaPlayer mp = new MediaPlayer();
        final float[] speeds = {1.0f, 1.25f, 1.5f};
        final int[] speedIdx = {0};

        try {
            mp.setDataSource(audioFile.getAbsolutePath());
            mp.prepare();
            seekBar.setMax(mp.getDuration());
            mp.start();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تشغيل الملف الصوتي", Toast.LENGTH_SHORT).show();
            return;
        }

        Runnable updateSeek = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mp.isPlaying()) {
                        seekBar.setProgress(mp.getCurrentPosition());
                        mainHandler.postDelayed(this, 400);
                    }
                } catch (Exception ignored) {}
            }
        };
        mainHandler.post(updateSeek);

        btnPlayPause.setOnClickListener(v -> {
            triggerHaptic(v);
            if (mp.isPlaying()) {
                mp.pause();
                btnPlayPause.setText("▶️ تشغيل");
            } else {
                mp.start();
                btnPlayPause.setText("⏸️ إيقاف مؤقت");
                mainHandler.post(updateSeek);
            }
        });

        btnSpeed.setOnClickListener(v -> {
            triggerHaptic(v);
            speedIdx[0] = (speedIdx[0] + 1) % speeds.length;
            float s = speeds[speedIdx[0]];
            btnSpeed.setText("السرعة: " + s + "x");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    PlaybackParams params = mp.getPlaybackParams();
                    params.setSpeed(s);
                    mp.setPlaybackParams(params);
                } catch (Exception ignored) {}
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) mp.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("إغلاق", (d, w) -> d.dismiss())
                .create();
        dialog.setOnDismissListener(d -> {
            mainHandler.removeCallbacks(updateSeek);
            try {
                if (mp.isPlaying()) mp.stop();
                mp.release();
            } catch (Exception ignored) {}
        });
        dialog.show();
    }

    private void saveDataUrlToDevice(String dataUrl, String fileName, String mimeType) {
        try {
            int comma = dataUrl.indexOf(',');
            if (comma < 0) return;
            String base64 = dataUrl.substring(comma + 1);
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            saveBytesToVaultAndDownloads(bytes, fileName, mimeType);
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "فشل حفظ الملف", Toast.LENGTH_SHORT).show());
        }
    }

    private void downloadHttpFileToDevice(String urlStr, String fileName, String mimeType) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                String cookies = CookieManager.getInstance().getCookie(urlStr);
                if (cookies != null) conn.setRequestProperty("Cookie", cookies);
                conn.connect();
                InputStream in = conn.getInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                in.close();
                saveBytesToVaultAndDownloads(bos.toByteArray(), fileName, mimeType);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "فشل تنزيل الملف", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void saveBytesToVaultAndDownloads(byte[] data, String fileName, String mimeType) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "EduTech_" + System.currentTimeMillis() + ".pdf";
            }
            File vaultFile = new File(vaultDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(vaultFile)) {
                fos.write(data);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EduTechMindset");
                ContentResolver resolver = getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = resolver.openOutputStream(uri)) {
                        if (os != null) os.write(data);
                    }
                }
            } else {
                File dlDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EduTechMindset");
                if (!dlDir.exists()) dlDir.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(new File(dlDir, fileName))) {
                    fos.write(data);
                }
            }

            String finalName = fileName;
            runOnUiThread(() -> Toast.makeText(this, "تم حفظ الملف في المكتبة والتنزيلات: " + finalName, Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "تعذر الحفظ في التنزيلات", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == 105 && pendingWebPermissionRequest != null) { boolean ok = grantResults.length > 0; for (int r : grantResults) { if (r != PackageManager.PERMISSION_GRANTED) { ok = false; break; } } if (ok) { pendingWebPermissionRequest.grant(pendingWebPermissionRequest.getResources()); } else { pendingWebPermissionRequest.deny(); } pendingWebPermissionRequest = null; } } @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE_CHOOSER) {
            if (fileUploadCallback != null) {
                Uri[] results = null; if (resultCode == Activity.RESULT_OK) { if (data != null && data.getClipData() != null) { int count = data.getClipData().getItemCount(); results = new Uri[count]; for (int i = 0; i < count; i++) { results[i] = data.getClipData().getItemAt(i).getUri(); } } else if (data != null && data.getData() != null) { results = new Uri[]{data.getData()}; } else if (cameraCaptureUri != null) { results = new Uri[]{cameraCaptureUri}; } } cameraCaptureUri = null;
                        //
                fileUploadCallback.onReceiveValue(results);
                fileUploadCallback = null;
            }
        } else if (requestCode == REQ_IMPORT_VAULT && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            importUriToVault(data.getData());
        } else if (requestCode == REQ_VOICE_INPUT && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                sendVoiceTextToWebAssistant(matches.get(0));
            }
        } else if (requestCode == REQ_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                toggleOverlayAssistant();
            }
        }
    }

    private void importUriToVault(Uri uri) {
        try {
            String name = "file_" + System.currentTimeMillis();
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) name = cursor.getString(idx);
                }
            }
            File dest = new File(vaultDir, name);
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int len;
                while (in != null && (len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            Toast.makeText(this, "تمت إضافة " + name + " إلى مكتبتي", Toast.LENGTH_SHORT).show();
            openVaultFile(dest);
        } catch (Exception e) {
            Toast.makeText(this, "فشل استيراد الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendVoiceTextToWebAssistant(String spokenText) {
        String escaped = JSONObject.quote(spokenText);
        String js = "(function(){" +
                "var txt=" + escaped + ";" +
                "var el=document.querySelector('textarea, input[type=\"text\"], input:not([type]), [contenteditable=\"true\"]');" +
                "if(el){" +
                "el.focus();" +
                "if(el.tagName==='TEXTAREA'||el.tagName==='INPUT'){el.value=txt;el.dispatchEvent(new Event('input',{bubbles:true}));}" +
                "else{el.innerText=txt;el.dispatchEvent(new Event('input',{bubbles:true}));}" +
                "var form=el.closest('form');if(form){form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));}" +
                "}" +
                "})();";
        if (webView != null) {
            webView.evaluateJavascript(js, null);
        }
        Toast.makeText(this, "تم إرسال: " + spokenText, Toast.LENGTH_SHORT).show();
    }

    private void requestAppPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECORD_AUDIO);
        perms.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.READ_MEDIA_AUDIO);
            perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        List<String> missing = new ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), REQ_PERMISSIONS);
        }
    }

    @Override
    public void onBackPressed() {
        if (popupContainer.getVisibility() == View.VISIBLE) {
            closePopupWebView();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
    private void triggerNativePrint() { new AndroidVaultBridge().printCurrentPage(); }
    
    public class AndroidSessionBridge {
        @JavascriptInterface
        public void saveSession(String json) {
            if (json != null) {
                sessionPrefs.edit().putString(KEY_SESSION_JSON, json).apply();
            }
        }

        @JavascriptInterface
        public String getSession() {
            return sessionPrefs.getString(KEY_SESSION_JSON, "{}");
        } @JavascriptInterface public void saveBase64File(String dataUrl, String fileName, String mimeType) { saveDataUrlToDevice(dataUrl, fileName, mimeType); }
    }

    public class AndroidVaultBridge {
        @JavascriptInterface
        public void saveBase64File(String dataUrl, String fileName, String mimeType) {
            saveDataUrlToDevice(dataUrl, fileName, mimeType);
        }

        @JavascriptInterface
        public void printCurrentPage() {
            runOnUiThread(() -> {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (printManager != null && webView != null) {
                    String jobName = "EduTech_Mindset_Print";
                    printManager.print(jobName, webView.createPrintDocumentAdapter(jobName), new PrintAttributes.Builder().build());
                }
            });
        }
    }

    private static class IntroSplashView extends View {
        private final Paint bgPaint = new Paint();
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ValueAnimator animator;
        private float animProgress = 0f;
        private final float[] px = new float[28];
        private final float[] py = new float[28];
        private final float[] pr = new float[28];

        public IntroSplashView(Context context) {
            super(context);
            Random rnd = new Random(42);
            for (int i = 0; i < px.length; i++) {
                px[i] = rnd.nextFloat();
                py[i] = rnd.nextFloat();
                pr[i] = 3f + rnd.nextFloat() * 7f;
            }
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(5f);
            particlePaint.setColor(Color.parseColor("#38BDF8"));

            titlePaint.setColor(Color.WHITE);
            titlePaint.setTextSize(68f);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextAlign(Paint.Align.CENTER);

            subPaint.setColor(Color.parseColor("#93C5FD"));
            subPaint.setTextSize(38f);
            subPaint.setTextAlign(Paint.Align.CENTER);

            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(2000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                animProgress = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;

            bgPaint.setShader(new RadialGradient(cx, cy, Math.max(w, h) * 0.7f,
                    Color.parseColor("#1E3A8A"), Color.parseColor("#020617"), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, bgPaint);

            for (int i = 0; i < px.length; i++) {
                float yPos = ((py[i] + animProgress * 0.35f) % 1.0f) * h;
                particlePaint.setAlpha(90 + (i % 5) * 30);
                canvas.drawCircle(px[i] * w, yPos, pr[i], particlePaint);
            }

            for (int r = 0; r < 3; r++) {
                float phase = (animProgress + r * 0.33f) % 1.0f;
                ringPaint.setColor(Color.parseColor("#38BDF8"));
                ringPaint.setAlpha((int) ((1f - phase) * 180));
                canvas.drawCircle(cx, cy - 40f, 120f + phase * 260f, ringPaint);
            }

            canvas.drawText("EduTech Mindset", cx, cy - 20f, titlePaint);
            canvas.drawText("إدارة الأستاذ باسل أشرف الفقعاوي", cx, cy + 55f, subPaint);
        }

        public void stopAnimation() {
            if (animator != null) animator.cancel();
        }
    }
}
