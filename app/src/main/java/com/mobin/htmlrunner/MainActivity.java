package com.mobin.htmlrunner;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int PICK = 1;
    private static final String BASE = "https://localhost/";
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        Button btn = new Button(this);
        btn.setText("📂 باز کردن فایل HTML");
        btn.setOnClickListener(v -> pick());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        lp.setMargins(24, 24, 24, 64);
        root.addView(btn, lp);
        setContentView(root);

        Uri u = getIntent().getData();
        if (u != null) load(u); else home();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i.getData() != null) load(i.getData());
    }

    private void pick() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK && res == RESULT_OK && data != null && data.getData() != null) load(data.getData());
    }

    private void load(Uri u) {
        try (InputStream in = getContentResolver().openInputStream(u)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            web.loadDataWithBaseURL(BASE, out.toString("UTF-8"), "text/html", "UTF-8", null);
        } catch (Exception e) {
            Toast.makeText(this, "فایل باز نشد", Toast.LENGTH_LONG).show();
        }
    }

    private void home() {
        String h = "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<body dir='rtl' style='font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:90vh;margin:0;text-align:center;background:#f6f7fb;color:#1b1e2b'>"
            + "<div><div style='font-size:64px'>🌐</div><h2>HTML Runner</h2>"
            + "<p>دکمه پایین رو بزن و یه فایل HTML انتخاب کن</p></div>";
        web.loadDataWithBaseURL(BASE, h, "text/html", "UTF-8", null);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
