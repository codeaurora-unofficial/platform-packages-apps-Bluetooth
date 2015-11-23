/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of The Linux Foundation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.bluetooth.avrcp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import com.android.bluetooth.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import android.util.Log;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import android.media.session.PlaybackState;
import android.media.MediaMetadata;
import android.media.MediaMetadataEditor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
/**
 * Provides helper classes used by other AvrcpControllerClasses.
 */
class AvrcpUtils {

    private static final String TAG = "AvrcpControllerClasses_AvrcpUtils";
    /*
     * First 2 apis are utility functions to converts values from AvrcpPlayerSettings defined
     * in BluetoothAvrcpPlayerSettings to BT spec defined Id and Vals.
     */
    public static int mapAttribIdValtoAvrcpPlayerSetting( byte attribId, byte attribVal) {
        if(AvrcpControllerConstants.VDBG) Log.d(TAG, "attribId: " + attribId + " attribVal: " + attribVal);
        if (attribId == AvrcpControllerConstants.ATTRIB_EQUALIZER_STATUS) {
            switch(attribVal) {
            case AvrcpControllerConstants.EQUALIZER_STATUS_OFF:
                return BluetoothAvrcpPlayerSettings.STATE_OFF;
            case AvrcpControllerConstants.EQUALIZER_STATUS_ON:
                return BluetoothAvrcpPlayerSettings.STATE_ON;
            }
        }
        else if (attribId == AvrcpControllerConstants.ATTRIB_REPEAT_STATUS) {
            switch(attribVal) {
            case AvrcpControllerConstants.REPEAT_STATUS_ALL_TRACK_REPEAT:
                return BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
            case AvrcpControllerConstants.REPEAT_STATUS_GROUP_REPEAT:
                return BluetoothAvrcpPlayerSettings.STATE_GROUP;
            case AvrcpControllerConstants.REPEAT_STATUS_OFF:
                return BluetoothAvrcpPlayerSettings.STATE_OFF;
            case AvrcpControllerConstants.REPEAT_STATUS_SINGLE_TRACK_REPEAT:
                return BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK;
            }
        }
        else if (attribId == AvrcpControllerConstants.ATTRIB_SCAN_STATUS) {
            switch(attribVal) {
            case AvrcpControllerConstants.SCAN_STATUS_ALL_TRACK_SCAN:
                return BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
            case AvrcpControllerConstants.SCAN_STATUS_GROUP_SCAN:
                return BluetoothAvrcpPlayerSettings.STATE_GROUP;
            case AvrcpControllerConstants.SCAN_STATUS_OFF:
                return BluetoothAvrcpPlayerSettings.STATE_OFF;
            }
        }
        else if (attribId == AvrcpControllerConstants.ATTRIB_SHUFFLE_STATUS) {
            switch(attribVal) {
            case AvrcpControllerConstants.SHUFFLE_STATUS_ALL_TRACK_SHUFFLE:
                return BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK;
            case AvrcpControllerConstants.SHUFFLE_STATUS_GROUP_SHUFFLE:
                return BluetoothAvrcpPlayerSettings.STATE_GROUP;
            case AvrcpControllerConstants.SHUFFLE_STATUS_OFF:
                return BluetoothAvrcpPlayerSettings.STATE_OFF;
            }
        }
        return BluetoothAvrcpPlayerSettings.STATE_INVALID;
    }
    public static int mapAvrcpPlayerSettingstoBTAttribVal(int mSetting, int mSettingVal) {
        if (mSetting == BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER) {
            switch(mSettingVal) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                return AvrcpControllerConstants.EQUALIZER_STATUS_OFF;
            case BluetoothAvrcpPlayerSettings.STATE_ON:
                return AvrcpControllerConstants.EQUALIZER_STATUS_ON;
            }
        }
        else if (mSetting == BluetoothAvrcpPlayerSettings.SETTING_REPEAT) {
            switch(mSettingVal) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                return AvrcpControllerConstants.REPEAT_STATUS_OFF;
            case BluetoothAvrcpPlayerSettings.STATE_SINGLE_TRACK:
                return AvrcpControllerConstants.REPEAT_STATUS_SINGLE_TRACK_REPEAT;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                return AvrcpControllerConstants.REPEAT_STATUS_ALL_TRACK_REPEAT;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                return AvrcpControllerConstants.REPEAT_STATUS_GROUP_REPEAT;
            }
        }
        else if (mSetting == BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE) {
            switch(mSettingVal) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                return AvrcpControllerConstants.SHUFFLE_STATUS_OFF;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                return AvrcpControllerConstants.SHUFFLE_STATUS_ALL_TRACK_SHUFFLE;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                return AvrcpControllerConstants.SHUFFLE_STATUS_GROUP_SHUFFLE;
            }
        }
        else if (mSetting == BluetoothAvrcpPlayerSettings.SETTING_SCAN) {
            switch(mSettingVal) {
            case BluetoothAvrcpPlayerSettings.STATE_OFF:
                return AvrcpControllerConstants.SCAN_STATUS_OFF;
            case BluetoothAvrcpPlayerSettings.STATE_ALL_TRACK:
                return AvrcpControllerConstants.SCAN_STATUS_ALL_TRACK_SCAN;
            case BluetoothAvrcpPlayerSettings.STATE_GROUP:
                return AvrcpControllerConstants.SCAN_STATUS_GROUP_SCAN;
            }
        }
        return AvrcpControllerConstants.STATUS_INVALID;
    }
    /*
     * This api converts btPlayStatus to PlaybackState
     */
    public static PlaybackState mapBtPlayStatustoPlayBackState(byte btPlayStatus, long btPlayPos) {
        int mState = PlaybackState.STATE_NONE;
        long position = btPlayPos;
        float speed = 1;
        switch(btPlayStatus) {
            case AvrcpControllerConstants.PLAY_STATUS_STOPPED:
                mState = PlaybackState.STATE_STOPPED;
                position = 0;
                speed = 0;
            break;
            case AvrcpControllerConstants.PLAY_STATUS_PLAYING:
                mState = PlaybackState.STATE_PLAYING;
            break;
            case AvrcpControllerConstants.PLAY_STATUS_PAUSED:
                mState = PlaybackState.STATE_PAUSED;
                speed = 0;
            break;
            case AvrcpControllerConstants.PLAY_STATUS_FWD_SEEK:
                mState = PlaybackState.STATE_FAST_FORWARDING;
                speed = 3;
            break;
            case AvrcpControllerConstants.PLAY_STATUS_REV_SEEK:
                mState = PlaybackState.STATE_REWINDING;
                speed = -3;
            break;
        }
        return new PlaybackState.Builder().setState(mState, position, speed).build();
    }
    /*
     * This api converts meta info into MediaMetaData
     */
    public static MediaMetadata getMediaMetaData(TrackInfo mTrackInfo) {
        if(AvrcpControllerConstants.VDBG) Log.d(TAG, " TrackInfo " + mTrackInfo.toString());
        MediaMetadata.Builder mMetaDataBuilder = new MediaMetadata.Builder();
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST,
                mTrackInfo.mArtistName);
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE,
                mTrackInfo.mTrackTitle);
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM,
                mTrackInfo.mAlbumTitle);
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_GENRE,
                mTrackInfo.mGenre);
        mMetaDataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER,
                mTrackInfo.mTrackNum);
        mMetaDataBuilder.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS,
                mTrackInfo.mTotalTracks);
        mMetaDataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION,
                mTrackInfo.mTrackLen);
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID,
                String.valueOf(mTrackInfo.mItemUid));
        if ((mTrackInfo.mThumbNailLocation != null) && (!mTrackInfo.mThumbNailLocation.
                equals(AvrcpControllerConstants.COVER_ART_LOCATION_INVALID))) {
            Bitmap mThumbNail = BitmapFactory.decodeFile(mTrackInfo.mThumbNailLocation);
            if(AvrcpControllerConstants.VDBG) Log.d(TAG, " put thumbnail " +
                                                            mTrackInfo.mThumbNailLocation);
            mMetaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mThumbNail);
        }
        if ((mTrackInfo.mImageLocation != null) && (!mTrackInfo.mImageLocation.
                equals(AvrcpControllerConstants.COVER_ART_LOCATION_INVALID))) {
            if(AvrcpControllerConstants.VDBG) Log.d(TAG, " put Image " +
                    mTrackInfo.mImageLocation);
            mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                    mTrackInfo.mImageLocation);
        }
        return mMetaDataBuilder.build();
    }
    /*
     * Display Apis
     */
    public static String displayMetaData(MediaMetadata mMetaData) {
        StringBuffer sb = new StringBuffer();
        /* this will only show artist, title and album */
        sb.append(mMetaData.getDescription().toString() + " ");
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_GENRE))
            sb.append(mMetaData.getString(MediaMetadata.METADATA_KEY_GENRE) + " ");
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_MEDIA_ID))
            sb.append(mMetaData.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) + " ");
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
            sb.append(Long.toString(mMetaData.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER)) + " ");
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_NUM_TRACKS))
            sb.append(Long.toString(mMetaData.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS)) + " ");
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
            sb.append(Long.toString(mMetaData.getLong(MediaMetadata.METADATA_KEY_DURATION)) + " ");
        if(mMetaData.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
            sb.append(Long.toString(mMetaData.getLong(MediaMetadata.METADATA_KEY_DURATION)) + " ");
        return sb.toString();
    }
    public static String displayBluetoothAvrcpSettings(BluetoothAvrcpPlayerSettings mSett) {
        StringBuffer sb =  new StringBuffer();
        int supportedSetting = mSett.getSettings();
        if(AvrcpControllerConstants.VDBG) Log.d(TAG," setting: " + supportedSetting);
        if((supportedSetting & BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER) != 0) {
            sb.append(" EQ : ");
            sb.append(Integer.toString(mSett.getSettingValue(BluetoothAvrcpPlayerSettings.
                                                             SETTING_EQUALIZER)));
        }
        if((supportedSetting & BluetoothAvrcpPlayerSettings.SETTING_REPEAT) != 0) {
            sb.append(" REPEAT : ");
            sb.append(Integer.toString(mSett.getSettingValue(BluetoothAvrcpPlayerSettings.
                                                             SETTING_REPEAT)));
        }
        if((supportedSetting & BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE) != 0) {
            sb.append(" SHUFFLE : ");
            sb.append(Integer.toString(mSett.getSettingValue(BluetoothAvrcpPlayerSettings.
                                                             SETTING_SHUFFLE)));
        }
        if((supportedSetting & BluetoothAvrcpPlayerSettings.SETTING_SCAN) != 0) {
            sb.append(" SCAN : ");
            sb.append(Integer.toString(mSett.getSettingValue(BluetoothAvrcpPlayerSettings.
                                                             SETTING_SCAN)));
        }
        return sb.toString();
    }
}
/*
 * Contains information about remote device
 */
