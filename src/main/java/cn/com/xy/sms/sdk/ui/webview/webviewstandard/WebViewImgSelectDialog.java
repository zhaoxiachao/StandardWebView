package cn.com.xy.sms.sdk.ui.webview.webviewstandard;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.widget.TextView;

import cn.com.xy.sms.sdk.ui.webview.R;

public class WebViewImgSelectDialog {
    private Context mContext;
    private Dialog dialog;
    private ValueCallback<Uri[]> mFilePathCallback;

    public WebViewImgSelectDialog(Context context, ValueCallback<Uri[]> filePathCallback) {
        super();
        this.mContext = context;
        this.mFilePathCallback = filePathCallback;
    }

    public void ShowDialog(final OnBottomClick click) {
        // TODO Auto-generated method stub
        dialog = new Dialog(mContext);
        dialog.setOnCancelListener(new OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface dialog) {
                // To cancel the request, call filePathCallback.onReceiveValue(null) and return true.
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
            }
        });
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);

        View customView = LayoutInflater.from(mContext).inflate(R.layout.duoqu_webview_img_select_dialog, null);
        // 手机相册
        TextView photoAlbum = (TextView) customView.findViewById(R.id.duoqu_webview_img_select_dialog_photo_album);
        // 相机照相
        TextView shooting = (TextView) customView.findViewById(R.id.duoqu_webview_img_select_dialog_shooting);
        // 文件管理器
        TextView choose = (TextView) customView.findViewById(R.id.duoqu_webview_img_select_dialog_choose);

        photoAlbum.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                click.onPhotoAlbumClick();
                dialog.dismiss();
            }
        });

        shooting.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                click.onShootClick();
                dialog.dismiss();
            }
        });
        choose.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                click.onChooseClick();
                dialog.dismiss();
            }
        });
        
        
        dialog.setContentView(customView);
        dialog.show();
    }

    /**
     * 选择弹窗点击事件监听回调
     *
     * 图片上传点击回调 @onPhotoAlbumClick
     * 拍照上传点击回调 @onShootClick
     * 文件管理器上传点击回调 @onChooseClick
     */
    public interface OnBottomClick {
        void onPhotoAlbumClick();
        void onShootClick();
        void onChooseClick();
    }
}

