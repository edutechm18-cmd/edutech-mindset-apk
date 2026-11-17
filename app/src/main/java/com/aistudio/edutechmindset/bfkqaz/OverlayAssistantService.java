package com.aistudio.edutechmindset.bfkqaz;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class OverlayAssistantService extends Service {

    public static final String ACTION_STOP = "com.aistudio.edutechmindset.bfkqaz.STOP_OVERLAY";
public static final String ACTION_HIDE = "HIDE_OVERLAY";
public static final String ACTION_SHOW = "SHOW_OVERLAY";
    private static final String CHANNEL_ID = "edutech_overlay_channel";
    private static final int NOTIFICATION_ID = 2026;

    private WindowManager windowManager;
    private FrameLayout floatingButton;
    private WindowManager.LayoutParams params;
    private ObjectAnimator pulseAnimator;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        setupFloatingBubble();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
if (intent != null && ACTION_HIDE.equals(intent.getAction())) {
    if (floatingButton != null) floatingButton.setVisibility(View.GONE);
} else if (intent != null && ACTION_SHOW.equals(intent.getAction())) {
    if (floatingButton != null) floatingButton.setVisibility(View.VISIBLE);
}
        return START_STICKY;
    }

    private void startForegroundNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "المساعد التعليمي العائم",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("تشغيل أيقونة المساعد التعليمي فوق التطبيقات");
            nm.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.putExtra("OPEN_VOICE_ASSISTANT", true);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int pendingFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingOpen = PendingIntent.getActivity(this, 0, openIntent, pendingFlags);

        Intent stopIntent = new Intent(this, OverlayAssistantService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingStop = PendingIntent.getService(this, 1, stopIntent, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setContentTitle("EduTech Mindset")
                .setContentText("🤖 مساعدي العائم نشط - اضغط للتحدث")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingOpen)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", pendingStop);

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void setupFloatingBubble() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 40;
        params.y = 320;

       floatingButton = new FrameLayout(this);
int iconSize = dpToPx(46);
int pad = dpToPx(3);
floatingButton.setPadding(pad, pad, pad, pad);
ImageView iconView = new ImageView(this);
iconView.setImageResource(R.mipmap.ic_launcher);
iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iconView.setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View view, Outline outline) { outline.setOval(0, 0, view.getWidth(), view.getHeight()); } });
        iconView.setClipToOutline(true);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(iconSize, iconSize);
iconLp.gravity = Gravity.CENTER;
floatingButton.addView(iconView, iconLp);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#0F172A"), Color.parseColor("#1D4ED8")}
        );
        bg.setCornerRadius(dpToPx(28));
        bg.setStroke(dpToPx(2), Color.parseColor("#38BDF8"));
        floatingButton.setBackground(bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            floatingButton.setElevation(dpToPx(10));
        }

        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                floatingButton,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.07f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.07f)
        );
        pulseAnimator.setDuration(1100);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.start();

        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isMoved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isMoved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(event.getRawX() - initialTouchX);
                        int dy = Math.round(event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isMoved = true;
                        }
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        if (windowManager != null && floatingButton.getWindowToken() != null) {
                            windowManager.updateViewLayout(floatingButton, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isMoved) {
                            triggerHaptic(v);
                            openVoiceAssistant();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingButton, params);
    }

    private void triggerHaptic(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(45);
            }
        }
    }

    private void openVoiceAssistant() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("OPEN_VOICE_ASSISTANT", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        if (windowManager != null && floatingButton != null && floatingButton.getWindowToken() != null) {
            windowManager.removeView(floatingButton);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
