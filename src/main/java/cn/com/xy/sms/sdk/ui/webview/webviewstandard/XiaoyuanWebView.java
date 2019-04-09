package cn.com.xy.sms.sdk.ui.webview.webviewstandard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ClientCertRequest;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import cn.com.xy.sms.sdk.ui.webview.R;
import cn.com.xy.sms.sdk.ui.webview.util.FileUtils;
import cn.com.xy.sms.sdk.ui.webview.util.SPUtil;

public class XiaoyuanWebView extends WebView {

    private static final String TAG = "XiaoyuanWebView";

    public interface Listener {
        /**
         * 页面加载前回调， 对应 setWebViewClient.
         * public void onPageStarted(WebView view, String url, Bitmap favicon)
         * @param url 加载的网页链接
         * @param favicon
         */
        void onPageStarted(String url, Bitmap favicon);

        /**
         * 页面加载进度回调，对应 setWebChromeClient.
         * public void onProgressChanged(WebView view, int newProgress)
         * @param view 当前 WebView
         * @param newProgress 加载进度 0~100
         */
        void onProgressChanged(WebView view, int newProgress);

        /**
         * 页面加载完毕回调, 对应 setWebViewClient.
         * @param url 加载网页的链接
         * public void onPageFinished(WebView view, String url)
         */
        void onPageFinished(String url);

        /**
         * 替换设置网页 title 回调, 对应 setWebChromeClient.
         * public void onReceivedTitle(WebView view, String title)
         * @param view 当前 WebView
         * @param title 网页标题, 对应的 <title/> 标签
         */
        void onReceivedTitle(WebView view, String title);

        /**
         * 页面加载异常回调， 对应 setWebViewClient.
         * public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
         * @param errorCode 错误编码
         * @param description 描述信息
         * @param failingUrl 错误 url
         */
        void onPageError(int errorCode, String description, String failingUrl);

