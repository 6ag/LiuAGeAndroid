package tv.baokan.liuageandroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import cn.jpush.android.api.JPushInterface;
import tv.baokan.liuageandroid.ui.activity.NewsDetailActivity;
import tv.baokan.liuageandroid.ui.activity.PhotoDetailActivity;
import tv.baokan.liuageandroid.utils.LogUtils;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
            LogUtils.d(TAG, "接收到通知");
            Bundle bundle = intent.getExtras();
            showTip(context, bundle);
        } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
            LogUtils.d(TAG, "用户点击打开了通知");
            Bundle bundle = intent.getExtras();
            jumpToDetail(context, bundle);
        }
    }

    /**
     * 收到通知后判断是否在前台，在则提示
     *
     * @param context
     * @param bundle
     */
    private void showTip(final Context context, Bundle bundle) {
//        if (!App.app.isApplicationBroughtToBackground(context)) {
//            try {
//                JSONObject json = new JSONObject(bundle.getString(JPushInterface.EXTRA_EXTRA));
//                String id = json.getString("id");
//                String classid = json.getString("classid");
//                String type = json.getString("type");
//                LogUtils.d(TAG, "id = " + id + " classid = " + classid + " type = " + type);
//
//                if (type.equals("photo")) {
//                    PhotoDetailActivity.start(context, classid, id);
//                } else {
//                    NewsDetailActivity.start(context, classid, id);
//                }
//            } catch (JSONException e) {
//                LogUtils.d(TAG, "解析推送数据失败");
//            }
//        }
    }

    /**
     * 跳转页面 - 点击通知后
     *
     * @param bundle 通知内容
     */
    private void jumpToDetail(Context context, Bundle bundle) {

        //清除指定通知
        JPushInterface.clearNotificationById(context, bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID));

        try {
            JSONObject json = new JSONObject(bundle.getString(JPushInterface.EXTRA_EXTRA));
            String id = json.getString("id");
            String classid = json.getString("classid");
            String type = json.getString("type");
            LogUtils.d(TAG, "id = " + id + " classid = " + classid + " type = " + type);
            if (type.equals("photo")) {
                PhotoDetailActivity.start(context, classid, id);
            } else {
                NewsDetailActivity.start(context, classid, id);
            }
        } catch (JSONException e) {
            LogUtils.d(TAG, "解析推送数据失败");
        }
    }
}
