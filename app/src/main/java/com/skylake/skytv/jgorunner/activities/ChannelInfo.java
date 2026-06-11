package com.skylake.skytv.jgorunner.activities;

import android.os.Parcel;
import android.os.Parcelable;

public class ChannelInfo implements Parcelable {
    String videoUrl;
    String logoUrl;
    String channelName;
    int channelNumber;

    public ChannelInfo(String videoUrl, String logoUrl, String channelName, int channelNumber) {
        this.videoUrl = videoUrl;
        this.logoUrl = logoUrl;
        this.channelName = channelName;
        this.channelNumber = channelNumber;
    }

    protected ChannelInfo(Parcel in) {
        videoUrl = in.readString();
        logoUrl = in.readString();
        channelName = in.readString();
        channelNumber = in.readInt();
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
        dest.writeString(logoUrl);
        dest.writeString(channelName);
        dest.writeInt(channelNumber);
    }

    // Getters
    public String getVideoUrl() { return videoUrl; }
    public String getLogoUrl() { return logoUrl; }
    public String getChannelName() { return channelName; }
    public int getChannelNumber() { return channelNumber; }
}