class RemoteDevice {
    BluetoothDevice mBTDevice;
    int mRemoteFeatures;
    int mBatteryStatus;
    int mSystemStatus;
    int mAbsVolNotificationState;
    int mNotificationLabel;
    AvrcpBipInitiator mAvrcpBipInitiator;
    int mBipL2capPsm;
    private static final String TAG = "AvrcpControllerClasses_RemoteDevice";

    public void cleanup() {
        if(mAvrcpBipInitiator != null) {
            mAvrcpBipInitiator.cleanup();
            mAvrcpBipInitiator =  null;
        }
        mBTDevice = null;
        mRemoteFeatures = AvrcpControllerConstants.BTRC_FEAT_NONE;
        mBatteryStatus = AvrcpControllerConstants.BATT_POWER_UNDEFINED;
        mSystemStatus = AvrcpControllerConstants.SYSTEM_STATUS_UNDEFINED;
        mAbsVolNotificationState = AvrcpControllerConstants.DEFER_VOLUME_CHANGE_RSP;
        mNotificationLabel = AvrcpControllerConstants.VOLUME_LABEL_UNDEFINED;
    }

    public RemoteDevice(BluetoothDevice mDevice) {
        mBTDevice = mDevice;
        mRemoteFeatures = AvrcpControllerConstants.BTRC_FEAT_NONE;
        mBatteryStatus = AvrcpControllerConstants.BATT_POWER_UNDEFINED;
        mSystemStatus = AvrcpControllerConstants.SYSTEM_STATUS_UNDEFINED;
        mAbsVolNotificationState = AvrcpControllerConstants.DEFER_VOLUME_CHANGE_RSP;
        mNotificationLabel = AvrcpControllerConstants.VOLUME_LABEL_UNDEFINED;
        mAvrcpBipInitiator =  null;
        mBipL2capPsm = AvrcpControllerConstants.DEFAULT_PSM;
    }

