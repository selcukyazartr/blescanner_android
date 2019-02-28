package com.sensors.bletarayici;

import android.content.Context;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.adrecord.AdRecord;

/**
 * Created by selcuk.yazar on 27.02.2019.
 */

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder> {


    private ArrayList<BluetoothLeDevice> mArrayList;
    private Context mContext;
    private LayoutInflater mInflater;


    //Relative layout bileşeninde tanımlı nesnelerin bilgileri alınarak menzil  içindeki BLE cihazlar gösterilecek
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Her bir veri öğesi bu durumda sadece bir metin olarak alınıyor
        public TextView bdAddress, deviceName, ScanResponse,RSSI;
        public String mlastSeen;
        public ViewHolder(View v) {
            super(v);

            deviceName = (TextView) v.findViewById(R.id.large_name);
            bdAddress = (TextView) v.findViewById(R.id.bd_address);
            ScanResponse = (TextView) v.findViewById(R.id.scn_rsp);
            RSSI = (TextView) v.findViewById(R.id.rssi);
            mlastSeen = new String("");

        }
    }
    //Sınıfıa ait yapıcı ( Constructor)
    ScanResultAdapter(Context a, LayoutInflater inflater) {
        super();
        mContext = a;
        mInflater = inflater;
        mArrayList = new ArrayList<>();
    }

    //Oluşturulan adapter için override metotu.
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_relative, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        BluetoothLeDevice scanResult = mArrayList.get(position);

        Log.d("TRSCAN",scanResult.getAddress());
        String name = scanResult.getDevice().getName();
        if (name == null) {
            name = "N/A.";
        }
        holder.deviceName.setText(name);
        holder.bdAddress.setText(scanResult.getDevice().getAddress());
        holder.RSSI.setText(String.format("%s",scanResult.getRssi()));

        for(AdRecord a : scanResult.getAdRecordStore().getRecordsAsCollection())
        {
            if (a.getType() == 2 || a.getType() == 34 )
                holder.ScanResponse.setText(bytesToHex(a.getData()).replaceAll("..(?=.)", "$0 "));
            Log.d("REOCRDS",a.getHumanReadableType() + " " + a.toString() );
        }
        holder.mlastSeen = getTimeSinceString(mContext, scanResult.getTimestamp());
    }
    @Override
    public int getItemCount() {
        return mArrayList.size();
    }
    @Override
    public long getItemId(int position) {
        return mArrayList.get(position).getDevice().getAddress().hashCode();
    }
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4] ;
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];

        }

        return new String(hexChars);
    }
    //Oluştutlan adapter içinde mevcut cihaz adresinin aranması ve değerinin geri döndürülmesi için metot.Cihaz bulunamazsa -1 sonucu döndürülür.
    private int getPosition(String address) {
        int position = -1;
        for (int i = 0; i < mArrayList.size(); i++) {
            if (mArrayList.get(i).getDevice().getAddress().equals(address)) {
                position = i;
                break;
            }
        }
        return position;
    }

    //Adapter içinde bulunmayan  cihazın eklenmesi. Cihaz listede varsa, bilgileri güncellenir.
    public void add(BluetoothLeDevice scanResult) {

        int existingPosition = getPosition(scanResult.getDevice().getAddress());

        if (existingPosition >= 0) {
            // Device is already in list, update its record.
            mArrayList.set(existingPosition, scanResult);
            notifyDataSetChanged();
            Log.d("ADAPTER","DÃ¼zelt");

        } else {
            // Add new Device's ScanResult to list.
            Log.d("ADAPTER","Yeni");
            mArrayList.add(scanResult);
            notifyDataSetChanged();
        }
    }
    //Adapter içeriğini temizle
    public void clear() {
        mArrayList.clear();
    }
    //Cihazların en son görülmesine ait zaman bilgisi
    public static String getTimeSinceString(Context context, long timeNanoseconds) {
        String lastSeenText = context.getResources().getString(R.string.last_seen) + " ";

        long timeSince = SystemClock.elapsedRealtimeNanos() - timeNanoseconds;
        long secondsSince = TimeUnit.SECONDS.convert(timeSince, TimeUnit.NANOSECONDS);

        if (secondsSince < 5) {
            lastSeenText += context.getResources().getString(R.string.just_now);
        } else if (secondsSince < 60) {
            lastSeenText += secondsSince + " " + context.getResources()
                    .getString(R.string.seconds_ago);
        } else {
            long minutesSince = TimeUnit.MINUTES.convert(secondsSince, TimeUnit.SECONDS);
            if (minutesSince < 60) {
                if (minutesSince == 1) {
                    lastSeenText += minutesSince + " " + context.getResources()
                            .getString(R.string.minute_ago);
                } else {
                    lastSeenText += minutesSince + " " + context.getResources()
                            .getString(R.string.minute_ago);
                }
            } else {
                long hoursSince = TimeUnit.HOURS.convert(minutesSince, TimeUnit.MINUTES);
                if (hoursSince == 1) {
                    lastSeenText += hoursSince + " " + context.getResources()
                            .getString(R.string.hour_ago);
                } else {
                    lastSeenText += hoursSince + " " + context.getResources()
                            .getString(R.string.hour_ago);
                }
            }
        }

        return lastSeenText;
    }
}
