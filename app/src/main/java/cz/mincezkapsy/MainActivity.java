package cz.mincezkapsy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.content.res.Configuration;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1;

    // Barvy odpovídající tmavému tématu aplikace
    private static final int COLOR_BG       = 0xFF121212;
    private static final int COLOR_SURFACE  = 0xFF1E1E1E;
    private static final int COLOR_BORDER   = 0xFF333333;
    private static final int COLOR_TEXT     = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT  = 0xFFA0A0A0;
    private static final int COLOR_BLUE     = 0xFF3B82F6;
    private static final int COLOR_RED      = 0xFFEF4444;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb,
                                              FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(Intent.createChooser(i, "Vyberte soubor"), FILE_CHOOSER_REQUEST);
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                showStyledAlert(message, result);
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                showStyledConfirm(message, result);
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message,
                                       String defaultValue, final JsPromptResult result) {
                showStyledPrompt(message, defaultValue, result);
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage msg) {
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /** Vytvoří styled dialog s tmavým pozadím odpovídajícím aplikaci */
    private AlertDialog.Builder styledBuilder() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        return b;
    }

    private void showStyledAlert(String message, final JsResult result) {
        // Vytvoříme custom view
        LinearLayout root = buildDialogRoot(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(root)
            .setCancelable(false)
            .create();

        // Tlačítko OK
        Button ok = buildButton("OK", COLOR_BLUE, false);
        ok.setOnClickListener(v -> { dialog.dismiss(); result.confirm(); });
        ((LinearLayout) root.getChildAt(root.getChildCount() - 1)).addView(ok);

        showDarkDialog(dialog);
    }

    private void showStyledConfirm(String message, final JsResult result) {
        boolean isDanger = message.contains("smazat") || message.contains("Smazat")
                        || message.contains("nevratn") || message.contains("data?");

        LinearLayout root = buildDialogRoot(message);
        LinearLayout btnRow = (LinearLayout) root.getChildAt(root.getChildCount() - 1);

        Button cancel = buildButton("Ne", COLOR_SURFACE, true);
        Button ok = buildButton("Ano", isDanger ? COLOR_RED : COLOR_BLUE, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(root)
            .setCancelable(false)
            .create();

        cancel.setOnClickListener(v -> { dialog.dismiss(); result.cancel(); });
        ok.setOnClickListener(v -> { dialog.dismiss(); result.confirm(); });

        btnRow.addView(cancel);
        btnRow.addView(ok);

        showDarkDialog(dialog);
    }

    private void showStyledPrompt(String message, String defaultValue, final JsPromptResult result) {
        LinearLayout root = buildDialogRoot(message);

        // Input pole
        EditText input = new EditText(this);
        input.setText(defaultValue != null ? defaultValue : "");
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(COLOR_SUBTEXT);
        input.setBackgroundDrawable(buildBorderedBg());
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, dp(14));
        input.setLayoutParams(inputParams);

        // Vložit input před btn row
        root.addView(input, root.getChildCount() - 1);

        LinearLayout btnRow = (LinearLayout) root.getChildAt(root.getChildCount() - 1);
        Button cancel = buildButton("Zrušit", COLOR_SURFACE, true);
        Button ok = buildButton("OK", COLOR_BLUE, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(root)
            .setCancelable(false)
            .create();

        cancel.setOnClickListener(v -> { dialog.dismiss(); result.cancel(); });
        ok.setOnClickListener(v -> {
            dialog.dismiss();
            result.confirm(input.getText().toString());
        });

        btnRow.addView(cancel);
        btnRow.addView(ok);

        showDarkDialog(dialog);
        input.requestFocus();
    }

    private LinearLayout buildDialogRoot(String message) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));

        // Zpráva
        TextView msg = new TextView(this);
        msg.setText(message);
        msg.setTextColor(COLOR_SUBTEXT);
        msg.setTextSize(15);
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        msgParams.setMargins(0, 0, 0, dp(20));
        msg.setLayoutParams(msgParams);
        root.addView(msg);

        // Řada tlačítek
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnRow.setLayoutParams(rowParams);
        root.addView(btnRow);

        return root;
    }

    private Button buildButton(String text, int bgColor, boolean isSecondary) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(isSecondary ? COLOR_SUBTEXT : COLOR_TEXT);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setBackgroundColor(bgColor);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            0, dp(46), 1f
        );
        p.setMargins(dp(4), 0, dp(4), 0);
        btn.setLayoutParams(p);
        return btn;
    }

    private android.graphics.drawable.GradientDrawable buildBorderedBg() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(COLOR_SURFACE);
        d.setStroke(dp(1), COLOR_BORDER);
        d.setCornerRadius(dp(10));
        return d;
    }

    private void showDarkDialog(AlertDialog dialog) {
        dialog.show();
        // Tmavé pozadí dialogu
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(COLOR_BG));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                String ds = data.getDataString();
                if (ds != null) results = new Uri[]{Uri.parse(ds)};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onPause() { super.onPause(); webView.onPause(); }
    @Override protected void onResume() { super.onResume(); webView.onResume(); }
    @Override protected void onDestroy() { super.onDestroy(); webView.destroy(); }
    @Override public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