    public boolean isBrowsingSupported() {
        if((mRemoteFeatures & AvrcpControllerConstants.BTRC_FEAT_BROWSE) != 0)
            return true;
        else
           return false;
    }

    public boolean isMetaDataSupported() {
        if((mRemoteFeatures & AvrcpControllerConstants.BTRC_FEAT_METADATA) != 0)
            return true;
        else
           return false;
    }

    public boolean isCoverArtSupported() {
        if((mRemoteFeatures & AvrcpControllerConstants.BTRC_FEAT_COVER_ART) != 0)
            return true;
        else
           return false;
    }

    public void connectBip(int psm) {
        if(mBTDevice == null)
            return;
        Log.d(TAG," connectBip psm " + psm);
        if(mAvrcpBipInitiator == null)
            mAvrcpBipInitiator = new AvrcpBipInitiator(mBTDevice, psm);
        if (!mAvrcpBipInitiator.isObexConnected())
            mAvrcpBipInitiator.connect();
    }

    public void GetLinkedThumbnail(String imgHandle) {

        if ((mAvrcpBipInitiator != null) && (mAvrcpBipInitiator.isObexConnected())) {
            mAvrcpBipInitiator.GetLinkedThumbnail(imgHandle);
        }
    }

    public void GetImage(String imgHandle, String encoding, String pixel, long maxSize) {

        if ((mAvrcpBipInitiator != null) && (mAvrcpBipInitiator.isObexConnected())) {
            mAvrcpBipInitiator.GetImage(imgHandle, encoding, pixel, maxSize);
        }
    }

