package com.aistudio.edutechmindset.bfkqaz;

import android.graphics.Outline; import android.media.AudioFormat; import android.media.AudioManager; import android.media.AudioTrack; import android.os.Message; import android.view.HapticFeedbackConstants; import android.view.ViewOutlineProvider; import androidx.activity.OnBackPressedCallback;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfRenderer;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONObject;

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
    private static final String PREFS_NAME = "edutech_prefs";
    private static final String KEY_SESSION_JSON = "session_storage_json";
    private static final int REQ_PERMS = 1001;
    private static final int REQ_FILE = 1002;
    private static final int REQ_VOICE = 1004;
    private static final int REQ_OVERLAY = 1005;

      private View quickMenuCard; private View videoViewerOverlay; private View pdfViewerOverlay; private View vaultOverlay; private WebView popupWebView;
    private FrameLayout rootLayout;
    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout assistButton;
    private LinearLayout offlineOverlay;
    private FrameLayout splashOverlay;
    private MediaPlayer introVoicePlayer;
    private ValueCallback<Uri[]> filePathCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean doubleBackToExit = false;
    private boolean splashDismissed = false;

    private static final String SMART_BRIDGE_JS =
        "(function(){" +
        "if(window.__eduBridgeInstalled) return;" +
        "window.__eduBridgeInstalled = true;" +
        "window.__inAnchorClick = false;" +
        "window.__inSaveItem = false;" +
        "function saveItemSafe(dataUrl, name){" +
        "  if(window.__inSaveItem) return;" +
        "  window.__inSaveItem = true;" +
        "  try{" +
        "    if(window.AndroidVault && dataUrl){ window.AndroidVault.saveDataUrl(String(dataUrl), String(name||'file')); }" +
        "  }finally{ setTimeout(function(){ window.__inSaveItem = false; }, 250); }" +
        "}" +
        "document.addEventListener('click', function(e){" +
        "  if(window.__inAnchorClick) return;" +
        "  var a = e.target && e.target.closest ? e.target.closest('a[download],a[href^=\"data:\"],a[href^=\"blob:\"]') : null;" +
        "  if(!a) return;" +
        "  var href = a.getAttribute('href') || '';" +
        "  if(href.indexOf('data:')===0){" +
        "    e.preventDefault(); window.__inAnchorClick = true;" +
        "    try{ saveItemSafe(href, a.getAttribute('download')||'edutech_file'); }finally{ setTimeout(function(){ window.__inAnchorClick = false; }, 250); }" +
        "  } else if(href.indexOf('blob:')===0){" +
        "    e.preventDefault(); window.__inAnchorClick = true;" +
        "    fetch(href).then(function(r){return r.blob();}).then(function(b){" +
        "      var rd = new FileReader(); rd.onloadend = function(){ saveItemSafe(rd.result, a.getAttribute('download')||'edutech_file'); }; rd.readAsDataURL(b);" +
        "    }).finally(function(){ setTimeout(function(){ window.__inAnchorClick = false; }, 250); });" +
        "  }" +
        "}, true);" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupImmersiveFullScreen();
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#0B1120"));
        setContentView(rootLayout);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.addView(webView);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams pbLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
        pbLp.gravity = Gravity.TOP;
        rootLayout.addView(progressBar, pbLp);

        configureWebView();
        setupOfflineOverlay();
        setupDraggableAssistantButton();
        setupIntroSplash();
        requestNeededPermissions();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) { @Override public void handleOnBackPressed() { handleSmartBack(); } });
        
        if (isNetworkAvailable()) {
            webView.loadUrl(HOME_URL);
        } else {
            offlineOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void setupImmersiveFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        WindowInsetsControllerCompat ctrl = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (ctrl != null) {
            ctrl.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            ctrl.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setupImmersiveFullScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupImmersiveFullScreen();
        sendOverlayAction(OverlayAssistantService.ACTION_HIDE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Settings.canDrawOverlays(this)) {
            sendOverlayAction(OverlayAssistantService.ACTION_SHOW);
        }
    }

    private void sendOverlayAction(String action) {
        try {
            if (!Settings.canDrawOverlays(this)) return;
            Intent i = new Intent(this, OverlayAssistantService.class);
            i.setAction(action);
            startService(i);
        } catch (Exception ignored) {}
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private void requestNeededPermissions() {
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!perms.isEmpty()) ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
    }
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new AndroidVaultBridge(), "AndroidVault");
        webView.addJavascriptInterface(new AndroidSessionBridge(), "AndroidSession");
        s.setSupportMultipleWindows(true); s.setJavaScriptCanOpenWindowsAutomatically(true); webView.addJavascriptInterface(new AndroidSessionBridge(), "AndroidSessionBridge");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                offlineOverlay.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                view.evaluateJavascript(SMART_BRIDGE_JS, null);
                String saved = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_SESSION_JSON, "");
                if (!saved.isEmpty()) {
                    view.evaluateJavascript("try{var d=" + JSONObject.quote(saved) + ";var o=JSON.parse(d);for(var k in o)sessionStorage.setItem(k,o[k]);}catch(e){}", null);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req != null && req.isForMainFrame()) {
                    offlineOverlay.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
        
                 @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) { closeAuthPopupView(); popupWebView = new WebView(MainActivity.this); WebSettings ps = popupWebView.getSettings(); ps.setJavaScriptEnabled(true); ps.setDomStorageEnabled(true); ps.setJavaScriptCanOpenWindowsAutomatically(true); ps.setSupportMultipleWindows(true); CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, true); popupWebView.addJavascriptInterface(new AndroidSessionBridge(), "AndroidSessionBridge"); popupWebView.setWebViewClient(new WebViewClient() { @Override public void onPageFinished(WebView pv, String url) { pv.evaluateJavascript("window.close = function() { if (window.AndroidSessionBridge) window.AndroidSessionBridge.closeAuthPopup(); };", null); } }); popupWebView.setWebChromeClient(new WebChromeClient() { @Override public void onCloseWindow(WebView window) { closeAuthPopupView(); } }); rootLayout.addView(popupWebView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj; transport.setWebView(popupWebView); resultMsg.sendToTarget(); return true; }
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                    return true;
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) { return true; }
        });

        webView.setDownloadListener((url, ua, cd, mime, len) -> {
            if (url != null && url.startsWith("data:")) {
                saveDataUrlToDevice(url, "edutech_download");
            } else if (url != null) {
                downloadUrlToVault(url, "edutech_download");
            }
        });
    }

    private void setupOfflineOverlay() {
        offlineOverlay = new LinearLayout(this);
        offlineOverlay.setOrientation(LinearLayout.VERTICAL);
        offlineOverlay.setGravity(Gravity.CENTER);
        offlineOverlay.setBackgroundColor(Color.parseColor("#EE0B1120"));
        offlineOverlay.setPadding(dp(28), dp(28), dp(28), dp(28));
        offlineOverlay.setVisibility(View.GONE);

        TextView title = new TextView(this);
        title.setText("📡 لا يوجد اتصال بالإنترنت");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20f);
        title.setGravity(Gravity.CENTER);
        offlineOverlay.addView(title);

        TextView sub = new TextView(this);
        sub.setText("يمكنك إعادة المحاولة أو تصفح ملفاتك المحفوظة في مكتبتي دون إنترنت.");
        sub.setTextColor(Color.parseColor("#94A3B8"));
        sub.setTextSize(14f);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(10), 0, dp(22));
        offlineOverlay.addView(sub);

        Button retryBtn = new Button(this);
        retryBtn.setText("🔄 إعادة المحاولة");
        retryBtn.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                offlineOverlay.setVisibility(View.GONE);
                webView.reload();
            } else {
                Toast.makeText(this, "ما زال الاتصال غير متاح", Toast.LENGTH_SHORT).show();
            }
        });
        offlineOverlay.addView(retryBtn);

        Button libBtn = new Button(this);
        libBtn.setText("📚 فتح مكتبتي");
        libBtn.setOnClickListener(v -> showLocalLibraryDialog());
        offlineOverlay.addView(libBtn);

        rootLayout.addView(offlineOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void setupDraggableAssistantButton() {
        assistButton = new FrameLayout(this);
        int size = dp(44);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setStroke(dp(2), Color.parseColor("#38BDF8"));
        assistButton.setBackground(bg);
        assistButton.setPadding(dp(2), dp(2), dp(2), dp(2));
        assistButton.setElevation(dp(8));

        ImageView img = new ImageView(this);
        img.setImageResource(R.mipmap.ic_launcher);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
               img.setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View view, Outline outline) { outline.setOval(0, 0, view.getWidth(), view.getHeight()); } }); img.setClipToOutline(true);
                assistButton.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = dp(12);
        lp.topMargin = dp(180);
        rootLayout.addView(assistButton, lp);

        final float[] d = new float[4];
        assistButton.setOnTouchListener((v, e) -> {
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    d[0] = e.getRawX() - p.leftMargin;
                    d[1] = e.getRawY() - p.topMargin;
                    d[2] = e.getRawX();
                    d[3] = e.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int maxX = Math.max(0, rootLayout.getWidth() - size);
                    int maxY = Math.max(0, rootLayout.getHeight() - size);
                    p.leftMargin = Math.max(0, Math.min(maxX, (int) (e.getRawX() - d[0])));
                    p.topMargin = Math.max(0, Math.min(maxY, (int) (e.getRawY() - d[1])));
                    v.setLayoutParams(p);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (Math.hypot(e.getRawX() - d[2], e.getRawY() - d[3]) < dp(8)) {
                             playClickFeedback();
                        showQuickAssistantMenu();
                    } else {
                        int mid = rootLayout.getWidth() / 2;
                        p.leftMargin = (p.leftMargin + size / 2 < mid) ? dp(8) : Math.max(0, rootLayout.getWidth() - size - dp(8));
                        v.setLayoutParams(p);
                    }
                    return true;
            }
            return false;
        });
    }

    private void showQuickAssistantMenu() {
        List<String> items = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        items.add("🤖 فتح المساعد الذكي (واجهة الموقع)");
        actions.add(() -> webView.evaluateJavascript("try{var b=document.querySelector('[data-assistant],.ai-btn,#ai-btn');if(b)b.click();}catch(e){}", null));
        items.add("🎙️ تحدث صوتياً");
        actions.add(this::startVoiceInput);
        items.add("📚 مكتبتي (PDF • صوت • فيديو)");
        actions.add(this::showLocalLibraryDialog);
        if (!Settings.canDrawOverlays(this)) {
            items.add("🤖 تفعيل الظهور فوق التطبيقات");
            actions.add(() -> startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY));
        }
        items.add("📤 مشاركة رابط المنصة");
        actions.add(() -> {
            Intent sh = new Intent(Intent.ACTION_SEND);
            sh.setType("text/plain");
            sh.putExtra(Intent.EXTRA_TEXT, HOME_URL);
            startActivity(Intent.createChooser(sh, "مشاركة رابط المنصة"));
        });
        items.add("⚡ تسريع وتنظيف الكاش");
        actions.add(() -> {
            webView.clearCache(true);
            Toast.makeText(this, "تم تنظيف الكاش وتسريع التطبيق", Toast.LENGTH_SHORT).show();
        });
        items.add("🔄 تحديث الصفحة");
        actions.add(() -> webView.reload());

        new AlertDialog.Builder(this)
                .setTitle("⚡ القائمة السريعة")
                .setItems(items.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void startVoiceInput() {
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
            startActivityForResult(i, REQ_VOICE);
        } catch (Exception e) {
            Toast.makeText(this, "الإدخال الصوتي غير مدعوم على هذا الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupIntroSplash() {
        splashOverlay = new FrameLayout(this);
        splashOverlay.setBackgroundColor(Color.parseColor("#0B1120"));
        splashOverlay.setClickable(true);

        MotionParticlesView particles = new MotionParticlesView(this);
        splashOverlay.addView(particles, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);

        ImageView logo = new ImageView(this);
                logo.setScaleType(ImageView.ScaleType.CENTER_CROP); logo.setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View view, Outline outline) { outline.setOval(0, 0, view.getWidth(), view.getHeight()); } }); logo.setClipToOutline(true);
        logo.setImageResource(R.mipmap.ic_launcher);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(110), dp(110));
        center.addView(logo, logoLp);

        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(logo,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.85f, 1.08f, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.85f, 1.08f, 1.0f));
        pulse.setDuration(1400);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.start();

        TextView appTitle = new TextView(this);
        appTitle.setText("EduTech Mindset");
        appTitle.setTextColor(Color.WHITE);
        appTitle.setTextSize(24f);
        appTitle.setPadding(0, dp(18), 0, dp(24));
        appTitle.setGravity(Gravity.CENTER); center.addView(appTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); TextView appSub = new TextView(this); appSub.setText("منصة التعليم الذكي"); appSub.setTextColor(Color.parseColor("#38BDF8")); appSub.setTextSize(16f); appSub.setGravity(Gravity.CENTER); appSub.setPadding(0, dp(4), 0, dp(8)); center.addView(appSub, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); TextView appTag = new TextView(this); appTag.setText("استعد لرحلة تعلّم استثنائية"); appTag.setTextColor(Color.parseColor("#CBD5E1")); appTag.setTextSize(14f); appTag.setGravity(Gravity.CENTER); appTag.setPadding(0, 0, 0, dp(24)); center.addView(appTag, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button enterBtn = new Button(this);
        enterBtn.setText("🚀 ادخل التطبيق");
        enterBtn.setVisibility(View.INVISIBLE);
        enterBtn.setAlpha(0f);
        enterBtn.setTextColor(Color.parseColor("#0F172A")); enterBtn.setAllCaps(false); enterBtn.setPadding(dp(28), dp(12), dp(28), dp(12)); android.graphics.drawable.GradientDrawable enterBg = new android.graphics.drawable.GradientDrawable(); enterBg.setColor(Color.parseColor("#38BDF8")); enterBg.setCornerRadius(dp(999)); enterBtn.setBackground(enterBg); enterBtn.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); playClickFeedback(); dismissSplash(); }); playIntroChime();
        LinearLayout.LayoutParams enterLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); enterLp.gravity = Gravity.CENTER; center.addView(enterBtn, enterLp);

        splashOverlay.addView(center, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.addView(splashOverlay);

        try {
            int rawId = getResources().getIdentifier("intro_voice", "raw", getPackageName());
            if (rawId != 0) {
                introVoicePlayer = MediaPlayer.create(this, rawId);
                if (introVoicePlayer != null) introVoicePlayer.start();
            }
        } catch (Exception ignored) {}

        mainHandler.postDelayed(() -> {
            if (!splashDismissed) {
                enterBtn.setVisibility(View.VISIBLE);
                enterBtn.animate().alpha(1f).scaleX(1.05f).scaleY(1.05f).setDuration(500).start();
            }
        }, 10000);

        mainHandler.postDelayed(this::dismissSplash, 15000);
    }

    private void dismissSplash() {
        if (splashDismissed) return;
        splashDismissed = true;
        try {
            if (introVoicePlayer != null) {
                introVoicePlayer.release();
                introVoicePlayer = null;
            }
        } catch (Exception ignored) {}
        if (splashOverlay != null) {
            splashOverlay.animate().alpha(0f).setDuration(350).withEndAction(() -> splashOverlay.setVisibility(View.GONE)).start();
        }
    }
    private File getVaultDir() {
        File dir = new File(getFilesDir(), "EduTechVault");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void saveDataUrlToDevice(String dataUrl, String rawName) {
        new Thread(() -> {
            try {
                int comma = dataUrl.indexOf(',');
                if (comma < 0) return;
                String meta = dataUrl.substring(0, comma);
                byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
                String ext = ".bin";
                if (meta.contains("pdf")) ext = ".pdf";
                else if (meta.contains("mpeg") || meta.contains("mp3")) ext = ".mp3";
                else if (meta.contains("wav")) ext = ".wav";
                else if (meta.contains("mp4")) ext = ".mp4";
                else if (meta.contains("png")) ext = ".png";
                else if (meta.contains("jpeg") || meta.contains("jpg")) ext = ".jpg";
                String safeName = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (!safeName.contains(".")) safeName += "_" + System.currentTimeMillis() + ext;

                File vaultFile = new File(getVaultDir(), safeName);
                try (FileOutputStream fos = new FileOutputStream(vaultFile)) { fos.write(bytes); }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
                    cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EduTechMindset");
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri != null) {
                        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                            if (os != null) os.write(bytes);
                        }
                    }
                } else {
                    File dlDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EduTechMindset");
                    if (!dlDir.exists()) dlDir.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(new File(dlDir, safeName))) { fos.write(bytes); }
                }
                runOnUiThread(() -> Toast.makeText(this, "تم الحفظ في مكتبتي و Downloads/EduTechMindset", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "تعذر الحفظ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void downloadUrlToVault(String urlStr, String rawName) {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
                c.connect();
                String ext = urlStr.contains(".pdf") ? ".pdf" : (urlStr.contains(".mp3") ? ".mp3" : (urlStr.contains(".mp4") ? ".mp4" : ".bin"));
                File out = new File(getVaultDir(), rawName + "_" + System.currentTimeMillis() + ext);
                try (InputStream is = c.getInputStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                }
                runOnUiThread(() -> Toast.makeText(this, "تم تنزيل الملف إلى مكتبتي", Toast.LENGTH_SHORT).show());
            } catch (Exception ignored) {}
        }).start();
    }

    private void showLocalLibraryDialog() {
        Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B1120"));
        root.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("📚 مكتبتي");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20f);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title);
        Button close = new Button(this);
        close.setText("✕");
        close.setOnClickListener(v -> dlg.dismiss());
        top.addView(close);
        root.addView(top);

        EditText search = new EditText(this);
        search.setHint("🔍 بحث فوري...");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.GRAY);
        root.addView(search);

        LinearLayout tabs = new LinearLayout(this);
        final String[] filterType = {"ALL"};
        String[] tabLabels = {"الكل", "PDF", "صوت", "فيديو"};
        String[] tabVals = {"ALL", "PDF", "AUDIO", "VIDEO"};

        ScrollView sv = new ScrollView(this);
        LinearLayout listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        sv.addView(listBox);

        Runnable refreshList = () -> {
            listBox.removeAllViews();
            File[] files = getVaultDir().listFiles();
            if (files == null || files.length == 0) {
                TextView empty = new TextView(this);
                empty.setText("لا توجد ملفات محفوظة بعد.");
                empty.setTextColor(Color.LTGRAY);
                empty.setPadding(dp(10), dp(20), dp(10), dp(20));
                listBox.addView(empty);
                return;
            }
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            for (File f : files) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (!q.isEmpty() && !n.contains(q)) continue;
                boolean isPdf = n.endsWith(".pdf");
                boolean isAudio = n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a");
                boolean isVideo = n.endsWith(".mp4") || n.endsWith(".webm");
                if ("PDF".equals(filterType[0]) && !isPdf) continue;
                if ("AUDIO".equals(filterType[0]) && !isAudio) continue;
                if ("VIDEO".equals(filterType[0]) && !isVideo) continue;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setBackgroundColor(Color.parseColor("#1E293B"));
                row.setPadding(dp(12), dp(10), dp(12), dp(10));
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = dp(8);
                row.setLayoutParams(rlp);

                TextView nameTv = new TextView(this);
                nameTv.setText((isPdf ? "📄 " : (isAudio ? "🎵 " : (isVideo ? "🎬 " : "📁 "))) + f.getName());
                nameTv.setTextColor(Color.WHITE);
                nameTv.setTextSize(15f);
                nameTv.setOnClickListener(v -> {
                    if (isPdf) openPdfViewer(f);
                    else if (isAudio) openAudioPlayer(f);
                    else if (isVideo) openVideoPlayer(f);
                    else shareVaultFile(f);
                });
                row.addView(nameTv);

                LinearLayout acts = new LinearLayout(this);
                Button bRen = new Button(this); bRen.setText("إعادة تسمية");
                bRen.setOnClickListener(v -> {
                    EditText et = new EditText(this);
                    et.setText(f.getName());
                    new AlertDialog.Builder(this).setTitle("إعادة تسمية").setView(et)
                            .setPositiveButton("حفظ", (d1, w) -> {
                                File nf = new File(f.getParentFile(), et.getText().toString().trim());
                                if (f.renameTo(nf)) listBox.post(() -> {});
                                dlg.dismiss(); showLocalLibraryDialog();
                            }).setNegativeButton("إلغاء", null).show();
                });
                Button bShare = new Button(this); bShare.setText("مشاركة");
                bShare.setOnClickListener(v -> shareVaultFile(f));
                Button bPub = new Button(this); bPub.setText("نشر للمنصة");
                bPub.setOnClickListener(v -> {
                    dlg.dismiss();
                    Toast.makeText(this, "يمكنك اختيار الملف الآن للرفع على المنصة", Toast.LENGTH_SHORT).show();
                });
                acts.addView(bRen); acts.addView(bShare); acts.addView(bPub);
                row.addView(acts);
                listBox.addView(row);
            }
        };

        for (int i = 0; i < tabLabels.length; i++) {
            final int idx = i;
            Button tb = new Button(this);
            tb.setText(tabLabels[i]);
            tb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tb.setOnClickListener(v -> { filterType[0] = tabVals[idx]; refreshList.run(); });
            tabs.addView(tb);
        }
        root.addView(tabs);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { refreshList.run(); }
            public void afterTextChanged(Editable s) {}
        });
        refreshList.run();
        dlg.setContentView(root);
        dlg.show();
    }

    private void shareVaultFile(File f) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "مشاركة الملف"));
        } catch (Exception e) {
            Toast.makeText(this, "تعذر مشاركة الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdfViewer(File file) {
        Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        Window w = dlg.getWindow();
        if (w != null) w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        final int[] pageIdx = {sp.getInt("pdf_page_" + file.getName(), 0)};
        final boolean[] night = {false};

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout bar = new LinearLayout(this);
        TextView info = new TextView(this);
        info.setTextColor(Color.WHITE);
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);

        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);
            int total = renderer.getPageCount();
            Runnable renderPage = () -> {
                if (pageIdx[0] < 0) pageIdx[0] = 0;
                if (pageIdx[0] >= total) pageIdx[0] = Math.max(0, total - 1);
                sp.edit().putInt("pdf_page_" + file.getName(), pageIdx[0]).apply();
                PdfRenderer.Page page = renderer.openPage(pageIdx[0]);
                Bitmap bmp = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                bmp.eraseColor(Color.WHITE);
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                img.setImageBitmap(bmp);
                if (night[0]) {
                    ColorMatrix cm = new ColorMatrix(new float[]{
                            -1,0,0,0,255, 0,-1,0,0,255, 0,0,-1,0,255, 0,0,0,1,0 });
                    img.setColorFilter(new ColorMatrixColorFilter(cm));
                } else {
                    img.clearColorFilter();
                }
                info.setText((pageIdx[0] + 1) + "/" + total);
            };
            Button prev = new Button(this); prev.setText("◀"); prev.setOnClickListener(v -> { pageIdx[0]--; renderPage.run(); });
            Button next = new Button(this); next.setText("▶"); next.setOnClickListener(v -> { pageIdx[0]++; renderPage.run(); });
            Button nightBtn = new Button(this); nightBtn.setText("🌙"); nightBtn.setOnClickListener(v -> { night[0] = !night[0]; renderPage.run(); });
            Button jumpBtn = new Button(this); jumpBtn.setText("انتقال");
            jumpBtn.setOnClickListener(v -> {
                EditText et = new EditText(this);
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                new AlertDialog.Builder(this).setTitle("القفز لصفحة (1-" + total + ")").setView(et)
                        .setPositiveButton("انتقال", (d, x) -> {
                            try { pageIdx[0] = Integer.parseInt(et.getText().toString()) - 1; renderPage.run(); } catch (Exception ignored) {}
                        }).show();
            });
            Button noteBtn = new Button(this); noteBtn.setText("📝");
            noteBtn.setOnClickListener(v -> {
                EditText et = new EditText(this);
                et.setText(sp.getString("pdf_note_" + file.getName(), ""));
                new AlertDialog.Builder(this).setTitle("ملاحظة على الملف").setView(et)
                        .setPositiveButton("حفظ", (d, x) -> sp.edit().putString("pdf_note_" + file.getName(), et.getText().toString()).apply()).show();
            });
            Button closeBtn = new Button(this); closeBtn.setText("✕");
            closeBtn.setOnClickListener(v -> { try { renderer.close(); pfd.close(); } catch (Exception ignored) {} dlg.dismiss(); });

            bar.addView(prev); bar.addView(info); bar.addView(next); bar.addView(nightBtn); bar.addView(jumpBtn); bar.addView(noteBtn); bar.addView(closeBtn);
            root.addView(bar);
            root.addView(img, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            renderPage.run();
            dlg.setContentView(root);
            dlg.show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح ملف PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAudioPlayer(File file) {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mp.setDataSource(file.getAbsolutePath());
            mp.prepare();
            int pos = sp.getInt("audio_pos_" + file.getName(), 0);
            if (pos > 0 && pos < mp.getDuration()) mp.seekTo(pos);
            mp.start();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تشغيل الصوت", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        final float[] spd = {1.0f};
        Button toggle = new Button(this); toggle.setText("⏸️ إيقاف / تشغيل");
        toggle.setOnClickListener(v -> { if (mp.isPlaying()) mp.pause(); else mp.start(); });
        Button speedBtn = new Button(this); speedBtn.setText("السرعة: 1.0x");
        speedBtn.setOnClickListener(v -> {
            spd[0] = spd[0] == 1.0f ? 1.25f : (spd[0] == 1.25f ? 1.5f : 1.0f);
            speedBtn.setText("السرعة: " + spd[0] + "x");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PlaybackParams pp = mp.getPlaybackParams();
                pp.setSpeed(spd[0]);
                mp.setPlaybackParams(pp);
            }
        });
        Button t15 = new Button(this); t15.setText("⏱️ مؤقت 15 دقيقة");
        t15.setOnClickListener(v -> { mainHandler.postDelayed(() -> { if (mp.isPlaying()) mp.pause(); }, 15 * 60 * 1000L); Toast.makeText(this, "تم ضبط مؤقت 15د", Toast.LENGTH_SHORT).show(); });
        Button t30 = new Button(this); t30.setText("⏱️ مؤقت 30 دقيقة");
        t30.setOnClickListener(v -> { mainHandler.postDelayed(() -> { if (mp.isPlaying()) mp.pause(); }, 30 * 60 * 1000L); Toast.makeText(this, "تم ضبط مؤقت 30د", Toast.LENGTH_SHORT).show(); });
        box.addView(toggle); box.addView(speedBtn); box.addView(t15); box.addView(t30);

        new AlertDialog.Builder(this).setTitle("🎵 " + file.getName()).setView(box)
                .setOnDismissListener(d -> {
                    try { sp.edit().putInt("audio_pos_" + file.getName(), mp.getCurrentPosition()).apply(); mp.release(); } catch (Exception ignored) {}
                }).setPositiveButton("إغلاق", null).show();
    }

    private void openVideoPlayer(File file) {
        Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(Color.BLACK);
        VideoView vv = new VideoView(this);
        vv.setVideoPath(file.getAbsolutePath());
        MediaController mc = new MediaController(this);
        mc.setAnchorView(vv);
        vv.setMediaController(mc);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        box.addView(vv, lp);
        Button close = new Button(this);
        close.setText("✕");
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.TOP | Gravity.END;
        close.setOnClickListener(v -> dlg.dismiss());
        box.addView(close, clp);
        dlg.setContentView(box);
        dlg.show();
        vv.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE) {
            if (filePathCallback != null) {
                Uri[] res = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                if (res != null && res.length > 0) playSuccessChime(); filePathCallback.onReceiveValue(res);
                filePathCallback = null;
            }
        } else if (requestCode == REQ_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (res != null && !res.isEmpty()) {
                String text = res.get(0);
                webView.evaluateJavascript("try{var el=document.activeElement;if(el&&(el.tagName==='INPUT'||el.tagName==='TEXTAREA')){el.value=" + JSONObject.quote(text) + ";el.dispatchEvent(new Event('input',{bubbles:true}));}}catch(e){}", null);
            }
        }
    }

    @Override
    public void onBackPressed() { handleSmartBack(); } private void handleSmartBack() { if (popupWebView != null) { if (popupWebView.canGoBack()) { popupWebView.goBack(); return; } closeAuthPopupView(); return; }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        if (doubleBackToExit) {
            finish();
            return;
        }
        doubleBackToExit = true;
        Toast.makeText(this, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
        mainHandler.postDelayed(() -> doubleBackToExit = false, 2000);
    }

    private class AndroidVaultBridge {
        @JavascriptInterface
        public void saveDataUrl(String dataUrl, String name) {
            saveDataUrlToDevice(dataUrl, name);
        }
    }

    private void closeAuthPopupView() { if (popupWebView != null) { try { if (rootLayout != null) rootLayout.removeView(popupWebView); popupWebView.stopLoading(); popupWebView.destroy(); } catch (Exception ignored) {} popupWebView = null; } } private void playToneSequence(final double[] freqs, final int noteMs, final double gain) { new Thread(() -> { AudioTrack track = null; try { int sr = 22050, spn = (sr * noteMs) / 1000, tot = spn * freqs.length; short[] buf = new short[tot]; for (int n = 0; n < freqs.length; n++) { double f = freqs[n]; int off = n * spn, fade = Math.min(200, spn / 4); for (int i = 0; i < spn; i++) { double env = (i < fade) ? (double) i / fade : (i > spn - fade ? (double) (spn - i) / fade : 1.0); buf[off + i] = (short) (Math.sin(2.0 * Math.PI * i * f / sr) * Short.MAX_VALUE * gain * env); } } int minB = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT); track = new AudioTrack(AudioManager.STREAM_MUSIC, sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minB, tot * 2), AudioTrack.MODE_STATIC); track.write(buf, 0, tot); track.play(); Thread.sleep((long) noteMs * freqs.length + 60); } catch (Exception ignored) {} finally { if (track != null) { try { track.stop(); } catch (Exception ignored) {} try { track.release(); } catch (Exception ignored) {} } } }).start(); } private void playIntroChime() { playToneSequence(new double[]{523.25, 659.25, 783.99, 1046.50}, 140, 0.25); } private void playClickFeedback() { playToneSequence(new double[]{880.0}, 45, 0.20); } private void playSuccessChime() { playToneSequence(new double[]{587.33, 880.0, 1174.66}, 110, 0.25); } private class AndroidSessionBridge { @JavascriptInterface public void closeAuthPopup() { runOnUiThread(() -> closeAuthPopupView()); }
        @JavascriptInterface
        public void syncSession(String json) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_SESSION_JSON, json).apply();
        }
    }

    private static class MotionParticlesView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] x = new float[28];
        private final float[] y = new float[28];
        private final float[] vy = new float[28];
        private final Random rnd = new Random();
        private boolean init = false;

        MotionParticlesView(Context ctx) {
            super(ctx);
            paint.setColor(Color.parseColor("#5538BDF8"));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            if (!init) {
                for (int i = 0; i < x.length; i++) {
                    x[i] = rnd.nextFloat() * w;
                    y[i] = rnd.nextFloat() * h;
                    vy[i] = 1.2f + rnd.nextFloat() * 2.5f;
                }
                init = true;
            }
            for (int i = 0; i < x.length; i++) {
                canvas.drawCircle(x[i], y[i], 4f + (i % 4) * 2f, paint);
                y[i] -= vy[i];
                if (y[i] < -10) { y[i] = h + 10; x[i] = rnd.nextFloat() * w; }
            }
            postInvalidateDelayed(30);
        }
    }
}
