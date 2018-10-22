package io.canvas.colors.ui.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import blufi.espressif.BlufiCallback;
import blufi.espressif.BlufiClient;
import blufi.espressif.params.BlufiConfigureParams;
import blufi.espressif.params.BlufiParameter;
import blufi.espressif.response.BlufiScanResult;
import blufi.espressif.response.BlufiStatusResponse;
import blufi.espressif.response.BlufiVersionResponse;
import io.canvas.colors.BlufiConstants;
import io.canvas.colors.R;
import io.canvas.colors.data.WifiScanData;
import io.canvas.colors.databinding.ActivityWifiSearchBinding;
import io.canvas.colors.ui.adapter.WifiScanAdapter;

public class ConnectWifiActivity extends AppCompatActivity {

    ActivityWifiSearchBinding binding;

    private BlufiClient mBlufiClient;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mBluetoothDeviceAddress;

    private BluetoothAdapter mBluetoothAdapter;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private final static String TAG = "BLE GATT";

    public static String SSID = "";
    public static String SSID_PW = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wifi_search);

        initBluetooth();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(linearLayoutManager);

        //mAdapter = new WifiScanAdapter();
        //binding.recyclerView.setAdapter(mAdapter);

        connect(mDeviceAddress);

        binding.connectWifi.setOnClickListener(view -> { //와이파이 연결 요청
            if (SSID_PW.length() > 0) {
                connectWifi();
            } else {
                Toast.makeText(this, "Please Input PW", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void initBluetooth() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }
    }

    public void connectWifi() {
        Log.d("WIFICONNECT", SSID_PW);
        BlufiConfigureParams params = new BlufiConfigureParams();
        params.setOpMode(BlufiParameter.OP_MODE_STA);
        params.setStaSSID(SSID);
        params.setStaPassword(SSID_PW);
        mBlufiClient.configure(params);
        mBlufiClient.negotiateSecurity();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                BluetoothGattService service = gatt.getService(BlufiConstants.UUID_SERVICE);
                if (service == null) {
                    //mLog.w("Discover service failed");
                    gatt.disconnect();
                    //updateMessage("Discover service failed", false);
                    return;
                }

                BluetoothGattCharacteristic writeCharact = service.getCharacteristic(BlufiConstants.UUID_WRITE_CHARACTERISTIC);
                if (writeCharact == null) {
                    //mLog.w("Get write characteristic failed");
                    gatt.disconnect();
                    //updateMessage("Get write characteristic failed", false);
                    return;
                }

                BluetoothGattCharacteristic notifyCharact = service.getCharacteristic(BlufiConstants.UUID_NOTIFICATION_CHARACTERISTIC);
                if (notifyCharact == null) {
                    // mLog.w("Get notification characteristic failed");
                    gatt.disconnect();
                    //updateMessage("Get notification characteristic failed", false);
                    return;
                }

                //updateMessage("Discover service and characteristics success", false);

                if (mBlufiClient != null) {
                    mBlufiClient.close();
                }
                mBlufiClient = new BlufiClient(gatt, writeCharact, notifyCharact, new BlufiCallbackMain());

                gatt.setCharacteristicNotification(notifyCharact, true);

                //WIFI검색
                mBlufiClient.requestDeviceWifiScan();

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("Characteristic", "onCharacteristicWrite " + status);
            // This is requirement
            mBlufiClient.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            mBlufiClient.onCharacteristicChanged(gatt, characteristic);
        }
    };

    private class BlufiCallbackMain extends BlufiCallback {
        //        @Override
//        public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
//            switch (status) {
//                case STATUS_SUCCESS:
//                    StringBuilder msg = new StringBuilder();
//                    msg.append("Receive device scan result:\n");
//                    for (BlufiScanResult scanResult : results) {
//                        msg.append(scanResult.toString()).append("\n");
//                    }
//                    Log.d("Blufi", msg.toString());
//                    break;
//                default:
//                    Log.e("Blufi", "Device scan result error");
//                    break;
//            }
//        }
        @Override
        public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
            List<WifiScanData> models = new ArrayList<>();
            // status is the result: "0" - successful, otherwise - failed.
            switch (status) {
                case STATUS_SUCCESS:
                    for (BlufiScanResult scanResult : results) {
                        String ssid = scanResult.getSsid(); // Obtain Wi-Fi SSID
                        String rssi = String.valueOf(scanResult.getRssi()); // Obtain Wi-Fi RSSI
                        Log.d("Blufi", "SSID: " + ssid + "  RSSI: " + rssi + "\n");
                        models.add(new WifiScanData(ssid, rssi));
                    }
                    WifiScanAdapter mAdapter = new WifiScanAdapter(getApplicationContext(), models);
                    binding.recyclerView.setAdapter(mAdapter);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
            // status is the result of encryption: "0" - successful, otherwise - failed.

            switch (status) {
                case STATUS_SUCCESS:
                    String version = response.getVersionString(); // Get the version number
                    Log.d("Blufi", version);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onNegotiateSecurityResult(BlufiClient client, int status) {
            switch (status) {
                case STATUS_SUCCESS:
                    Log.d("Blufi", "Negotiate security complete");
                    break;
                default:
                    Log.e("Blufi", "Negotiate security failed");
                    break;
            }
        }

        @Override
        public void onConfigureResult(BlufiClient client, int status) {
            switch (status) {
                case STATUS_SUCCESS:
                    Log.d("Blufi", "Post configure params complete");
                    break;
                default:
                    Log.e("Blufi", "Post configure params failed");
                    break;
            }
        }

        @Override
        public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
            switch (status) {
                case STATUS_SUCCESS:
                    Log.d("Blufi", String.format("Receive device status response:\n%s", response.generateValidInfo()));
                    Toast.makeText(getApplicationContext(), response.generateValidInfo(), Toast.LENGTH_SHORT).show();
                    if (response.generateValidInfo().contains("Sta connect wifi now")) {
                        Intent goMain = new Intent(ConnectWifiActivity.this, MainActivity.class);
                        startActivity(goMain);
                    }
                    break;
                default:
                    Log.e("Blufi", "Device status response error");
                    break;
            }
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                //mConnectionState = STATE_CONNECTING;
            } else {
                Toast.makeText(this, "연결 중 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            Toast.makeText(this, "연결 중 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        //mConnectionState = STATE_CONNECTING;
    }
}