    public boolean isBipConnected() {
        if(mAvrcpBipInitiator != null)
            return mAvrcpBipInitiator.isObexConnected();
        return false;
    }
}

/*
 * Base Class for Media Item
 */
class MediaItem {
    /*
     * This is a combination of locaiton and item. Spec Snippet
     * In VFS if same item is in different location it may have same uid.
     * In Now Playing same item should have differnt UID
     * Can never be 0, used only for GetElementAttributes
     * TODO: UID counter, which is used for database aware player
     */
    double mItemUid;
}

/*
 * Contains information Player Application Setting
 */
class PlayerApplicationSettings {
    public byte attr_Id;
    public byte attr_val;
    public byte [] supported_values;
    public String attr_text;
    public String [] supported_values_text;// This is to keep displayable text in UTF-8
}
/*
 * Contains information about remote player
 */
class PlayerInfo {
    private static final String TAG = "AvrcpControllerClasses_PlayerInfo";
    byte mPlayStatus;
    long mPlayTime;
    /*
     * 2 byte player id to identify player.
     * In 1.3 this value will be set to zero
     */
    char mPlayerId;
    ArrayList<PlayerApplicationSettings> mPlayerAppSetting;
    private void resetPlayer() {
        mPlayStatus = AvrcpControllerConstants.PLAY_STATUS_STOPPED;
        mPlayTime   = AvrcpControllerConstants.PLAYING_TIME_INVALID;
        mPlayerId   = 0;
        mPlayerAppSetting = new ArrayList<PlayerApplicationSettings>();
    }
    public PlayerInfo() {
        resetPlayer();
    }
    public void setSupportedPlayerAppSetting (ByteBuffer bb) {
        /* ByteBuffer has to be of the following format
         * id, num_values, values[]
         */
        while(bb.hasRemaining()) {
            PlayerApplicationSettings plAppSetting = new PlayerApplicationSettings();
            plAppSetting.attr_Id = bb.get();
            byte numSupportedVals = bb.get();
            plAppSetting.supported_values = new byte[numSupportedVals];
            for (int i = 0; i<numSupportedVals; i++) {
                plAppSetting.supported_values[i] = bb.get();
            }
            mPlayerAppSetting.add(plAppSetting);
        }
    }
    public void updatePlayerAppSetting(ByteBuffer bb) {
        /* ByteBuffer has to be of the following format
         * <id, value>
         */
        if(mPlayerAppSetting.isEmpty())
            return;
        while(bb.hasRemaining()) {
            byte attribId = bb.get();
            for(PlayerApplicationSettings plAppSetting: mPlayerAppSetting) {
                if(plAppSetting.attr_Id == attribId)
                    plAppSetting.attr_val = bb.get();
            }
        }
    }