        /**
         * 网页下载任务监听， 对应调用 WebView.setDownloadListener 实现回调。
         * @param url
         * @param suggestedFilename
         * @param mimeType
         * @param contentLength
         * @param contentDisposition
         * @param userAgent
         */
        void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent);
    }

    protected WeakReference<Activity> mActivity;
    protected Listener mListener;

    protected WebViewClient mCustomWebViewClient;
    protected WebChromeClient mCustomWebChromeClient;
    protected boolean mGeolocationEnabled;
    protected final Map<String, String> mHttpHeaders = new HashMap<String, String>();

    private static LinkedList<String> mFileName;
    private ValueCallback<Uri[]> mUploadMessage = null;
    // 手机相册
    private int TAKE_PHOTO_ALBUM = 1;
    // 相机照相
    private int TAKE_PHOTO_REQUEST_CODE = 2;
    // 文件管理器
    private int FILECHOOSER_RESULTCODE = 3;
    // 拍照存储的 uri 路径
    private Uri mImageUri;

    public XiaoyuanWebView(Context context) {
        super(context);
        init(context);
    }

    public XiaoyuanWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public XiaoyuanWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 增加注册系列回调， 具体回调函数查看 XiaoyuanWebView.Listener
     * @param activity 上下文， 一般是包含 WebView 的Activity
     * @param listener XiaoyuanWebView.Listener
     */
    public void setListener(final Activity activity, final Listener listener) {
        if (activity != null) {
            mActivity = new WeakReference<>(activity);
        } else {
            mActivity = null;
        }

        mListener = listener;
    }

    @Override
    public void setWebViewClient(final WebViewClient client) {
        mCustomWebViewClient = client;
    }

    @Override
    public void setWebChromeClient(final WebChromeClient client) {
        mCustomWebChromeClient = client;
    }

    /**
     * 是否允许 WebView 不用提示用户直接默认获取当前地理位置。
     * @param enabled ‘TRUE’ 不采用弹窗提示用户是否允许地理位置权限。
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void setGeolocationEnabled(final boolean enabled) {
        if (enabled) {
            getSettings().setJavaScriptEnabled(true);
            getSettings().setGeolocationEnabled(true);
            setGeolocationDatabasePath();
        }

        mGeolocationEnabled = enabled;
    }

    @SuppressLint("NewApi")
    protected void setGeolocationDatabasePath() {
        final Activity activity;

        if (mActivity != null && mActivity.get() != null) {
            activity = mActivity.get();
        } else {
            return;
        }

        getSettings().setGeolocationDatabasePath(activity.getDir("database", Context.MODE_PRIVATE).getPath());
    }

    /**
     * 清空 WebView 的缓存数据信息， 代表 WebView 被销毁.
     */
    public void onDestroy() {
        if(mActivity != null){
            mActivity.clear();
        }
        // try to remove this view from its parent first
        try {
            ((ViewGroup) getParent()).removeView(this);
        } catch (Exception ignored) {
        }

        // then try to remove all child views from this view
        try {
            removeAllViews();
        } catch (Exception ignored) {
        }
        // and finally destroy this view
        destroy();

        delMyBitmap();
    }

    /**
     * 拍照上传图片操作执行类
     */
    private class WebViewImgSelectDialogClick implements WebViewImgSelectDialog.OnBottomClick {
        ValueCallback<Uri[]> mFilePathCallback = null;
        WebChromeClient.FileChooserParams mFileChooserParams = null;

        public WebViewImgSelectDialogClick(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            // TODO Auto-generated constructor stub
            this.mFilePathCallback = filePathCallback;
            this.mFileChooserParams = fileChooserParams;
            if (mFileName == null) {
                mFileName = new LinkedList<String>();
            }
        }

        @Override
        public void onShootClick() {
            openCamera(mFilePathCallback, mFileChooserParams);
        }

        @Override
        public void onChooseClick() {
            openLocalFile(mFilePathCallback, mFileChooserParams);
        }

        @Override
        public void onPhotoAlbumClick() {
            openPhotoAlbumImage(mFilePathCallback, mFileChooserParams);
        }
    }

    /**
     * 手机相册
     *
     * @param filePathCallback
     * @param fileChooserParams
     */
    private void openPhotoAlbumImage(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(null);
        }
        mUploadMessage = filePathCallback;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");

        mActivity.get().startActivityForResult(Intent.createChooser(i, getResources().getString( R.string.duoqu_webview_select_application)), TAKE_PHOTO_ALBUM);
    }

    /**
     * 相机照相
     */
    private void openCamera(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(null);
        }
        mUploadMessage = filePathCallback;
        String fileName = "camera_" + System.currentTimeMillis() + ".jpg";
        mFileName.add(fileName);
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        mImageUri = mActivity.get().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        mActivity.get().startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
    }

    /**
     * 文件管理器
     */
    @SuppressLint("NewApi")
    private void openLocalFile(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(null);
        }
        mUploadMessage = filePathCallback;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        mActivity.get().startActivityForResult(Intent.createChooser(i, getResources().getString( R.string.duoqu_webview_select_application)), FILECHOOSER_RESULTCODE);
    }

    /**
     * 实现 WebView 的选择上传文件的必须调用方法.
     * 需要在 Activity 的onActivityResult回调， 直接调用该 WebView 的此方法.
     * @param requestCode 请求状态码
     * @param resultCode 回调状态码
     * @param data 回调数据
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            Uri result = null;
            if (resultCode != Activity.RESULT_OK) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
                return;
            }

            if (requestCode == FILECHOOSER_RESULTCODE || requestCode == TAKE_PHOTO_ALBUM) {
                if (null == mUploadMessage) {
                    return;
                }
                result = data == null ? null : data.getData();
            } else if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
                if (mUploadMessage == null) {
                    return;
                }
                if (mImageUri == null) {
                    return;
                }

                result = mImageUri;
                // Images captured by the URI, and through compression
                // processing
                Bitmap bitmap = null;
                try {
                    bitmap = getBitmapFormUri(mActivity.get(), result);
                } catch (Exception e) {
                }
                // resave Image .
                if (bitmap != null) {
                    saveMyBitmap(bitmap, mFileName.getLast());
                }
            }

            if (result == null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
                return;
            }
            /* RM-943 zhaoxiachao 20161117 start */
            if (result.toString().contains("content://") && requestCode != TAKE_PHOTO_REQUEST_CODE) {
                mUploadMessage.onReceiveValue(new Uri[]{result});
                mUploadMessage = null;
                return;
            }
            /* RM-943 zhaoxiachao 20161117 end */
            String path = FileUtils.getPath(mActivity.get(), result);
            if (TextUtils.isEmpty(path)) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
                return;
            }

            // Uri[] uri = new Uri[]{Uri.fromFile(new File(path))};
            Uri[] uri = new Uri[]{Uri.parse("file://" + path)};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mUploadMessage.onReceiveValue(new Uri[]{uri[0]});
            } else {
                mUploadMessage.onReceiveValue(uri);
            }
            mUploadMessage = null;
        } catch (Throwable ex) {
        }
    }

    /**
     * 通过 Uri 获取出一个 Bitmap
     *
     * @param uri 图片路径存储的 Uri
     */
    public static Bitmap getBitmapFormUri(Context ctx, Uri uri) throws IOException {
        InputStream input = ctx.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1)){
            return null;
        }
        float hh = 1024f;
        float ww = 768f;
        int be = 1;//be=1表示不缩放
        if (originalWidth > originalHeight && originalWidth > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (originalHeight / hh);
        }
        if (be <= 0){
            be = 1;
        }
        //比例压缩
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = be;//设置缩放比例
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        input = ctx.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return bitmap;
    }

    /**
     * 将 Bitmap 文件信息保存在 SD 卡根目录中
     * 先把图片保存起来，要进行图片上传， 等待 WebView 进行销毁时将图片删除
     * @param bitmap 图片
     * @param fileName 文件名
     */
    public void saveMyBitmap(Bitmap bitmap, String fileName) {
        File f = new File(Environment.getExternalStorageDirectory(), fileName);
        try {
            f.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
        }
        if (fOut != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            try {
                fOut.flush();
            } catch (IOException e) {
            }
            try {
                fOut.close();
            } catch (IOException e) {
            }
        }

    }

    /**
     * 删除保存在 SD卡根目录的图片
     */
    private void delMyBitmap() {
        // TODO Auto-generated method stub
        new Thread(sDelBitmap).start();
    }

    /**
     * 删除图片实现 Runnable.
     * 静态外部类，不持有 WebView 引用， 避免发生内存泄漏情况.
     */
    private static Runnable sDelBitmap = new Runnable() {
        @Override
        public void run() {
            try {
                if (mFileName == null) {
                    return;
                }
                File f = null;
                for (String fileName : mFileName) {
                    f = new File(Environment.getExternalStorageDirectory(), fileName);
                    if (f.isFile() && f.exists()) {
                        f.delete();
                    }
                }
            } catch (Throwable e) {
            }
        }
    };

	/**
     * 添加在访问 url 加载的时候， 携带的请求头数据信息
     *
     * @param name  添加进 HTTP 请求头的 key 名
     * @param value 添加进 HTTP 请求头的 key 值
     */
    public void addHttpHeader(final String name, final String value) {
        mHttpHeaders.put(name, value);
    }

    /**
     * 移除在访问 url 加载的时候， 携带的请求头数据信息
     *
     * @param name HTTP 请求头的 key 名
     */
    public void removeHttpHeader(final String name) {
        mHttpHeaders.remove(name);
    }

    /**
     * 防止WebView跨源攻击
     * @param webSettings
     * @param allowed
     */
    @SuppressLint("NewApi")
    protected static void setAllowAccessFromFileUrls(final WebSettings webSettings, final boolean allowed) {
        if (Build.VERSION.SDK_INT >= 16) {
            webSettings.setAllowFileAccessFromFileURLs(allowed);
            webSettings.setAllowUniversalAccessFromFileURLs(allowed);
        }
    }

    /**
     * 是否允许使用 Cookie
     * @param enabled
     */
    @SuppressWarnings("static-method")
    public void setCookiesEnabled(final boolean enabled) {
        CookieManager.getInstance().setAcceptCookie(enabled);
    }

    /**
     * 是否开启 Cookie 的支持
     * @param enabled
     */
    @SuppressLint("NewApi")
    public void setThirdPartyCookiesEnabled(final boolean enabled) {
        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, enabled);
        }
    }

    /**
     * 是否允许使用 Cookie
     */
    @SuppressWarnings("static-method")
    public void removeAllCookies() {
        CookieManager.getInstance().removeAllCookies(null);
    }

    /**
     * 设置是否允许WebView采用混合加载
     *
     * @param allowed true, 允许https中加载http链接  false, 不允许加载http
     */
    public void setMixedContentAllowed(final boolean allowed) {
        setMixedContentAllowed(getSettings(), allowed);
    }

    @SuppressWarnings("static-method")
    @SuppressLint("NewApi")
    protected void setMixedContentAllowed(final WebSettings webSettings, final boolean allowed) {
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(allowed ? WebSettings.MIXED_CONTENT_ALWAYS_ALLOW : WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
    }

    /**
     * 设置网页内容兼容手机端展示
     *
     * @param enabled
     */
    public void setDesktopMode(final boolean enabled) {
        final WebSettings webSettings = getSettings();

        final String newUserAgent;
        if (enabled) {
            newUserAgent = webSettings.getUserAgentString().replace("Mobile", "eliboM").replace("Android", "diordnA");
        } else {
            newUserAgent = webSettings.getUserAgentString().replace("eliboM", "Mobile").replace("diordnA", "Android");
        }

        webSettings.setUserAgentString(newUserAgent);
        webSettings.setUseWideViewPort(enabled);
        webSettings.setLoadWithOverviewMode(enabled);
        webSettings.setSupportZoom(enabled);
        webSettings.setBuiltInZoomControls(enabled);
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    protected void init(Context context) {
        // in IDE's preview mode
        if (isInEditMode()) {
            // do not run the code from this method
            return;
        }

        if (context instanceof Activity) {
            mActivity = new WeakReference<>((Activity) context);
        }

        setFocusable(true);
        setFocusableInTouchMode(true);

        setSaveEnabled(true);

        final String filesDir = context.getFilesDir().getPath();

        final WebSettings webSettings = getSettings();
        // 防止WebView跨源攻击
        // 设置是否允许WebView使用File协议，默认值是允许
        // 注意：不允许使用File协议，则不会存在通过file协议的跨源安全威胁，但同时也限制了WebView的功能，使其不能加载本地的HTML文件
        webSettings.setAllowFileAccess(false);
        setAllowAccessFromFileUrls(webSettings, false);
        // 允许缩放
        webSettings.setBuiltInZoomControls(false);
        // 设置是否允许WebView使用JavaScript
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 开启DomStorage缓存
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // xxxx
        webSettings.setSaveFormData(true);

        // 设置默认允许混合加载模式
        setMixedContentAllowed(webSettings, true);
        // 开启第三方Cookie的支持
        setThirdPartyCookiesEnabled(true);
        //设置 缓存模式
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        //提高渲染优先级
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        //图片放在最后加载
        webSettings.setBlockNetworkImage(true);

        super.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // 回调设置的监听器, 通过setListener方法。
                if (mListener != null) {
                    mListener.onPageStarted(url, favicon);
                }

                // 回调设置的WebViewClient， 通过setWebViewClient方法
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onPageStarted(view, url, favicon);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // 回调设置的监听器, 通过setListener方法。
                if (mListener != null) {
                    mListener.onPageFinished(url);
                }

                // 回调设置的WebViewClient， 通过setWebViewClient方法
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onPageFinished(view, url);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // 回调设置的监听器, 通过setListener方法。
                if (mListener != null) {
                    mListener.onPageError(errorCode, description, failingUrl);
                }

                // 回调设置的WebViewClient， 通过setWebViewClient方法
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onReceivedError(view, errorCode, description, failingUrl);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                Log.e("zxc", "webview3, url: "+url);
                if (TextUtils.isEmpty(url)
                        || url.toLowerCase(Locale.getDefault()).startsWith("http")) {
                    return false;
                }


                // if there is a user-specified handler available
                if (mCustomWebViewClient != null) {
                    // if the user-specified handler asks to override the request
                    if (mCustomWebViewClient.shouldOverrideUrlLoading(view, url)) {
                        // cancel the original request
                        return true;
                    }
                }

                final Uri uri = Uri.parse(url);
                final String scheme = uri.getScheme();

                if (scheme != null) {
                    final Intent externalSchemeIntent;

                    if (scheme.equals("tel")) {
                        // 拨打电话
                        externalSchemeIntent = new Intent(Intent.ACTION_DIAL, uri);
                    } else if (scheme.equals("sms")) {
                        // 发送短信
                        externalSchemeIntent = new Intent(Intent.ACTION_SENDTO, uri);
                    } else if (scheme.equals("mailto")) {
                        // 发送邮件
                        externalSchemeIntent = new Intent(Intent.ACTION_SENDTO, uri);
                    } else {
                        // 可支持唤起其他 deeplink 协议的应用
                        externalSchemeIntent = new Intent(Intent.ACTION_VIEW);
                        externalSchemeIntent.setData(uri);
                    }

                    // 每次打开一个任务栈， 与当前应用 activity 栈脱离
                    externalSchemeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        if (mActivity != null && mActivity.get() != null) {
                            mActivity.get().startActivity(externalSchemeIntent);
                        } else {
                            getContext().startActivity(externalSchemeIntent);
                        }
                    } catch (ActivityNotFoundException ignored) {
                        Log.e(TAG, ignored.getMessage());
                        super.shouldOverrideUrlLoading(view, url);
                    }

                    return true;
                }

                return true;

            }

            @Override
            public void onLoadResource(WebView view, String url) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onLoadResource(view, url);
                } else {
                    super.onLoadResource(view, url);
                }
            }

            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (mCustomWebViewClient != null) {
                    return mCustomWebViewClient.shouldInterceptRequest(view, url);
                } else {
                    return super.shouldInterceptRequest(view, url);
                }
            }

            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebViewClient != null) {
                        return mCustomWebViewClient.shouldInterceptRequest(view, request);
                    } else {
                        return super.shouldInterceptRequest(view, request);
                    }
                } else {
                    return null;
                }
            }

            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onFormResubmission(view, dontResend, resend);
                } else {
                    super.onFormResubmission(view, dontResend, resend);
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.doUpdateVisitedHistory(view, url, isReload);
                } else {
                    super.doUpdateVisitedHistory(view, url, isReload);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onReceivedSslError(view, handler, error);
                } else {
                    super.onReceivedSslError(view, handler, error);
                }
            }

            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient.onReceivedClientCertRequest(view, request);
                    } else {
                        super.onReceivedClientCertRequest(view, request);
                    }
                }
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
                } else {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm);
                }
            }

            @Override
            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                if (mCustomWebViewClient != null) {
                    return mCustomWebViewClient.shouldOverrideKeyEvent(view, event);
                } else {
                    return super.shouldOverrideKeyEvent(view, event);
                }
            }

            @Override
            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onUnhandledKeyEvent(view, event);
                } else {
                    super.onUnhandledKeyEvent(view, event);
                }
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient.onScaleChanged(view, oldScale, newScale);
                } else {
                    super.onScaleChanged(view, oldScale, newScale);
                }
            }

            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
                if (Build.VERSION.SDK_INT >= 12) {
                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient.onReceivedLoginRequest(view, realm, account, args);
                    } else {
                        super.onReceivedLoginRequest(view, realm, account, args);
                    }
                }
            }

        });

        super.setWebChromeClient(new WebChromeClient() {

            @SuppressWarnings("all")
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (Build.VERSION.SDK_INT >= 21) {
                    WebViewImgSelectDialog dialog = new WebViewImgSelectDialog(mActivity.get(), filePathCallback);
                    dialog.ShowDialog(new WebViewImgSelectDialogClick(filePathCallback, fileChooserParams));

                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // 回调设置的监听器, 通过setListener方法。
                if (mListener != null) {
                    mListener.onProgressChanged(view, newProgress);
                }

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onProgressChanged(view, newProgress);
                } else {
                    super.onProgressChanged(view, newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                // 回调设置的监听器, 通过setListener方法。
                if (mListener != null) {
                    mListener.onReceivedTitle(view, title);
                }

                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onReceivedTitle(view, title);
                } else {
                    super.onReceivedTitle(view, title);
                }
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onReceivedIcon(view, icon);
                } else {
                    super.onReceivedIcon(view, icon);
                }
            }

            @Override
            public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
                } else {
                    super.onReceivedTouchIconUrl(view, url, precomposed);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onShowCustomView(view, callback);
                } else {
                    super.onShowCustomView(view, callback);
                }
            }

            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
                if (Build.VERSION.SDK_INT >= 14) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
                    } else {
                        super.onShowCustomView(view, requestedOrientation, callback);
                    }
                }
            }

            @Override
            public void onHideCustomView() {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onHideCustomView();
                } else {
                    super.onHideCustomView();
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
                } else {
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
                }
            }

            @Override
            public void onRequestFocus(WebView view) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onRequestFocus(view);
                } else {
                    super.onRequestFocus(view);
                }
            }

            @Override
            public void onCloseWindow(WebView window) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onCloseWindow(window);
                } else {
                    super.onCloseWindow(window);
                }
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsAlert(view, url, message, result);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity.get());
                    builder.setTitle("Alert");
                    builder.setMessage(message);
                    builder.setPositiveButton(android.R.string.ok,
                            new AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    result.confirm();
                                }
                            });
                    builder.setCancelable(false);
                    builder.create();
                    builder.show();
                    return true;
                }
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsConfirm(view, url, message, result);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity.get());
                    builder.setTitle("confirm");
                    builder.setMessage(message);
                    builder.setPositiveButton(android.R.string.ok,
                            new AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    result.confirm();
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    result.cancel();
                                }
                            });
                    builder.setCancelable(false);
                    builder.create();
                    builder.show();
                    return true;
                }
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsPrompt(view, url, message, defaultValue, result);
                } else {
                    return super.onJsPrompt(view, url, message, defaultValue, result);
                }
            }

            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsBeforeUnload(view, url, message, result);
                } else {
                    return super.onJsBeforeUnload(view, url, message, result);
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
                if (mGeolocationEnabled || SPUtil.getBoolean(mActivity.get(), SPUtil.WEB_LOCATION, origin)) {
                    callback.invoke(origin, true, false);
                } else {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
                    } else {
                        super.onGeolocationPermissionsShowPrompt(origin, callback);
                        AlertDialog locationDialog = new AlertDialog.Builder(mActivity.get()).setMessage(R.string.duoqu_webview_location_prompt_message)
                                .setPositiveButton(R.string.duoqu_webview_location_prompt_positive, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mGeolocationEnabled = true;
                                        SPUtil.setBoolean(mActivity.get(), SPUtil.WEB_LOCATION, origin, mGeolocationEnabled);
                                        setGeolocationEnabled(mGeolocationEnabled);
                                        callback.invoke(origin, mGeolocationEnabled, false);
                                    }
                                }).setNegativeButton(R.string.duoqu_webview_location_prompt_negative, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mGeolocationEnabled = false;
                                        SPUtil.setBoolean(mActivity.get(), SPUtil.WEB_LOCATION, origin, mGeolocationEnabled);
                                        setGeolocationEnabled(mGeolocationEnabled);
                                        callback.invoke(origin, mGeolocationEnabled, false);
                                    }
                                }).create();

                        locationDialog.show();
                    }
                }
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onGeolocationPermissionsHidePrompt();
                } else {
                    super.onGeolocationPermissionsHidePrompt();
                }
            }

            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onPermissionRequest(PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient.onPermissionRequest(request);
                    } else {
                        super.onPermissionRequest(request);
                    }
                }
            }

            @SuppressLint("NewApi")
            @SuppressWarnings("all")
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient.onPermissionRequestCanceled(request);
                    } else {
                        super.onPermissionRequestCanceled(request);
                    }
                }
            }

            @Override
            public boolean onJsTimeout() {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onJsTimeout();
                } else {
                    return super.onJsTimeout();
                }
            }

            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.onConsoleMessage(message, lineNumber, sourceID);
                } else {
                    super.onConsoleMessage(message, lineNumber, sourceID);
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.onConsoleMessage(consoleMessage);
                } else {
                    return super.onConsoleMessage(consoleMessage);
                }
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.getDefaultVideoPoster();
                } else {
                    return super.getDefaultVideoPoster();
                }
            }

            @Override
            public View getVideoLoadingProgressView() {
                if (mCustomWebChromeClient != null) {
                    return mCustomWebChromeClient.getVideoLoadingProgressView();
                } else {
                    return super.getVideoLoadingProgressView();
                }
            }

            @Override
            public void getVisitedHistory(ValueCallback<String[]> callback) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient.getVisitedHistory(callback);
                } else {
                    super.getVisitedHistory(callback);
                }
            }
        });

        // 设置 WebView 下载监听
        setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(final String url, final String userAgent, final String contentDisposition, final String mimeType, final long contentLength) {
                final String suggestedFilename = URLUtil.guessFileName(url, contentDisposition, mimeType);

                if (mListener != null) {
                    mListener.onDownloadRequested(url, suggestedFilename, mimeType, contentLength, contentDisposition, userAgent);
                }
            }

        });
    }

    @Override
    public void loadUrl(final String url, Map<String, String> additionalHttpHeaders) {
        if (additionalHttpHeaders == null) {
            additionalHttpHeaders = mHttpHeaders;
        } else if (mHttpHeaders.size() > 0) {
            additionalHttpHeaders.putAll(mHttpHeaders);
        }

        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void loadUrl(final String url) {
        if (mHttpHeaders.size() > 0) {
            super.loadUrl(url, mHttpHeaders);
        } else {
            super.loadUrl(url);
        }
    }

    public void loadUrl(String url, final Map<String, Object> urlExtend, final Map<String, String> additionalHttpHeaders) {
        url = makeUrlUnique(url, urlExtend);
        loadUrl(url, additionalHttpHeaders);
    }

    /**
     * 按钮点击次数打点， 将按钮的扩展数据拼接到url链接中
     *
     * @param url 原访问 url
     * @param urlExtend 需要拼接到 url 字段
     * @return 拼接参数后访问的url
     */
    protected static String makeUrlUnique(final String url, final Map<String, Object> urlExtend) {
        if(urlExtend == null){
            return url;
        }

        String loadUrl = url;
        StringBuilder unique = new StringBuilder();
        unique.append(loadUrl);

        Iterator<Map.Entry<String, Object>> iterator = urlExtend.entrySet().iterator();

        while (iterator.hasNext()){

            Map.Entry<String, Object> entry = iterator.next();
            if (loadUrl.contains("?")) {
                unique.append('&');
            } else {
                unique.append('?');
            }

            unique.append(entry.getKey());
            unique.append('=');
            unique.append(entry.getValue());
            loadUrl = unique.toString();
        }
        return unique.toString();
    }

    /**
     * 通过从“fromUrl”加载文件并将其保存到外部存储上的“toFilename”来处理下载, 采用系统 DownloadManager 进行管理
     * <p>
     * 需要两个权限 `android.permission.INTERNET` 和 `android.permission.WRITE_EXTERNAL_STORAGE`
     * <p>
     * @param context    当前上下文
     * @param fromUrl    下载文件的网址链接
     * @param toFilename 下载完毕后， 保存的文件名字
     *
     * @return 是否下载成功
     */
    @SuppressLint("NewApi")
    public static boolean handleDownload(final Context context, final String fromUrl, final String toFilename) {
        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fromUrl));
        if (Build.VERSION.SDK_INT >= 11) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, toFilename);

        final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            try {
                dm.enqueue(request);
            } catch (SecurityException e) {
                if (Build.VERSION.SDK_INT >= 11) {
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                }
                dm.enqueue(request);
            }

            return true;
        }
        // if the download manager app has been disabled on the device
        catch (IllegalArgumentException e) {
            return false;
        }
    }
}
