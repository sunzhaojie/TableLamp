package sunzj.sunjz_tablamp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainAty extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    /**
     * 一些控件
     */
    private TextView mTextView;
    private Button mButtonOprn, mButtonClose;
    private ListView mListViewDevices;


    /**
     * 蓝牙辅助类
     */
    private BluetoothTool mBluetoothTool = null;

    /**
     * listview 适配器...
     */
    private SimpleAdapter simpleAdapter = null;
    private List<HashMap<String, Object>> data = null;
    private int index = -1;

    /**
     * 获得默认的蓝牙适配器
     */
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        /** 判断蓝牙是否可用，不可用时请求打开*/
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        /** 获取以前匹配过的蓝牙设备*/
        Set<BluetoothDevice> devices = null;
        if (mBluetoothAdapter != null)
            devices = mBluetoothAdapter.getBondedDevices();
        else
            Toast.makeText(MainAty.this, "该设备不支持蓝牙功能 ", Toast.LENGTH_SHORT).show();

        if (devices != null && devices.size() > 0) {
            data.clear();
            for (BluetoothDevice device : devices) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("lv_left_icon", R.drawable.lv_left_icon);
                map.put("lv_address", device.getAddress());
                map.put("lv_right_icon", R.drawable.lv_right_white);
                data.add(map);
            }
        } else {
            HashMap<String, Object> map = new HashMap<>();
            map.put("lv_left_icon", R.drawable.lv_left_icon);
            map.put("lv_address", "没有已经匹配的设备");
            map.put("lv_right_icon", R.drawable.lv_right_white);
            data.add(map);
            mTextView.append("没有已经匹配的设备" + "\r\n");
        }

        simpleAdapter.notifyDataSetChanged();

    }

    /**
     * 初始化
     */
    private void init() {

        mTextView = (TextView) findViewById(R.id.id_tv);
        mButtonOprn = (Button) findViewById(R.id.id_btn_open);
        mButtonClose = (Button) findViewById(R.id.id_btn_close);
        mListViewDevices = (ListView) findViewById(R.id.id_lv_devices);

        data = new ArrayList<>();
        simpleAdapter = new SimpleAdapter(this, data, R.layout.layout_lv_divices_item, new String[]{"lv_left_icon", "lv_address", "lv_right_icon"}, new int[]{R.id.id_iv_left, R.id.id_tv_address, R.id.id_iv_right});
        mListViewDevices.setAdapter(simpleAdapter);
        mListViewDevices.setOnItemClickListener(this);


        mButtonOprn.setOnClickListener(this);
        mButtonClose.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
            case R.id.id_btn_open:
                if (mBluetoothTool != null) {
                    mBluetoothTool.sendData("ff");
                } else
                    Toast.makeText(MainAty.this, "蓝牙未连接...", Toast.LENGTH_SHORT).show();
                break;
            case R.id.id_btn_close:
                if (mBluetoothTool != null) {
                    mBluetoothTool.sendData("00");
                } else
                    Toast.makeText(MainAty.this, "蓝牙未连接...", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        index = i;
        String address = (String) data.get(i).get("lv_address");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainAty.this);
        builder.setTitle("连接");
        builder.setMessage(address);
        builder.setPositiveButton("连接", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mBluetoothTool = new BluetoothTool((String) data.get(0).get("lv_address"),
                        BluetoothTool.ServiceOrClient.CLIENT);
                mBluetoothTool.SetOnIUpdateUI(new IUpdateUI() {
                    @Override
                    public void updateListViewDevices() {
                        for (int i = 0; i < data.size(); i++) {
                            if (i == index) {
                                data.get(i).put("lv_right_icon", R.drawable.checked);
                                continue;
                            }
                            data.get(i).put("lv_right_icon", R.drawable.lv_right_white);
                        }

                        simpleAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void updateLog(String msg) {
                        mTextView.append("\r\n" + msg);
                    }
                });

            }


        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }


}
