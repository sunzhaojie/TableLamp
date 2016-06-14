package sunzj.sunjz_tablamp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by SunZJ on 16/5/28.
 */
public class BluetoothTool {

    private static final String TAG = "SunZJ";

    /**
     * 蓝牙设备地址
     */
    private String mBluetoothAddress = null;

    /**
     * 枚举 标示是客户端还是服务端
     */
    public static enum ServiceOrClient {
        NONE, SERVICE, CLIENT
    }
    private ServiceOrClient mServiceOrClient = ServiceOrClient.NONE;


    /**
     * 是否已经连接
     */
    private boolean mIsConnected = false;

    /**
     * 一些常量，代表服务器的名称
     */
    public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
    public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
    public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";

    /**
     * 蓝牙服务端socket
     */
    private BluetoothServerSocket mServiceSocket = null;

    /**
     * 蓝牙客户端socket
     */
    private BluetoothSocket mClientSocket = null;

    /**
     * 服务端线程
     */
    private ServiceThread mServiceThread = null;

    /**
     * 客户端线程
     */
    private ClientThread mClientThread = null;

    /**
     * 读取数据线程
     */
    private ReadThread mReadThread = null;


    /**
     * 蓝牙设备
     */
    private BluetoothDevice mDevice = null;

    /**
     * 更新UI接口
     */
    private IUpdateUI iUpdateUI = null;

    private static final int MSG_UPDATE_LISTVIEW = 0, MSG_UPDATE_LOG = 1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_LISTVIEW:
                    if (iUpdateUI != null)
                        iUpdateUI.updateListViewDevices();
                    break;
                case MSG_UPDATE_LOG:
                    if(iUpdateUI!=null)
                    iUpdateUI.updateLog(msg.obj + "");
                    break;
            }
        }
    };

    public BluetoothTool(String mBluetoothAddress, ServiceOrClient mServiceOrClient) {
        this.mBluetoothAddress = mBluetoothAddress;
        this.mServiceOrClient = mServiceOrClient;

        connect();
    }


    /**
     * 连接蓝牙
     */
    private void connect() {
        if (mIsConnected == true) {
            Log.d(TAG, "connect: 已经连接");
            Message msg = Message.obtain(null, MSG_UPDATE_LOG);
            msg.obj = "已经连接";
            mHandler.sendMessage(msg);
            return;
        }

        /** 客户端*/
        if (mServiceOrClient == ServiceOrClient.CLIENT) {
            if (!mBluetoothAddress.equals("null")) {

                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mBluetoothAddress);

                mClientThread = new ClientThread();
                mClientThread.start();
                mIsConnected = true;
            } else Log.d(TAG, "connect: address is null");
        }
     /*   else if (mServiceOrClient == ServiceOrClient.SERVICE) {
            mServiceThread = new ServiceThread();
            mServiceThread.start();
            mIsConnected = true;
        }*/
    }

    /**
     * 注册接口
     *
     * @param iUpdateUI
     */
    public void SetOnIUpdateUI(IUpdateUI iUpdateUI) {
        this.iUpdateUI = iUpdateUI;
    }


    /**
     * 客户端线程
     */
    private class ClientThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                /** 客户端通过服务端的UUID与之连接*/
                mClientSocket = mDevice.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));


                Log.d(TAG, "run: 正在连接。。。");

                Message msg = Message.obtain(null, MSG_UPDATE_LOG);
                msg.obj = "正在连接。。。";
                mHandler.sendMessage(msg);

                /** 连接*/
                mClientSocket.connect();

                Log.d(TAG, "run: 连接成功");

                msg = Message.obtain(null, MSG_UPDATE_LOG);
                msg.obj = "连接成功";
                mHandler.sendMessage(msg);


                msg = Message.obtain(null, MSG_UPDATE_LISTVIEW);
                mHandler.sendMessage(msg);

                /** 接收数据*/
                mReadThread = new ReadThread();
                mReadThread.start();

            } catch (IOException e) {
                e.printStackTrace();

                Log.d(TAG, "run: 连接失败");
                Message msg = Message.obtain(null, MSG_UPDATE_LOG);
                msg.obj = "连接失败";
                mHandler.sendMessage(msg);
            }
        }
    }


    /**
     * 服务端线程
     */
    public class ServiceThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {

                /** 创建一个蓝牙服务器 传入UUID*/
                mServiceSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(
                        PROTOCOL_SCHEME_RFCOMM,
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                Log.d(TAG, "run: 等待客户端连接...");

                /** 接收客服端连接请求*/
                mClientSocket = mServiceSocket.accept();

                Log.d(TAG, "run: accept successs");


                /** 接收数据*/
                mReadThread = new ReadThread();
                mReadThread.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 读取数据线程
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();

            byte[] buffer = new byte[1024];
            int bytes;
            InputStream in = null;

            try {
                in = mClientSocket.getInputStream();

                while (true) {
                    if ((bytes = in.read(buffer)) > 0) {
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];


                            int j = buffer[i];
                            j = buffer[i] & 0xff;
                            String str = Integer.toHexString(j);
                            Log.d(TAG, "run: 收到：" + str);
                            if ("ff".equals(str)) {
                                Message msg = Message.obtain(null, MSG_UPDATE_LOG);
                                msg.obj = "台灯打开";
                                mHandler.sendMessage(msg);
                            } else if ("0".equals(str)) {
                                Message msg = Message.obtain(null, MSG_UPDATE_LOG);
                                msg.obj = "台灯关闭";
                                mHandler.sendMessage(msg);
                            } else {
                               Message msg = Message.obtain(null, MSG_UPDATE_LOG);
                                msg.obj = "err...";
                                mHandler.sendMessage(msg);
                            }

                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "run: 接收数据失败。。。");

                Message msg = Message.obtain(null, MSG_UPDATE_LOG);
                msg.obj = "连接数据失败";
                mHandler.sendMessage(msg);
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }

        }
    }


    /**
     * 发送数据
     *
     * @param str
     */
    public void sendData(String str) {
        if (mClientSocket == null) {
            Log.d(TAG, "sendData: 没有连接。。。");

            Message msg = Message.obtain(null, MSG_UPDATE_LOG);
            msg.obj = "没有连接";
            mHandler.sendMessage(msg);
            return;
        }

        OutputStream out = null;
        try {
            out = mClientSocket.getOutputStream();
            out.write(getHexBytes(str));

            Log.d(TAG, "sendData: 发送：" + str);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "sendData: 发送 " + str + " 失败");

            Message msg = Message.obtain(null, MSG_UPDATE_LOG);
            msg.obj = "发送失败";
            mHandler.sendMessage(msg);

        } finally {
         /*   if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
        }
    }


    /**
     * 转化为16进制
     *
     * @param str
     * @return
     */
    private byte[] getHexBytes(String str) {
        int len = str.length() / 2;
        char[] chars = str.toCharArray();
        String[] hexStr = new String[len];
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; j < len; i += 2, j++) {
            hexStr[j] = "" + chars[i] + chars[i + 1];
            bytes[j] = (byte) Integer.parseInt(hexStr[j], 16);
        }
        return bytes;
    }


}
