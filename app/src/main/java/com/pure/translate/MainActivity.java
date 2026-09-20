package com.pure.translate;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * الشاشة الرئيسية - زر واحد بس "بدء الترجمة".
 * يضغط عليه المستخدم مرة وحدة، والتطبيق يسلسل الصلاحيتين لحاله:
 * أول يطلب صلاحية النافذة العائمة (لو ناقصة)، وبعد ما يرجع منها يطلب صلاحية تسجيل الشاشة تلقائياً،
 * وبعدها يشغل الخدمة ويسكر نفسه. ما يحتاج المستخدم يضغط زرين.
 */
public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;
    private static final int REQ_CAPTURE = 1002;

    private MediaProjectionManager projectionManager;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        statusText = findViewById(R.id.statusText);

        Button btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> startFlow());
    }

    private void startFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            statusText.setText("الخطوة ١ من ٢: فعّل صلاحية النافذة العائمة ثم ارجع");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_OVERLAY);
        } else {
            requestScreenCapture();
        }
    }

    private void requestScreenCapture() {
        statusText.setText("الخطوة ٢ من ٢: وافق على تسجيل الشاشة عشان الترجمة تشتغل");
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                requestScreenCapture();
            } else {
                statusText.setText("لازم توافق على صلاحية النافذة العائمة عشان نكمل");
            }
            return;
        }

        if (requestCode == REQ_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(this, OverlayService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(this, "بدأت نافذة بيور - اضغط هوم وشوفها فوق أي تطبيق", Toast.LENGTH_LONG).show();
                finish();
            } else {
                statusText.setText("لازم توافق على تسجيل الشاشة عشان الترجمة تشتغل");
            }
        }
    }
}
