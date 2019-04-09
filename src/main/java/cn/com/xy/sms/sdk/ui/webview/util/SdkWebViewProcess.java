package cn.com.xy.sms.sdk.ui.webview.util;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/* HUAWEI-4289 huangzhiqiang 20180504 begin */
public class SdkWebViewProcess {

    private static final String TAG = "SdkWebViewProcess";

    private Map<String, HashSet<String>> mCheckAppNameCache = new HashMap<String, HashSet<String>>();
    private String mCurrentUrl = null;
    private static final int APP_NAME_CHECK_MAX_SIZE = 10;

    public void setCurrentUrl(String url) {
        mCurrentUrl = url;
    }

    public boolean requestPermissionCheckAppName(String appName) {
        if (mCurrentUrl == null) {
            Log.e(TAG, "SdkWebViewProcess requestPermissionCheckAppName url is null");
            return false;
        }
        HashSet<String> checkAppSet = mCheckAppNameCache.get(mCurrentUrl);
        if (checkAppSet == null) {
            checkAppSet = new HashSet<>();
            mCheckAppNameCache.put(mCurrentUrl, checkAppSet);
        }
        if (checkAppSet.contains(appName)) {
            return true;
        }
        if (checkAppSet.size() < APP_NAME_CHECK_MAX_SIZE) {
            checkAppSet.add(appName);
            return true;
        }
        Log.e(TAG, "SdkWebViewProcess requestPermissionCheckAppName checkCount > maxSize");
        return false;
    }

    public void destroy() {
        mCheckAppNameCache.clear();
    }
}
/* HUAWEI-4289 huangzhiqiang 20180504 end */
