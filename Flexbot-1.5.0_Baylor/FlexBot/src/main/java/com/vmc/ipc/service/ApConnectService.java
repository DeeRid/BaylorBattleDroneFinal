package com.vmc.ipc.service;

import java.util.List;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.vmc.ipc.config.VmcConfig;
import com.vmc.ipc.util.DebugHandler;

public class ApConnectService extends Service{

    private final static String TAG = "ApConnectHandler";

    private Handler ApCheckHandler;
    private final static int MSG_AP_ENABLE_CHECK = 1000;
    private final static int MSG_AP_CONNECT_AUTO = 1001;
    private final static int MSG_AP_CONNECT_MANUALLY = 1002;
    
    public final static String ACTION_CHECK_AND_ENABLE_WIFI = "action_check_and_ENABLE_wifi";
    public final static String ACTION_AUTO_CONNECT_LAST_IPC = "action_auto_connect_last_ipc";
    public final static String ACTION_CONNECT_MANUALLY = "action_connect_manually";
    
    ApAdapter mApAdapter = null;
    
    public ApConnectService() {
	// TODO Auto-generated constructor stub
    }
    
    public class LocalBinder extends Binder {
	ApConnectService getService() {
            return ApConnectService.this;
        }
    }

    @Override
    public void onCreate() {
	mApAdapter = new ApAdapter(this);
	mApAdapter.start();
	try {
	    Thread.sleep(500);
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        if(intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if(action.equals(ACTION_CHECK_AND_ENABLE_WIFI)) {
            ApCheckHandler.removeMessages(MSG_AP_ENABLE_CHECK);
            ApCheckHandler.sendEmptyMessage(MSG_AP_ENABLE_CHECK);
        }
        else if(action.equals(ACTION_AUTO_CONNECT_LAST_IPC)) {
            ApCheckHandler.removeMessages(MSG_AP_CONNECT_AUTO);
            ApCheckHandler.sendEmptyMessage(MSG_AP_CONNECT_AUTO);
        }
        else if(action.equals(ACTION_CONNECT_MANUALLY)) {
            ApCheckHandler.removeMessages(MSG_AP_CONNECT_MANUALLY);
            ApCheckHandler.sendEmptyMessage(MSG_AP_CONNECT_MANUALLY);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
	ApCheckHandler.getLooper().quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    
    class ApAdapter extends Thread {  
	
	WifiAdmin mWifiAdmin;
	Context mContext;
	
	public ApAdapter(Context context) {
	    mContext = context;
	    mWifiAdmin = new WifiAdmin(mContext);
	}
	
        @Override  
        public void run() {  
            Looper.prepare();
            ApCheckHandler = new Handler(){
                public void handleMessage (Message msg) {
                    switch(msg.what) {
                        case MSG_AP_ENABLE_CHECK: {
                            DebugHandler.logd(TAG, "-----MSG_AP_ENABLE_CHECK");
                            if(mWifiAdmin.checkAndEnableWifi()) {
                        	ApCheckHandler.obtainMessage(MSG_AP_CONNECT_AUTO).sendToTarget();
                            }
                    	    break;
                        }
                        case MSG_AP_CONNECT_AUTO:{
                            DebugHandler.logd(TAG, "-----MSG_AP_CONNECT_AUTO");
                    		boolean isAutoConnect = VmcConfig.getInstance().isAutoConnect2AvailableAp();
                    		String lastAvailableIpc = VmcConfig.getInstance().getLastAvailableIpcAp();
                    		if(isAutoConnect && lastAvailableIpc != null) {
                    		    mWifiAdmin.addNetwork(mWifiAdmin.IsExsits(lastAvailableIpc));
                    		}
                    		break;
                        }
                        case MSG_AP_CONNECT_MANUALLY: {
                            DebugHandler.logd(TAG, "-----MSG_AP_CONNECT_MANUALLY");
            		    Intent intent = new Intent();
            		    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            		    intent.setAction(WifiManager.ACTION_PICK_WIFI_NETWORK);
            		    mContext.startActivity(intent);
            		    break;
                        }
                        default:{
                    		DebugHandler.logd(TAG, "the APAdapter don't know what you want to do!");
                        }
                    }  
                };  
            };
            Looper.loop();//4、启动消息循�? 
        }  
    } 
    
    public class WifiAdmin {  
	    // 定义WifiManager对象   
	    private WifiManager mWifiManager;  
	    // 定义WifiInfo对象   
	    private WifiInfo mWifiInfo;  
	    // 扫描出的网络连接列表   
	    private List<ScanResult> mWifiList;  
	    // 网络连接列表   
	    private List<WifiConfiguration> mWifiConfiguration;  
	    // 定义�?��WifiLock   
	    WifiLock mWifiLock;  
	 
	  
	    // 构�?�?  
	    public WifiAdmin(Context context) {  
	        // 取得WifiManager对象   
	        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);  
	        // 取得WifiInfo对象   
	        mWifiInfo = mWifiManager.getConnectionInfo();  
	    }  
	  
	    // 打开WIFI   
	    public void openWifi() {  
	        if (!mWifiManager.isWifiEnabled()) {  
	            mWifiManager.setWifiEnabled(true);  
	        }  
	    }  
	  
	    // 关闭WIFI   
	    public void closeWifi() {  
	        if (mWifiManager.isWifiEnabled()) {  
	            mWifiManager.setWifiEnabled(false);  
	        }  
	    }  
	  
	    // �?��当前WIFI状�?   
	    public int checkState() {  
	        return mWifiManager.getWifiState();  
	    }  
	    
	    private boolean checkAndEnableWifi() {
		boolean wifiEnabled = mWifiManager.isWifiEnabled();
		if (!wifiEnabled) {
//		    DebugHandler.logdWithToast(this, this.getResources().getString(R.string.enable_wifi_force), 2000);
		    int index = 0;
		    while(!(wifiEnabled = mWifiManager.setWifiEnabled(true)) && index++ < 5) {
//			DebugHandler.logdWithToast(this, this.getResources().getString(R.string.enable_wifi_fail), 3000);
		    }
		}
		return wifiEnabled;
	    }
	  
	    // 锁定WifiLock   
	    public void acquireWifiLock() {  
	        mWifiLock.acquire();  
	    }  
	  
	    // 解锁WifiLock   
	    public void releaseWifiLock() {  
	        // 判断时�?锁定   
	        if (mWifiLock.isHeld()) {  
	            mWifiLock.acquire();  
	        }  
	    }  
	  
	    // 创建�?��WifiLock   
	    public void creatWifiLock() {  
	        mWifiLock = mWifiManager.createWifiLock("Test");  
	    }  
	  
	    // 得到配置好的网络   
	    public List<WifiConfiguration> getConfiguration() {  
	        return mWifiConfiguration;  
	    }  
	  
	    // 指定配置好的网络进行连接   
	    public void connectConfiguration(int index) {  
	        // 索引大于配置好的网络索引返回   
	        if (index > mWifiConfiguration.size()) {  
	            return;  
	        }  
	        // 连接配置好的指定ID的网�?  
	        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId,  
	                true);  
	    }  
	  
	    public void startScan() {  
	        mWifiManager.startScan();  
	        // 得到扫描结果   
	        mWifiList = mWifiManager.getScanResults();  
	        // 得到配置好的网络连接   
	        mWifiConfiguration = mWifiManager.getConfiguredNetworks();  
	    }  
	  
	    // 得到网络列表   
	    public List<ScanResult> getWifiList() {  
	        return mWifiList;  
	    }  
	  
	    // 查看扫描结果   
	    public StringBuilder lookUpScan() {  
	        StringBuilder stringBuilder = new StringBuilder();  
	        for (int i = 0; i < mWifiList.size(); i++) {  
	            stringBuilder  
	                    .append("Index_" + new Integer(i + 1).toString() + ":");  
	            // 将ScanResult信息转换成一个字符串�?  
	            // 其中把包括：BSSID、SSID、capabilities、frequency、level   
	            stringBuilder.append((mWifiList.get(i)).toString());  
	            stringBuilder.append("/n");  
	        }  
	        return stringBuilder;  
	    } 
	  
	    // 得到MAC地址   
	    public String getMacAddress() {  
	        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();  
	    }  
	  
	    // 得到接入点的BSSID   
	    public String getBSSID() {  
	        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();  
	    }  
	  
	    // 得到IP地址   
	    public int getIPAddress() {  
	        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();  
	    }  
	  
	    // 得到连接的ID   
	    public int getNetworkId() {  
	        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();  
	    }  
	  
	    // 得到WifiInfo的所有信息包   
	    public String getWifiInfo() {  
	        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();  
	    }  
	  
	    // 添加�?��网络并连�?  
	    public void addNetwork(WifiConfiguration wcg) {  
	     int wcgID = mWifiManager.addNetwork(wcg);  
	     boolean b =  mWifiManager.enableNetwork(wcgID, true);  
	     System.out.println("a--" + wcgID); 
	     System.out.println("b--" + b); 
	    }  
	  
	    // 断开指定ID的网�?  
	    public void disconnectWifi(int netId) {  
	        mWifiManager.disableNetwork(netId);  
	        mWifiManager.disconnect();  
	    }
	  
	    public WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type)  
	    {  
	          WifiConfiguration config = new WifiConfiguration();    
	           config.allowedAuthAlgorithms.clear();  
	           config.allowedGroupCiphers.clear();  
	           config.allowedKeyManagement.clear();  
	           config.allowedPairwiseCiphers.clear();  
	           config.allowedProtocols.clear();  
	          config.SSID = "\"" + SSID + "\"";    
	           
	          WifiConfiguration tempConfig = this.IsExsits(SSID);            
	          if(tempConfig != null) {   
	              mWifiManager.removeNetwork(tempConfig.networkId);   
	          } 
	           
	          if(Type == 1) //WIFICIPHER_NOPASS 
	          {  
	               config.wepKeys[0] = "";  
	               config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);  
	               config.wepTxKeyIndex = 0;  
	          }  
	          if(Type == 2) //WIFICIPHER_WEP 
	          {  
	              config.hiddenSSID = true; 
	              config.wepKeys[0]= "\""+Password+"\"";  
	              config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);  
	              config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);  
	              config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);  
	              config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);  
	              config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);  
	              config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);  
	              config.wepTxKeyIndex = 0;  
	          }  
	          if(Type == 3) //WIFICIPHER_WPA 
	          {  
	          config.preSharedKey = "\""+Password+"\"";  
	          config.hiddenSSID = true;    
	          config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);    
	          config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);                          
	          config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);                          
	          config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);                     
	          //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);   
	          config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP); 
	          config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP); 
	          config.status = WifiConfiguration.Status.ENABLED;    
	          } 
	           return config;  
	    }  
	     
	    private WifiConfiguration IsExsits(String SSID)   
	    {   
	        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();   
	           for (WifiConfiguration existingConfig : existingConfigs)    
	           {   
	             if (existingConfig.SSID.equals("\""+SSID+"\""))   
	             {   
	                 return existingConfig;   
	             }   
	           }   
	        return null;    
	    } 
	   
	} 
}
