package __APP_ID__;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends BridgeActivity {

  private static final boolean LAPTOP = __LAPTOP__;

  // Make the page look like a laptop screen
  private static final String VIEWPORT_JS =
    "(function(){var m=document.querySelector('meta[name=viewport]');"
    + "if(!m){m=document.createElement('meta');m.name='viewport';(document.head||document.documentElement).appendChild(m);}"
    + "m.setAttribute('content','width=1280');})();";

  // Blob downloads (itinerary PDF, calendar .ics) do not work in a WebView by default.
  // Route them to the native side, which saves them into Downloads.
  private static final String DOWNLOAD_JS =
    "(function(){if(window.__nomadDl)return;window.__nomadDl=true;"
    + "function send(href,name){fetch(href).then(function(r){return r.blob();}).then(function(b){"
    + "var fr=new FileReader();fr.onloadend=function(){var d=String(fr.result);"
    + "window.NomadNative.save(d.substring(d.indexOf(',')+1),b.type||'application/octet-stream',name||'nomad-file');};"
    + "fr.readAsDataURL(b);}).catch(function(e){window.NomadNative.fail(String(e));});}"
    + "var oc=HTMLAnchorElement.prototype.click;"
    + "HTMLAnchorElement.prototype.click=function(){"
    + "if(this.href&&this.href.indexOf('blob:')===0){send(this.href,this.download);return;}"
    + "return oc.apply(this,arguments);};"
    + "document.addEventListener('click',function(e){var a=e.target&&e.target.closest?e.target.closest('a'):null;"
    + "if(a&&a.href&&a.href.indexOf('blob:')===0){e.preventDefault();e.stopPropagation();send(a.href,a.download);}},true);"
    + "})();";

  private final Handler ui = new Handler(Looper.getMainLooper());
  private long lastBackPress = 0;
  private long updateDownloadId = -1;

  // When the update APK has finished downloading, open the system installer on it.
  private final BroadcastReceiver onDownloadDone = new BroadcastReceiver() {
    @Override
    public void onReceive(Context c, Intent i) {
      long id = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
      if (id == -1 || id != updateDownloadId) return;
      try {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = dm.getUriForDownloadedFile(id);
        if (uri == null) {
          Toast.makeText(MainActivity.this, "Update download failed. Please try again.", Toast.LENGTH_LONG).show();
          return;
        }
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(install);
      } catch (Exception e) {
        Toast.makeText(MainActivity.this, "Update downloaded. Open Downloads and tap it to install.", Toast.LENGTH_LONG).show();
      }
    }
  };

  private void startUpdateDownload(String url) {
    try {
      DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
      DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
      r.setTitle("NOMAD update");
      r.setDescription("Downloading the latest version");
      r.setMimeType("application/vnd.android.package-archive");
      r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
      r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "nomad-update-" + System.currentTimeMillis() + ".apk");
      updateDownloadId = dm.enqueue(r);
      Toast.makeText(this, "Downloading update... the installer opens when it finishes.", Toast.LENGTH_LONG).show();
    } catch (Exception e) {
      Toast.makeText(this, "Could not start the update download.", Toast.LENGTH_LONG).show();
    }
  }


  public class NomadNative {
    private final Context ctx;
    NomadNative(Context c) { ctx = c; }

    @JavascriptInterface
    public void save(String base64, String mime, String name) {
      try {
        byte[] data = Base64.decode(base64, Base64.DEFAULT);
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        Uri opened = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          ContentValues v = new ContentValues();
          v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
          v.put(MediaStore.Downloads.MIME_TYPE, mime);
          v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
          Uri uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
          if (uri == null) throw new Exception("Could not create file");
          try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) { os.write(data); }
          opened = uri;
        } else {
          File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
          if (!dir.exists()) dir.mkdirs();
          File f = new File(dir, safe);
          try (FileOutputStream os = new FileOutputStream(f)) { os.write(data); }
          opened = Uri.fromFile(f);
        }
        final Uri toOpen = opened;
        final String type = mime;
        ui.post(() -> {
          Toast.makeText(ctx, "Saved to Downloads: " + safe, Toast.LENGTH_LONG).show();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
              Intent i = new Intent(Intent.ACTION_VIEW);
              i.setDataAndType(toOpen, type);
              i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
              ctx.startActivity(i);
            } catch (Exception ignored) { /* no viewer installed; file is still saved */ }
          }
        });
      } catch (Exception e) {
        fail(String.valueOf(e.getMessage()));
      }
    }

    // Called by the login page after a successful login: tells the phone's autofill
    // service (Google Password Manager etc.) that the form was submitted, so it can ask
    // "Save password?".
    @JavascriptInterface
    public void loginSuccess() {
      ui.post(() -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          try {
            AutofillManager am = ctx.getSystemService(AutofillManager.class);
            if (am != null) am.commit();
          } catch (Exception ignored) { /* autofill not available */ }
        }
      });
    }

    // --- in-app updates: the web page compares these with the latest GitHub release ---
    @JavascriptInterface
    public int getVersionCode() {
      try {
        PackageInfo p = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        return p.versionCode;
      } catch (Exception e) {
        return 0;
      }
    }

    @JavascriptInterface
    public String getVariant() {
      return "__VARIANT__";
    }

    @JavascriptInterface
    public void downloadUpdate(String url) {
      // Only ever download from this project's own releases.
      if (url == null || !url.startsWith("https://github.com/sujaytumu/nomads/releases/download/")) {
        fail("Invalid update link");
        return;
      }
      ui.post(() -> startUpdateDownload(url));
    }

    @JavascriptInterface
    public void fail(String msg) {
      ui.post(() -> Toast.makeText(ctx, "Download failed: " + msg, Toast.LENGTH_LONG).show());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final WebView wv = getBridge().getWebView();
    WebSettings s = wv.getSettings();
    s.setDomStorageEnabled(true);
    s.setJavaScriptEnabled(true);
    s.setGeolocationEnabled(true);
    wv.addJavascriptInterface(new NomadNative(this), "NomadNative");
    ContextCompat.registerReceiver(this, onDownloadDone,
        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
    // BridgeActivity.onDestroy() is final, so clean up through the lifecycle instead.
    getLifecycle().addObserver(new DefaultLifecycleObserver() {
      @Override
      public void onDestroy(LifecycleOwner owner) {
        try { unregisterReceiver(onDownloadDone); } catch (Exception ignored) { /* not registered */ }
      }
    });
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      wv.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
    }

    // Back button: one step back inside the app. Only when there is nothing left to go back to,
    // ask for a second press before closing (so it never exits by accident).
    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        WebView w = getBridge().getWebView();
        if (w.canGoBack()) {
          w.goBack();
          return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastBackPress < 2000) {
          finish();
        } else {
          lastBackPress = now;
          Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
      }
    });

    if (LAPTOP) {
      s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
      s.setUseWideViewPort(true);
      s.setLoadWithOverviewMode(true);
      s.setSupportZoom(true);
      s.setBuiltInZoomControls(true);
      s.setDisplayZoomControls(false);
    }

    wv.setWebViewClient(new BridgeWebViewClient(getBridge()) {
      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        view.evaluateJavascript(DOWNLOAD_JS, null);
        if (LAPTOP) view.evaluateJavascript(VIEWPORT_JS, null);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        view.evaluateJavascript(DOWNLOAD_JS, null);
        if (LAPTOP) view.evaluateJavascript(VIEWPORT_JS, null);
      }
    });
  }
}
