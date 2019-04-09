package cn.com.xy.sms.sdk.ui.webview.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SPUtil {

    public static final String WEB_LOCATION = "web_location";
    /**
     * 保存Boolean类型的数据
     * @param context 用来创建出SharedPreferences
     * @param name    当前file的名字
     * @param key     key名
     * @param value   key名对应的值
     */
    public static void setBoolean(Context context, String name, String key , boolean value){
        // 私有的文件
        SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(key, value);
        ed.apply();
    }


    /**
     * 获取Boolean类型的保存值
     * @param context 用来创建出SharedPreferences
     * @param name    当前file的名字
     * @param key     对应的KEY
     * @return        对应KEY的VALUE 默认值为false
     */
    public static Boolean getBoolean(Context context, String name, String key){
        SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        return sp.getBoolean(key, false);
    }
}
