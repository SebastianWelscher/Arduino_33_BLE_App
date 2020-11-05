package com.example.arduino33bleapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

class BleBluetoothHelper extends AppCompatActivity {

    Activity activity;

    TextView peripheralTextView;

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btLeScanner;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    Boolean btScanning = false;
    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered= new ArrayList<BluetoothDevice>();
    BluetoothGatt btGatt;

    public final static String ACTION_GATT_CONNECTED = "Gatt connected";
    public final static String ACTION_GATT_DISCONNECTED = "Gatt disconnected";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "Gatt services discovered";
    public final static String ACTION_DATA_AVAILABLE = "Data available";
    public final static String EXTRA_DATA = "EXTRA_DATA";

    public Map<String,String> uuids = new HashMap<String, String>();

    private Handler mHandler = new Handler();
    private final static int SCAN_PERIOD = 5000;

    BleBluetoothHelper(Activity activity){
        this.activity = activity;
    }

    public void checkBluetoothConnection(){
        if(btAdapter != null && !btAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
    }

    public void initBluetooth(){
        btManager = (BluetoothManager) getSystemService(activity.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btLeScanner = btAdapter.getBluetoothLeScanner();
    }

    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(activity)
                        .setTitle("Standortdatenzugriff benötigt")
                        .setMessage("App benötigt den zugriff auf den Standort um Bluetoth LE Geräte zu finden")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                activity.requestPermissions(
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        PERMISSION_REQUEST_COARSE_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            peripheralTextView.append("Index: " + deviceIndex + ", Device Name: " +
            result.getDevice().getName() + " rssi; " + result.getRssi() + "\n");
            devicesDiscovered.add(result.getDevice());
            deviceIndex++;

            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount())
                    - peripheralTextView.getHeight();

            if(scrollAmount > 0){
                peripheralTextView.scrollTo(0,scrollAmount);
            }
        }
    };

    private final BluetoothGattCallback  btleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    peripheralTextView.append("device read or wrote to\n");
                }
            });
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            System.out.println(newState);
            switch (newState){
                case 0:
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peripheralTextView.append("device disconnected\n");
                        }
                    });
                    break;
                case 2:
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peripheralTextView.append("device connected");
                        }
                    });
                    btGatt.discoverServices();
                    break;
                default:
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peripheralTextView.append("We encountered an unknown state");
                        }
                    });
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    peripheralTextView.append("device services have been discovered\n");
                }
            });
              displayGattServices(btGatt.getServices());
        }
    };

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic){
        System.out.println(characteristic.getUuid());
    }

    private void displayGattServices(List<BluetoothGattService> gattServices){

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        System.out.println("coarse location permission granted");
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }
}
