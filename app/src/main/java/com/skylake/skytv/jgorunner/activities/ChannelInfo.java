package com.skylake.skytv.jgorunner.activities;

import android.os.Parcel;
import android.os.Parcelable;

public class ChannelInfo implements Parcelable {
    String videoUrl;
    String keyUrl;
    String logoUrl;
    String channelName;

    public ChannelInfo(String videoUrl, String keyUrl, String logoUrl, String channelName) {
        this.videoUrl = videoUrl;
        this.keyUrl = keyUrl;
        this.logoUrl = logoUrl;
        this.channelName = channelName;
    }

    protected ChannelInfo(Parcel in) {
        videoUrl = in.readString();
        keyUrl = in.readString();
        logoUrl = in.readString();
        channelName = in.readString();
    }

    public static final Creator<ChannelInfo> CREATOR = new Creator<>() {
        @Override
        public ChannelInfo createFromParcel(Parcel in) {
            return new ChannelInfo(in);
        }

        @Override
        public ChannelInfo[] newArray(int size) {
            return new ChannelInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(videoUrl);
        dest.writeString(keyUrl);
        dest.writeString(logoUrl);
        dest.writeString(channelName);
    }

    // Getters
    public String getVideoUrl() { return videoUrl; }
    public String getKeyUrl() { return keyUrl; }
    public String getLogoUrl() { return logoUrl; }
    public String getChannelName() { return channelName; }
}
