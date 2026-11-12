package com.mobin.htmlrunner;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1;
    private static final int PICK_FOLDER = 2;
    private static final String PROJECT_HOST = "htmlrunner.local";
    private static final String PROJECT_URL = "https://" + PROJECT_HOST + "/index.html";
    private static final String PREFS = "html_runner";
    private static final String PROJECT_URI = "project_tree_uri";

    private WebView web;
    private Uri projectTree;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);

        Button folderButton = new Button(this);
        folderButton.setText("📁 پوشه پروژه");
        folderButton.setOnClickListener(v -> pickProjectFolder());

        Button fileButton = new Button(this);
        fileButton.setText("📄 فایل HTML");
        fileButton.setOnClickListener(v -> pickHtmlFile());

        Button reloadButton = new Button(this);
        reloadButton.setText("⟳");
        reloadButton.setContentDescription("بارگذاری مجدد");
        reloadButton.setOnClickListener(v -> {
            if (projectTree != null) web.loadUrl(PROJECT_URL);
            else web.reload();
        });

        toolbar.addView(folderButton, new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(fileButton, new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(reloadButton, new LinearLayout.LayoutParams(-2, -2));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));

        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        web.setWebViewClient(new ProjectWebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);

        Uri incoming = getIntent().getData();
        if (incoming != null) {
            loadSingleHtmlFile(incoming);
        } else {
            restoreProject();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getData() != null) loadSingleHtmlFile(intent.getData());
    }

    private void pickProjectFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_FOLDER);
    }

    private void pickHtmlFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/html");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri selected = data.getData();
        if (requestCode == PICK_FOLDER) {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(selected, flags);
                projectTree = selected;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(PROJECT_URI, selected.toString()).apply();
                web.loadUrl(PROJECT_URL);
            } catch (SecurityException e) {
                Toast.makeText(this, "دسترسی به پوشه ذخیره نشد", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PICK_FILE) {
            try {
                getContentResolver().takePersistableUriPermission(selected,
                        data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
                // Some document providers do not offer persistable permissions.
            }
            loadSingleHtmlFile(selected);
        }
    }

    private void restoreProject() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(PROJECT_URI, null);
        if (saved != null) {
            try {
                projectTree = Uri.parse(saved);
                web.loadUrl(PROJECT_URL);
                return;
            } catch (Exception ignored) {
                projectTree = null;
            }
        }
        showHome();
    }

    private void loadSingleHtmlFile(Uri uri) {
        projectTree = null;
        web.loadUrl(uri.toString());
    }

    private WebResourceResponse serveProjectFile(Uri requestUri) {
        if (projectTree == null) return null;

        List<String> segments = requestUri.getPathSegments();
        if (segments.isEmpty()) segments = java.util.Collections.singletonList("index.html");

        Uri current;
        try {
            String rootId = DocumentsContract.getTreeDocumentId(projectTree);
            current = DocumentsContract.buildDocumentUriUsingTree(projectTree, rootId);

            for (String segment : segments) {
                if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                        || segment.contains("/") || segment.contains("\\\\")) {
                    return notFound();
                }
                current = DocumentsContract.findDocumentUri(getContentResolver(), current, segment);
                if (current == null) return notFound();
            }

            InputStream stream = getContentResolver().openInputStream(current);
            if (stream == null) return notFound();

            String mime = getContentResolver().getType(current);
            if (mime == null) mime = mimeFromName(segments.get(segments.size() - 1));
            boolean text = mime.startsWith("text/") || mime.contains("javascript")
                    || mime.contains("json") || mime.contains("xml") || mime.contains("svg");
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-store");
            headers.put("Access-Control-Allow-Origin", "*");
            return new WebResourceResponse(mime, text ? "UTF-8" : null,
                    200, "OK", headers, stream);
        } catch (Exception e) {
            return notFound();
        }
    }

    private String mimeFromName(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private WebResourceResponse notFound() {
        return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found",
                new HashMap<>(), new java.io.ByteArrayInputStream(
                "Project file not found".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private void showHome() {
        String html = "<!doctype html><html lang='fa' dir='rtl'><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<body style='font-family:sans-serif;background:#101827;color:#f8fafc;"
                + "min-height:90vh;display:grid;place-items:center;text-align:center;padding:24px'>"
                + "<main><div style='font-size:64px'>🌐</div><h2>HTML Runner</h2>"
                + "<p>برای اجرای یک پروژه وب، پوشه‌ای را انتخاب کن که فایل index.html دارد.</p>"
                + "<p style='color:#94a3b8'>فایل‌های CSS، JavaScript و تصاویر داخل همان پوشه نیز بارگذاری می‌شوند.</p>"
                + "</main></body></html>";
        web.loadDataWithBaseURL("https://" + PROJECT_HOST + "/", html,
                "text/html", "UTF-8", null);
    }

    private class ProjectWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (PROJECT_HOST.equals(uri.getHost())) return serveProjectFile(uri);
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (PROJECT_HOST.equals(uri.getHost())) return false;
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
