package com.sensors.bletarayici;

/**
 * Created by selcuk.yazar on 27.02.2019.
 */


import java.io.Serializable;
import com.google.gson.annotations.SerializedName;

public class myBlueToothDevice implements Serializable  {
    @SerializedName("mbdAddress")
    public String mbdAddress;
    @SerializedName("mdeviceName")
    public String mdeviceName;
    @SerializedName("mScanResponse")
    public String mScanResponse;
    @SerializedName("mRSSI")
    public String mRSSI;
    @SerializedName("mData")
    public String mData;
    @SerializedName("mTarih")
    public String mTarih;
}
