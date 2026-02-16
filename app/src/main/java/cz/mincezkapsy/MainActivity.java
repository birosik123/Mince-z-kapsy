package cz.mincezkapsy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1;

    private static final int C_BG      = 0xFF0D0D0D;
    private static final int C_CARD    = 0xFF1A1A1A;
    private static final int C_BORDER  = 0xFF2E2E2E;
    private static final int C_TEXT    = 0xFFFFFFFF;
    private static final int C_SUB     = 0xFF9A9A9A;
    private static final int C_BLUE    = 0xFF3B82F6;
    private static final int C_RED     = 0xFFEF4444;

    // JavaScript interface pro export souboru
    public class AndroidBridge {
        @JavascriptInterface
        public void saveFile(String filename, String content) {
            try {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, filename);
                FileWriter fw = new FileWriter(file);
                fw.write(content);
                fw.close();
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "✅ Záloha uložena do Stažené soubory:\n" + filename,
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "❌ Chyba uložení: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
            }
        }
    }

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

        // Registruj JavaScript bridge pro export
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

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
            public boolean onJsAlert(WebView view, String url, String msg, final JsResult result) {
                showDialog("ℹ️", msg, "OK", null, false, result, null);
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String msg, final JsResult result) {
                boolean danger = msg.contains("smazat") || msg.contains("Smazat")
                              || msg.contains("nevratn") || msg.contains("data?")
                              || msg.contains("úvěr") || msg.contains("transakci");
                showDialog(danger ? "⚠️" : "❓", msg, "Ano", "Ne", danger, result, null);
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String msg,
                                       String def, final JsPromptResult result) {
                showDialog("✏️", msg, "OK", "Zrušit", false, null, result);
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage m) { return true; }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundedBg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable borderedBg(int color, int borderColor, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setStroke(dp(1), borderColor);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private void showDialog(String icon, String message,
                             String yesText, String noText,
                             boolean danger,
                             final JsResult alertResult,
                             final JsPromptResult promptResult) {
        runOnUiThread(() -> {
            // Hlavní container
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackground(borderedBg(C_BG, C_BORDER, 20));
            root.setPadding(dp(24), dp(24), dp(24), dp(20));

            // Ikona
            TextView ico = new TextView(this);
            ico.setText(icon);
            ico.setTextSize(32);
            ico.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams icoP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            icoP.setMargins(0, 0, 0, dp(12));
            ico.setLayoutParams(icoP);
            root.addView(ico);

            // Zpráva
            TextView msg = new TextView(this);
            msg.setText(message);
            msg.setTextColor(C_SUB);
            msg.setTextSize(15);
            msg.setGravity(Gravity.CENTER);
            msg.setLineSpacing(0, 1.4f);
            LinearLayout.LayoutParams msgP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            msgP.setMargins(0, 0, 0, dp(20));
            msg.setLayoutParams(msgP);
            root.addView(msg);

            // Input pole (jen pro prompt)
            EditText input = null;
            if (promptResult != null) {
                input = new EditText(this);
                input.setTextColor(C_TEXT);
                input.setHintTextColor(C_SUB);
                input.setBackground(borderedBg(C_CARD, C_BORDER, 12));
                input.setPadding(dp(14), dp(13), dp(14), dp(13));
                LinearLayout.LayoutParams inpP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                inpP.setMargins(0, 0, 0, dp(16));
                input.setLayoutParams(inpP);
                root.addView(input);
            }

            // Řada tlačítek
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            final EditText finalInput = input;

            // Tlačítko Ne/Zrušit (pokud je)
            if (noText != null) {
                Button btnNo = makeBtn(noText, C_CARD, C_SUB, C_BORDER);
                LinearLayout.LayoutParams noP = new LinearLayout.LayoutParams(0, dp(50), 1f);
                noP.setMargins(0, 0, dp(6), 0);
                btnNo.setLayoutParams(noP);
                btnRow.addView(btnNo);

                btnNo.setOnClickListener(v -> {
                    if (alertResult != null) alertResult.cancel();
                    if (promptResult != null) promptResult.cancel();
                });
            }

            // Tlačítko Ano/OK
            Button btnYes = makeBtn(yesText, danger ? C_RED : C_BLUE, C_TEXT, 0);
            LinearLayout.LayoutParams yesP = new LinearLayout.LayoutParams(0, dp(50), 1f);
            yesP.setMargins(noText != null ? dp(6) : 0, 0, 0, 0);
            btnYes.setLayoutParams(yesP);
            btnRow.addView(btnYes);

            btnYes.setOnClickListener(v -> {
                if (promptResult != null) {
                    promptResult.confirm(finalInput != null ? finalInput.getText().toString() : "");
                } else if (alertResult != null) {
                    alertResult.confirm();
                }
            });

            root.addView(btnRow);

            // Zobrazit dialog
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .setCancelable(false)
                .create();

            // Přepojit tlačítka na zavření dialogu
            btnYes.setOnClickListener(v -> {
                dialog.dismiss();
                if (promptResult != null) {
                    promptResult.confirm(finalInput != null ? finalInput.getText().toString() : "");
                } else if (alertResult != null) {
                    alertResult.confirm();
                }
            });
            if (noText != null) {
                ((Button) btnRow.getChildAt(0)).setOnClickListener(v -> {
                    dialog.dismiss();
                    if (alertResult != null) alertResult.cancel();
                    if (promptResult != null) promptResult.cancel();
                });
            }

            dialog.show();
            if (dialog.getWindow() != null) {
                // Odstranit VŠECHNA výchozí Android pozadí = žádný hranatý rámeček
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getDecorView().setBackground(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
                // Šířka 90% obrazovky
                int w = (int)(getResources().getDisplayMetrics().widthPixels * 0.90f);
                dialog.getWindow().setLayout(w, WindowManager.LayoutParams.WRAP_CONTENT);
            }

            if (finalInput != null) finalInput.requestFocus();
        });
    }

    private Button makeBtn(String text, int bgColor, int textColor, int borderColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(15);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);
        if (borderColor != 0) {
            btn.setBackground(borderedBg(bgColor, borderColor, 12));
        } else {
            btn.setBackground(roundedBg(bgColor, 12));
        }
        btn.setPadding(dp(8), 0, dp(8), 0);
        return btn;
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
