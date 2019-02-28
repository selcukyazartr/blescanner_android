package com.sensors.bletarayici;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.adrecord.AdRecord;

import static com.sensors.bletarayici.ScanResultAdapter.bytesToHex;



public class BackgroundService extends Service  implements Callback<myBlueToothDevice> {

    private boolean isRunning;
    private Context context;
    private Thread backgroundThread;
    private ApiService apiInterface;
    static final int DELAY = 60000; // a minute

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate()  {
        super.onCreate();
        this.context = this;
        this.isRunning = false;

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.connectTimeout(7000, TimeUnit.MILLISECONDS);
        httpClient.readTimeout(7000,TimeUnit.MILLISECONDS);
        httpClient.writeTimeout(7000, TimeUnit.MILLISECONDS);



        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiService.ENDPOINT)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiInterface = retrofit.create(ApiService.class);

        this.backgroundThread = new Thread(myTask);


    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(!this.isRunning) {
            this.isRunning = true;
            this.backgroundThread.start();
        }
        return START_STICKY;
    }

    private Runnable myTask = new Runnable() {
        public void run() {
            // Do something here
            Log.d("SERVICE", "SERVİS ÇALIŞIYOR");
            String txtdata="";

            for ( BluetoothLeDevice a : MainActivity.mDeviceStore.getDeviceList())
            {
                Log.d("SERVCHZ", a.getAddress());
                Log.d("SERVCHZ",String.format("%s",a.getRssi()));



                try {
                    JSONObject paramObject = new JSONObject();
                    paramObject.put("mbdAddress",a.getAddress());
                    String name = a.getDevice().getName();
                    if (name == null) {
                        name = "N/A.";
                    }
                    paramObject.put("mdeviceName",name);
                    for(AdRecord b : a.getAdRecordStore().getRecordsAsCollection())
                    {
                        if (b.getType() == 2 ||  b.getType() == 9 || b.getType() == 34 )
                            txtdata += bytesToHex(b.getData()).replaceAll("..(?=.)", "$0 ");

                    }
                    paramObject.put("mScanResponse", txtdata);
                    paramObject.put("mRSSI",  String.format("%s",a.getRssi()));
                    paramObject.put("mData",  txtdata);

                    paramObject.put("mTarih",  DateFormat.getDateTimeInstance().format(new Date()));

                    Log.d("JSON", paramObject.toString());

                    //Thread.sleep(DELAY);
                    Call userCall = apiInterface.getMyJSON(paramObject.toString());
                    userCall.enqueue(new Callback<myBlueToothDevice>() {
                        @Override
                        public void onResponse(Call<myBlueToothDevice> call, Response<myBlueToothDevice> response) {
                            Log.e("JSON","Success " + response.toString() + " " + response.headers() + " " + call.request().headers().toString());
                            MainActivity.mJSON_STATUS = "START";
                        }

                        @Override
                        public void onFailure(Call<myBlueToothDevice> call, Throwable t) {
                            // handle execution failures like no internet connectivity
                            t.printStackTrace();
                            Log.e("JSON","Failure " + t.getMessage() );
                        }
                    });
                    txtdata = "";
                } catch (Exception e) {
                    txtdata = "";
                    e.printStackTrace();
                }

            }
            stopSelf();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.isRunning = false;
        this.backgroundThread.interrupt();
    }



    @Override
    public void onResponse(Call<myBlueToothDevice> call, Response<myBlueToothDevice> response) {
        Log.e("JSON","Success...");
    }

    @Override
    public void onFailure(Call<myBlueToothDevice> call, Throwable t) {
        Log.e("JSON","Failure...");
    }
}