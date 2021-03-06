package com.example.alexandre.rollingspiderpilotingwear;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dronit.adapter.Adapter;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceBLEService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.parrot.arsdk.arsal.ARSALPrint;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate, WearableListView.ClickListener {

    private static String TAG = MainActivity.class.getSimpleName();

    static
    {
        try
        {
            System.loadLibrary("arsal");
            System.loadLibrary("arsal_android");
            System.loadLibrary("arnetworkal");
            System.loadLibrary("arnetworkal_android");
            System.loadLibrary("arnetwork");
            System.loadLibrary("arnetwork_android");
            System.loadLibrary("arcommands");
            System.loadLibrary("arcommands_android");
            System.loadLibrary("ardiscovery");
            System.loadLibrary("ardiscovery_android");

            ARSALPrint.enableDebugPrints();

        }
        catch (Exception e)
        {
            Log.e(TAG, "Oops (LoadLibrary)", e);
        }
    }

    private WearableListView listView ;
    private List<ARDiscoveryDeviceService> deviceList;
    private String[] deviceNameList;

    private ARDiscoveryService ardiscoveryService;
    private boolean ardiscoveryServiceBound = false;
    private ServiceConnection ardiscoveryServiceConnection;
    public IBinder discoveryServiceBinder;

    private BroadcastReceiver ardiscoveryServicesDevicesListUpdatedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServices();

        // init brocast receiver
        ardiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        initServiceConnection();

        listView = (WearableListView) findViewById(R.id.wearable_list);

        deviceList = new ArrayList<ARDiscoveryDeviceService>();
        deviceNameList = new String[]{"Searching for drones..."};

        // Assign adapter to ListView
        listView.setAdapter(new Adapter(this, deviceNameList));

        listView.setClickListener(this);
    }

    private void startServices()
    {
        //startService(new Intent(this, ARDiscoveryService.class));
    }

    private void initServices()
    {
        if (discoveryServiceBinder == null)
        {
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, ardiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        }
        else
        {
            ardiscoveryService = ((ARDiscoveryService.LocalBinder) discoveryServiceBinder).getService();
            ardiscoveryServiceBound = true;

            try {
                Thread.sleep(1000);
                if(ardiscoveryService != null)
                    ardiscoveryService.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void closeServices()
    {
        Log.d(TAG, "closeServices ...");

        if (ardiscoveryServiceBound)
        {
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    ardiscoveryService.stop();

                    getApplicationContext().unbindService(ardiscoveryServiceConnection);
                    ardiscoveryServiceBound = false;
                    discoveryServiceBinder = null;
                    ardiscoveryService = null;
                }
            }).start();


        }
    }

    private void initServiceConnection()
    {
        ardiscoveryServiceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {
                Log.d("DronItWear","onServiceConnected");
                discoveryServiceBinder = service;
                ardiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
                ardiscoveryServiceBound = true;

                Log.d("DronItWear","onServiceConnected : " + ardiscoveryService);

                try {
                    Thread.sleep(1000);
                    if(ardiscoveryService != null)
                        ardiscoveryService.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onServiceDisconnected(ComponentName name)
            {
                ardiscoveryService = null;
                ardiscoveryServiceBound = false;
            }
        };
    }

    private void registerReceivers()
    {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(ardiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));

    }

    private void unregisterReceivers()
    {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(ardiscoveryServicesDevicesListUpdatedReceiver);
    }


    @Override
    public void onResume()
    {
        super.onResume();

        Log.d(TAG, "onResume ...");

        onServicesDevicesListUpdated();

        registerReceivers();

        initServices();

    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause ...");

        unregisterReceivers();
        closeServices();

        super.onPause();
    }

    @Override
    public void onServicesDevicesListUpdated() {
        Log.d(TAG, "onServicesDevicesListUpdated ...");

        List<ARDiscoveryDeviceService> list;

        if (ardiscoveryService != null)
        {
            list = ardiscoveryService.getDeviceServicesArray();

            deviceList = new ArrayList<ARDiscoveryDeviceService> ();
            List<String> deviceNames = new ArrayList<String>();

            if(list != null)
            {
                for (ARDiscoveryDeviceService service : list)
                {
                    Log.d(TAG, "service :  "+ service);
                    if (service.getDevice() instanceof ARDiscoveryDeviceBLEService)
                    {
                        deviceList.add(service);
                        deviceNames.add(service.getName());
                    }
                }
            }

            deviceNameList = deviceNames.toArray(new String[deviceNames.size()]);

            // Assign adapter to ListView
            listView.setAdapter(new Adapter(this, deviceNameList));
        }
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        // ListView Clicked item index

        Integer itemPosition = (Integer) viewHolder.itemView.getTag();
        Log.d("DronIt", itemPosition + " - " + viewHolder.itemView);

        ARDiscoveryDeviceService service = deviceList.get(itemPosition);

        Intent intent = new Intent(MainActivity.this, PilotingActivity.class);
        intent.putExtra(PilotingActivity.EXTRA_DEVICE_SERVICE, service);

        startActivity(intent);
    }

    @Override
    public void onTopEmptyRegionClick() {

    }
}
