# 标准WebView

已合入功能.

- WebView 上传文件、拍照上传、从文件管理器选择文件
- WebView 支持 DeepLink 打开具体指定 App
- xxx



接入方式



```
mWebView.setDesktopMode(true);
// 默认不允许读取当前位置， 通过系统弹窗替代
mWebView.setGeolocationEnabled(false);
mWebView.setMixedContentAllowed(false);
mWebView.setCookiesEnabled(true);
mWebView.setThirdPartyCookiesEnabled(true);

mWebView.setListener(this, new XiaoyuanWebView.Listener() {
            @Override
            public void onPageStarted(String url, Bitmap favicon) {
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
            }

            @Override
            public void onPageFinished(String url) {
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
            }

            @Override
            public void onPageError(int errorCode, String description, String failingUrl) {
            }

            @Override
            public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
        });
        

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mWebView.onActivityResult(requestCode, resultCode, data);
    }
    
    protected void onDestroy() {
        super.onDestroy();
        mWebView.onDestroy();
    }
```