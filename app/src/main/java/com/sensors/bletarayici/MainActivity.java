package com.sensors.bletarayici;


import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;

public class MainActivity extends AppCompatActivity {

    //Bluetooth cihazlarını bulabilmek için gerekli nesne tanımları

    private BluetoothLeScanner mScanner;
    public static BluetoothLeDeviceStore mDeviceStore;
    private BluetoothUtils mBluetoothUtils;
    private ScanResultAdapter mAdapter;


    private Handler mHandler;
    private RecyclerView recycler_view;

    private static final String TAG ="MyBLEScan";
    public static String mSCAN_STATUS="STOP";
    public static String mJSON_STATUS="STOP";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    TextView blelocationText ;

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            final BluetoothLeDevice deviceLe = new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis());
            mDeviceStore.addDevice(deviceLe);

            final List<BluetoothLeDevice> itemList = new ArrayList<>();
            itemList.add(deviceLe);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.add(deviceLe);
                    mAdapter.notifyDataSetChanged();

                }
            });
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDeviceStore = new BluetoothLeDeviceStore();
        mBluetoothUtils = new BluetoothUtils(this);
        mScanner = new BluetoothLeScanner(mLeScanCallback, mBluetoothUtils);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //Oreo da bu kısım olmalı..
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }
        //Bileşenlerin bulunması
        blelocationText = (TextView) findViewById(R.id.bletextView);
        recycler_view = (RecyclerView) findViewById(R.id.recycler_view);


        mAdapter = new ScanResultAdapter(this, LayoutInflater.from(MainActivity.this));
        mHandler = new Handler();
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recycler_view.setLayoutManager(mLayoutManager);
        recycler_view.setItemAnimator(new DefaultItemAnimator());
        recycler_view.setAdapter(mAdapter);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothUtils.askUserToEnableBluetoothIfNeeded();

        if (!mBluetoothUtils.isBluetoothOn())
        {
            blelocationText.setTextColor(Color.RED);
            mSCAN_STATUS="STOP";
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);


        Intent alarm = new Intent(this, AlarmReceiver.class);
        boolean alarmRunning = (PendingIntent.getBroadcast(this, 0, alarm, PendingIntent.FLAG_NO_CREATE) != null);
        if(alarmRunning == false) {
            pendingIntent = PendingIntent.getBroadcast(this, 0, alarm, 0);
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 3000, pendingIntent);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_ON:
                        startScanning();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        stopScanning();
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTED:
                        stopScanning();
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        stopScanning();
                        break;
                    case  BluetoothAdapter.STATE_TURNING_OFF:
                        stopScanning();
                        break;
                }
            }
        }
    };

    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent background = new Intent(context, BackgroundService.class);
            context.startService(background);
        }

    }

    public void startScanning() {
        final boolean isBluetoothOn = mBluetoothUtils.isBluetoothOn();
        final boolean isBluetoothLePresent = mBluetoothUtils.isBluetoothLeSupported();
        mDeviceStore.clear();

        if (isBluetoothOn && isBluetoothLePresent) {
            blelocationText.setTextColor(Color.GREEN);
            mSCAN_STATUS="START";
            mScanner.scanLeDevice(-1, true);
            invalidateOptionsMenu();

        }
        else
        {
            blelocationText.setTextColor(Color.RED);
            mSCAN_STATUS="STOP";
        }
    }

    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");
        blelocationText.setTextColor(Color.RED);
        mSCAN_STATUS="STOP";
        mScanner.scanLeDevice(-1, false);
        stopService(new Intent(MainActivity.this,BackgroundService.class));
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDestroy()
    {
        stopScanning();
        super.onDestroy();
    }
    @Override
    protected void onPause() {

        if (mBluetoothUtils.isBluetoothOn()) {
            blelocationText.setTextColor(Color.GREEN);
            mSCAN_STATUS = "START";
        }
        else
        {blelocationText.setTextColor(Color.RED);
            mSCAN_STATUS="STOP";
        }

        stopScanning();


        super.onPause();
    }
    protected void onResume(){
        super.onResume();
        if (mBluetoothUtils.isBluetoothOn()) {
            blelocationText.setTextColor(Color.GREEN);
            mSCAN_STATUS = "START";
            mScanner.scanLeDevice(-1, true);
        }
        else
        {blelocationText.setTextColor(Color.RED);
            mSCAN_STATUS = "STOP";}

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 3000, pendingIntent);
        registerReceiver(this.mReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, yay! Start the Bluetooth device scan.
                } else {
                    // Alert the user that this application requires the location permission to perform the scan.
                }
            }
        }
    }

}