    public BluetoothAvrcpPlayerSettings getSupportedPlayerAppSetting() {
        /*
         * Here we create PlayerAppSetting
         * based on BluetoothAvrcpPlayerSettings
         */
        int supportedSettings = 0; // Player App Setting used by BluetoothAvrcpPlayerSettings.
        for(PlayerApplicationSettings plAppSetting: mPlayerAppSetting) {
            switch(plAppSetting.attr_Id) {
            case AvrcpControllerConstants.ATTRIB_EQUALIZER_STATUS:
                supportedSettings |= BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER;
                break;
            case AvrcpControllerConstants.ATTRIB_REPEAT_STATUS:
                supportedSettings |= BluetoothAvrcpPlayerSettings.SETTING_REPEAT;
                break;
            case AvrcpControllerConstants.ATTRIB_SCAN_STATUS:
                supportedSettings |= BluetoothAvrcpPlayerSettings.SETTING_SCAN;
                break;
            case AvrcpControllerConstants.ATTRIB_SHUFFLE_STATUS:
                supportedSettings |= BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE;
                break;
            }
        }
        BluetoothAvrcpPlayerSettings mAvrcpPlayerAppSetting = new
                                  BluetoothAvrcpPlayerSettings(supportedSettings);
        for(PlayerApplicationSettings plAppSetting: mPlayerAppSetting) {
            switch(plAppSetting.attr_Id) {
            case AvrcpControllerConstants.ATTRIB_EQUALIZER_STATUS:
                mAvrcpPlayerAppSetting.addSettingValue(
                           BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER,
                           AvrcpUtils.mapAttribIdValtoAvrcpPlayerSetting(plAppSetting.attr_Id,
                                plAppSetting.attr_val));
                break;
            case AvrcpControllerConstants.ATTRIB_REPEAT_STATUS:
                mAvrcpPlayerAppSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT,
                        AvrcpUtils.mapAttribIdValtoAvrcpPlayerSetting(plAppSetting.attr_Id,
                                plAppSetting.attr_val));
                break;
            case AvrcpControllerConstants.ATTRIB_SCAN_STATUS:
                mAvrcpPlayerAppSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN,
                        AvrcpUtils.mapAttribIdValtoAvrcpPlayerSetting(plAppSetting.attr_Id,
                                plAppSetting.attr_val));
                break;
            case AvrcpControllerConstants.ATTRIB_SHUFFLE_STATUS:
                mAvrcpPlayerAppSetting.addSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE,
                        AvrcpUtils.mapAttribIdValtoAvrcpPlayerSetting(plAppSetting.attr_Id,
                                plAppSetting.attr_val));
                break;
            }
        }
        return mAvrcpPlayerAppSetting;
    }
    public byte getCurrentPlayerAppSettingValue(byte mPlayerAppAttrId) {
        for(PlayerApplicationSettings plAppSetting: mPlayerAppSetting) {
            if(mPlayerAppAttrId == plAppSetting.attr_Id)
                return plAppSetting.attr_val;
        }
        return 0;
    }
    /*
     * Checks if current setting is supported by remote.
     * input would be in form of flattened strucuture <id,val>
     */
    public boolean isPlayerAppSettingSupported(byte numAttributes, byte[] playerAppSetting) {
        for( int i = 0; (i < 2*numAttributes);) {
            byte id = playerAppSetting[i++];
            byte val = playerAppSetting[i++];
            boolean found = false;
            for(PlayerApplicationSettings plAppSetting: mPlayerAppSetting) {
                if(plAppSetting.attr_Id == id) {
                    for(int j = 0; j < plAppSetting.supported_values.length; j++) {
                        if(val == plAppSetting.supported_values[j]) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            if(!found)
                return false;
        }
        return true;
    }
}

/*
 * Contains information about track
 */
class TrackInfo extends MediaItem {
    String mArtistName;
    String mTrackTitle;
    String mAlbumTitle;
    String mGenre;
    long mTrackNum; // number of audio file on original recording.
    long mTotalTracks;// total number of tracks on original recording
    long mTrackLen;// full length of AudioFile.
    String mCoverArtHandle;
    String mThumbNailLocation;
    String mImageLocation;
    /* In case of 1.3 we have to set itemUid explicitly to 0 */

    /* reset it to default values */
    private void resetTrackInfo() {
        mArtistName = AvrcpControllerConstants.ARTIST_NAME_INVALID;;
        mTrackTitle = AvrcpControllerConstants.TITLE_INVALID;;
        mAlbumTitle = AvrcpControllerConstants.ALBUM_NAME_INVALID;
        mGenre      = AvrcpControllerConstants.GENRE_INVALID;
        mTrackNum   = AvrcpControllerConstants.TRACK_NUM_INVALID;
        mTotalTracks = AvrcpControllerConstants.TOTAL_TRACK_TIME_INVALID;
        mTrackLen = AvrcpControllerConstants.TOTAL_TRACK_TIME_INVALID;
        mCoverArtHandle = AvrcpControllerConstants.COVER_ART_HANDLE_INVALID;
        mThumbNailLocation =  AvrcpControllerConstants.COVER_ART_LOCATION_INVALID;
        mImageLocation =  AvrcpControllerConstants.COVER_ART_LOCATION_INVALID;
    }
    public TrackInfo() {
        resetTrackInfo();
    }
    public TrackInfo(int mTrackId, byte mNumAttributes, int[] mAttribIds, String[] mAttribs) {
        mItemUid = mTrackId;
        resetTrackInfo();
        for (int i = 0; i < mNumAttributes; i++) {
            switch(mAttribIds[i]) {
            case AvrcpControllerConstants.MEDIA_ATTRIBUTE_TITLE:
                mTrackTitle = mAttribs[i];
                break;
            case AvrcpControllerConstants.MEDIA_ATTRIBUTE_ARTIST_NAME:
                mArtistName = mAttribs[i];
                break;
            case AvrcpControllerConstants.MEDIA_ATTRIBUTE_ALBUM_NAME:
                mAlbumTitle = mAttribs[i];
                break;
            case AvrcpControllerConstants.MEDIA_ATTRIBUTE_TRACK_NUMBER:
                if(!mAttribs[i].isEmpty())
                    mTrackNum = Long.valueOf(mAttribs[i]);
                break;
            case AvrcpControllerConstants.MEDIA_ATTRIBUTE_TOTAL_TRACK_NUMBER:
                if(!mAttribs[i].isEmpty())
                    mTotalTracks = Long.valueOf(mAttribs[i]);
                break;
            case AvrcpControllerConstants.MEDIA_ATTRIBUTE_GENRE:
                mGenre = mAttribs[i];
                break;
            case AvrcpControllerConstants.MEDIA_ATTRIBUTE_PLAYING_TIME:
                if(!mAttribs[i].isEmpty())
                    mTrackLen = Long.valueOf(mAttribs[i]);
                break;
            case AvrcpControllerConstants.MEDIA_ATTRIBUTE_COVER_ART_HANDLE:
                mCoverArtHandle = mAttribs[i];
                break;
            }
        }
    }
    public String toString() {
        return "Metadata [artist=" + mArtistName + " trackTitle= " + mTrackTitle +
                " albumTitle= " + mAlbumTitle + " genre= " +mGenre+" trackNum= "+
                Long.toString(mTrackNum) + " track_len : "+ Long.toString(mTrackLen) +
                " TotalTracks " + Long.toString(mTotalTracks) + "]";
    }
}
class AppProperties {
    boolean isCoverArtRequested;
    String mSupportedCoverArtMimetype;
    int mSupportedCovertArtHeight;
    int mSupportedCoverArtWidth;
    long mSupportedCoverArtMaxSize;

    void resetAppProperties() {
        isCoverArtRequested = false;
        mSupportedCoverArtMimetype = "JPEG";
        mSupportedCovertArtHeight = 500;
        mSupportedCoverArtWidth = 500;
        mSupportedCoverArtMaxSize = 200000;
    }
    public AppProperties() {
        resetAppProperties();
    }
}
