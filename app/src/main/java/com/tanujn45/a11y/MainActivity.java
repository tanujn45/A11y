package com.tanujn45.a11y;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    public static Mds mMds;
    public static String connectedSerial;
    private static RxBleClient mBleClient;
    private Disposable mScanSubscription;
    private final ArrayList<MyScanResult> mScanResArrayList = new ArrayList<>();
    private ArrayAdapter<MyScanResult> mScanResArrayAdapter;
    private Button connectButton, homeButton;
    ListView mScanResultListView;
    MyScanResult selectedDevice;
    private boolean permissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI elements
        connectButton = findViewById(R.id.connectButton);
        homeButton = findViewById(R.id.homeButton);
        mScanResultListView = findViewById(R.id.bluetoothListView);

        // Check for and request permissions
        permissionsGranted = requestNeededPermissions();

        // Initialize the Mds object
        initMds();

        // Initialize the scan result adapter
        initMScanResAdapter();
    }


    /**
     * Initialize the scan result adapter
     * Sets the adapter for the mScanResultListView
     * Sets the onItemClickListener for the mScanResultListView
     * Sets padding for the TextView inside the default layout
     * Notifies the mScanResArrayAdapter
     * Logs any errors
     */
    private void initMScanResAdapter() {
        mScanResArrayAdapter = new ArrayAdapter<MyScanResult>(this, android.R.layout.simple_list_item_1, mScanResArrayList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Apply padding to the TextView inside the default layout
                TextView textView = view.findViewById(android.R.id.text1);
                float newSizeInSP = 23;
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSizeInSP);
                textView.setPadding(20, 30, 20, 30); // Set padding in pixels (left, top, right, bottom)

                return view;
            }
        };
        mScanResultListView.setAdapter(mScanResArrayAdapter);
        mScanResultListView.setOnItemClickListener(this);
    }


    /**
     * Get the RxBleClient object
     * If the object is null, create a new RxBleClient object
     *
     * @return: The RxBleClient object
     */
    private RxBleClient getBleClient() {
        if (mBleClient == null) {
            mBleClient = RxBleClient.create(this);
        }

        return mBleClient;
    }


    /**
     * Initialize the Mds object
     */
    private void initMds() {
        mMds = Mds.builder().build(this);
    }


    /**
     * Check if the app has the necessary permissions
     * If the app does not have the necessary permissions, return false
     * If the app has the necessary permissions, return true
     * Log the permissions that are granted and not granted
     */
    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission not granted: " + permission);
                    return false;
                } else {
                    System.out.println("Permission granted: " + permission);
                }
            }
        }
        return true;
    }


    /**
     * Request permissions needed for the app to function
     * Request the necessary permissions
     * Call hasPermissions() to confirm check if the app has the necessary permissions
     * Return true if the app has the necessary permissions
     */
    private boolean requestNeededPermissions() {
        int PERMISSION_ALL = 1;

        // Permissions needed for the app to function
        String[] PERMISSIONS = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.CAMERA,
        };

        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);

        // Call hasPermissions() to confirm check if the app has the necessary permissions
        return hasPermissions(this, PERMISSIONS);
    }


    /**
     * Start scanning for BLE devices
     * Clears the mScanResArrayList and notifies the mScanResArrayAdapter
     * Subscribes to the scanBleDevices method of the RxBleClient
     * Adds the scanned devices to the mScanResArrayList
     * Notifies the mScanResArrayAdapter
     * Logs any errors
     */
    public void startScan() {
        connectButton.setText("Scanning");
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


    /**
     * Stop scanning for BLE devices
     * Disposes the mScanSubscription
     */
    public void stopScan() {
        if (mScanSubscription != null) {
            mScanSubscription.dispose();
            mScanSubscription = null;
        }
    }


    /**
     * onItemClick method for the mScanResultListView
     * Connects to the BLE device that was clicked
     * If the device is not connected, connect to the device
     *
     * @param parent: The AdapterView where the click happened
     * @param view: The view within the AdapterView that was clicked
     * @param position: The position of the view in the adapter
     * @param id: The row id of the item that was clicked
     *
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0 || position >= mScanResArrayList.size()) return;

        MyScanResult device = mScanResArrayList.get(position);
        if (!device.isConnected()) {
            connectBLEDevice(device);
        }
    }


    /**
     * Connect to the BLE device
     * Connect to the BLE device using the Mds object
     * If the connection is successful, mark the device as connected
     * Notify the mScanResArrayAdapter
     * Set the connectedSerial
     * Enable the homeButton
     * Set the homeButton background tint
     *
     * @param device: The BLE device to connect to
     */
    public void connectBLEDevice(MyScanResult device) {
        // Toast.makeText(getApplicationContext(), "Connecting, this may take a moment...", Toast.LENGTH_SHORT).show();
        connectButton.setText("Connecting");
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
                // Toast.makeText(getApplicationContext(), "Device Connected", Toast.LENGTH_SHORT).show();
                connectButton.setText("Scan");
                homeButton.setEnabled(true);
                // Set home button background tint
                homeButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.theme));
                selectedDevice = device;
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


    /**
     * Disconnect the BLE device
     * Disconnect the BLE device using the Mds object
     * If the device is connected, disconnect the device
     */
    public void disconnectBLEDevice() {
        if(mMds != null && selectedDevice != null && selectedDevice.isConnected()) {
            mMds.disconnect(selectedDevice.macAddress);
            selectedDevice.markDisconnected();

        }
    }


    /**
     * Show an alert dialog with the error message
     *
     * @param e: The MdsException object
     */
    private void showConnectionError(MdsException e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Connection Error:").setMessage(e.getMessage());

        builder.create().show();
    }


    /**
     * Called when the connect button is clicked
     *
     * @param view: The view that was clicked
     */
    public void connectButtonClicked(View view) {
        // Check if the required permissions are granted
        if (!permissionsGranted) {
            permissionsGranted = requestNeededPermissions();
            System.out.println("Permissions granted: " + permissionsGranted);
        } else {
            // Stop scanning if already scanning and disable the homeButton
            stopScan();
            homeButton.setEnabled(false);
            homeButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.theme2));
            disconnectBLEDevice();

            // Start scanning for bluetooth devices
            startScan();
        }
    }

    /**
     * Called when the home button is clicked
     *
     * @param view: The view that was clicked
     */
    public void homeButtonClicked(View view) {
        // Go to the HomeActivity
         Intent intent = new Intent(this, GestureActivity.class);
         startActivity(intent);
    }
}