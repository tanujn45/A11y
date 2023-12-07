package com.tanujn45.a11y;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.movesense.mds.Mds;
import com.movesense.mds.MdsConnectionListener;
import com.movesense.mds.MdsException;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    static Mds mMds;
    static String connectedSerial;
    static private RxBleClient mBleClient;
    private final ArrayList<MyScanResult> mScanResArrayList = new ArrayList<>();
    ArrayAdapter<MyScanResult> mScanResArrayAdapter;
    Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNeededPermissions();

        initMds();

        // UI
        ListView mScanResultListView = findViewById(R.id.bluetoothListView);
        mScanResArrayAdapter = new ArrayAdapter<MyScanResult>(this, android.R.layout.simple_list_item_1, mScanResArrayList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Apply padding to the TextView inside the default layout
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setPadding(20, 20, 20, 20); // Set padding in pixels (left, top, right, bottom)

                return view;
            }
        };
        mScanResultListView.setAdapter(mScanResArrayAdapter);
        mScanResultListView.setOnItemClickListener(this);

        connectButton = findViewById(R.id.connectButton);
        connectButton.setEnabled(false);
        startScan();
    }

    private RxBleClient getBleClient() {
        if (mBleClient == null) {
            mBleClient = RxBleClient.create(this);
        }

        return mBleClient;
    }

    private void initMds() {
        mMds = Mds.builder().build(this);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    void requestNeededPermissions() {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = new String[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PERMISSIONS = new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    Disposable mScanSubscription;

    public void startScan() {
        mScanResArrayList.clear();
        mScanResArrayAdapter.notifyDataSetChanged();

        mScanSubscription = getBleClient().scanBleDevices(new ScanSettings.Builder().build()).subscribe(scanResult -> {
            Log.d(LOG_TAG, "scanResult: " + scanResult);

            if (scanResult.getBleDevice() != null && scanResult.getBleDevice().getName() != null && scanResult.getBleDevice().getName().startsWith("Movesense")) {

                MyScanResult msr = new MyScanResult(scanResult);
                if (mScanResArrayList.contains(msr))
                    mScanResArrayList.set(mScanResArrayList.indexOf(msr), msr);
                else mScanResArrayList.add(0, msr);

                mScanResArrayAdapter.notifyDataSetChanged();
            }
        }, throwable -> Log.e(LOG_TAG, "scan error: " + throwable));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0 || position >= mScanResArrayList.size()) return;

        MyScanResult device = mScanResArrayList.get(position);
        if (!device.isConnected()) {
            Log.d(LOG_TAG, "Inside the connect method");
            connectBLEDevice(device);
        }
    }

    public void stopScan() {
        if (mScanSubscription != null) {
            mScanSubscription.dispose();
            mScanSubscription = null;
        }
    }

    public void connectBLEDevice(MyScanResult device) {
        Toast.makeText(getApplicationContext(), "Connecting, this may take a moment...", Toast.LENGTH_SHORT).show();

        RxBleDevice bleDevice = getBleClient().getBleDevice(device.macAddress);

        Log.i(LOG_TAG, "Connecting to BLE device: " + bleDevice.getMacAddress());

        stopScan();
        mMds.connect(bleDevice.getMacAddress(), new MdsConnectionListener() {

            @Override
            public void onConnect(String s) {
                Log.d(LOG_TAG, "onConnect:" + s);
            }

            @Override
            public void onConnectionComplete(String macAddress, String serial) {
                for (MyScanResult sr : mScanResArrayList) {
                    if (sr.macAddress.equalsIgnoreCase(macAddress)) {
                        sr.markConnected(serial);
                        break;
                    }
                }
                mScanResArrayAdapter.notifyDataSetChanged();
                connectedSerial = serial;
                Toast.makeText(getApplicationContext(), "Device Connected", Toast.LENGTH_SHORT).show();
                connectButton.setEnabled(true);
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "onError:" + e);

                showConnectionError(e);
            }

            @Override
            public void onDisconnect(String bleAddress) {
                Log.d(LOG_TAG, "onDisconnect: " + bleAddress);
                for (MyScanResult sr : mScanResArrayList) {
                    if (bleAddress.equals(sr.macAddress)) {
                        sr.markDisconnected();

                    }
                }
                mScanResArrayAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showConnectionError(MdsException e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Connection Error:").setMessage(e.getMessage());

        builder.create().show();
    }

    public void connectButtonClicked(View view) {
        Intent intent = new Intent(MainActivity.this, GestureActivity.class);
        startActivity(intent);
    }
}