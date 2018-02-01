package com.tencent.map.geolocation.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
	private TextView tvLocationInfos;
	private UIHandler mUIHandler = null;

	private static SimpleDateFormat mSdf = new SimpleDateFormat("HH:mm:ss");

	private TencentLocationManager mLocMgr;
	private TencentLocationRequest mLocRequest;
	private TencentLocationListener mLocListener;
	private TencentLocationListener mLocSingleListener;

	private HandlerThread mLocHandlerThread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tvLocationInfos = (TextView) findViewById(R.id.tv_location_infos);
		if (Build.VERSION.SDK_INT >= 23) {
			String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE};

			if (checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(permissions, 0);
			}
		}

		mLocHandlerThread = new HandlerThread("demo");
		mLocHandlerThread.start();

		mUIHandler = new UIHandler(getMainLooper());
		mLocMgr = TencentLocationManager.getInstance(getApplicationContext());
		// 定位请求相关参数设置
		mLocRequest = TencentLocationRequest.create().setInterval(5000);
		// 连续定位回调Listener
		mLocListener = new TencentLocationListener() {
			@Override
			public void onLocationChanged(TencentLocation location, int error, String reason) {
				// showMsgInTextview("curThread:" + Thread.currentThread().getName(), UIHandler.MSG_SHOW_STRING_LOCATION);
				String str = "周期回调---" + mSdf.format(new Date()) + "---" + Thread.currentThread().getName() + "\n";
				if (location != null) {
					if (error == TencentLocation.ERROR_OK) {
						str += String.format("%.6f,%.6f,%.2f,%s,%s,%d", location.getLatitude(), location.getLongitude(),
								location.getAccuracy(), location.getProvider(), location.getIndoorBuildingFloor(), location.getTime());
					} else {
						str += "(*," + error + "," + reason + ")";
					}
				} else {
					str += "(null," + error + "," + reason + ")";
				}
				showMsgInTextview(str, UIHandler.MSG_SHOW_STRING_LOCATION);
			}

			@Override
			public void onStatusUpdate(String name, int status, String desc) {
				String str = name + "," + status + "," + desc + "," + Thread.currentThread().getName();

				showMsgInTextview(str, UIHandler.MSG_SHOW_STRING_LOCATION);
			}
		};
		// 单点定位回调Listener
		mLocSingleListener = new TencentLocationListener() {
			@Override
			public void onLocationChanged(TencentLocation location, int error, String reason) {
				String str = "单次定位---" + mSdf.format(new Date()) + "---" + Thread.currentThread().getName() + "\n";
				if (location != null) {
					if (error == TencentLocation.ERROR_OK) {
						str += String.format("%.6f,%.6f,%.2f,%s,%s", location.getLatitude(), location.getLongitude(),
								location.getAccuracy(), location.getProvider(), location.getAddress());
					} else {
						str += "(*," + error + "," + reason + ")";
					}
				} else {
					str += "(null," + error + "," + reason + ")";
				}
				showMsgInTextview(str, UIHandler.MSG_SHOW_STRING_LOCATION);
			}

			@Override
			public void onStatusUpdate(String name, int status, String desc) {

			}
		};
	}

	public void btnClickStartLocation(View view) {
		int error = mLocMgr.requestLocationUpdates(mLocRequest, mLocListener, mLocHandlerThread.getLooper());
		String message = "start : " + (error == 0 ? "success." : "failed.") + "[" + error + "]";
		showMsgInTextview(message, UIHandler.MSG_SHOW_STRING_LOCATION);
	}

	public void btnClickStopLocation(View view) {
		mLocMgr.removeUpdates(mLocListener);
		showMsgInTextview("stop", UIHandler.MSG_SHOW_STRING_LOCATION);
	}

	public void btnClickSingleLocation(View view) {
		int error = mLocMgr.requestSingleFreshLocation(mLocSingleListener, mLocHandlerThread.getLooper());
		String message = "start single : " + (error == 0 ? "success." : "failed.") + "[" + error + "]";
		showMsgInTextview(message, UIHandler.MSG_SHOW_STRING_LOCATION);
	}

	@Override
	protected void onDestroy() {
		if (mLocHandlerThread != null && mLocHandlerThread.isAlive()) {
			mLocHandlerThread.quit();
			mLocHandlerThread = null;
		}

		super.onDestroy();
	}

	private long mClickBackTime = 0L;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			long curTime = System.currentTimeMillis();
			if (curTime - mClickBackTime > 2000) {
				showMsgInToast("再按一次退出程序");
				mClickBackTime = curTime;
			} else {
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showMsgInToast(String str) {
		Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
	}

	private void showMsgInTextview(String str, int msgId) {
		if (mUIHandler != null) {
			Message message = mUIHandler.obtainMessage(msgId);
			StringBuilder sb = new StringBuilder();
			sb.append(mSdf.format(new Date())).append(":");
			sb.append(str).append("\n");
			String msgObj = sb.toString();
			message.obj = msgObj;
			message.sendToTarget();
		}
	}

	private class UIHandler extends Handler {
		public static final int MSG_SHOW_STRING_LOCATION = 0x01;
		public static final int MSG_CLEAR_STATUS = 0x02;

		private StringBuilder sbLocation = new StringBuilder();

		public UIHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_SHOW_STRING_LOCATION:
					String strLoc = (String) msg.obj;
					sbLocation.append(strLoc);
					if (sbLocation.length() > 5 * 1024L) {
						sbLocation.delete(0, 1000);
					}
					tvLocationInfos.setText(sbLocation.toString());
					break;
				case MSG_CLEAR_STATUS:
					if (sbLocation.length() > 200) {
						sbLocation.setLength(0);
					}
					tvLocationInfos.setText(sbLocation.toString());
					break;
				default:
					break;
			}
		}
	}
}
