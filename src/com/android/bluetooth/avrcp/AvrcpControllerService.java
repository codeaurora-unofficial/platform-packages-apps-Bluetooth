/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 * Not a Contribution
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.avrcp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothAvrcpController;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import com.android.bluetooth.avrcp.Avrcp.Metadata;
import com.android.bluetooth.a2dp.A2dpSinkService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import android.util.Log;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
/**
 * Provides Bluetooth AVRCP Controller profile, as a service in the Bluetooth application.
 * @hide
 */
public class AvrcpControllerService extends ProfileService {
    private static final boolean DBG = AvrcpControllerConstants.DBG;
    private static final boolean VDBG = AvrcpControllerConstants.VDBG;
    private static final String TAG = "AvrcpControllerService";

/*
 *  Messages handled by mHandler
 */

    RemoteDevice mAvrcpRemoteDevice;
    RemoteFileSystem mRemoteFileSystem;
    RemoteMediaPlayers mRemoteMediaPlayers;
    NowPlaying mRemoteNowPlayingList;
    AppProperties mAppProperties;

    private AvrcpMessageHandler mHandler;
    private static AvrcpControllerService sAvrcpControllerService;
    private static AudioManager mAudioManager;

    private final ArrayList<BluetoothDevice> mConnectedDevices
            = new ArrayList<BluetoothDevice>();

    static {
        classInitNative();
    }

    public AvrcpControllerService() {
        initNative();
    }

    protected String getName() {
        return TAG;
    }

    protected IProfileServiceBinder initBinder() {
        Log.d(TAG," initBinder Called ");
        return new BluetoothAvrcpControllerBinder(this);
    }

    protected boolean start() {
        HandlerThread thread = new HandlerThread("BluetoothAvrcpHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mHandler = new AvrcpMessageHandler(looper);
        setAvrcpControllerService(this);
        mAudioManager = (AudioManager)sAvrcpControllerService.
                                  getSystemService(Context.AUDIO_SERVICE);
        mAppProperties = new AppProperties();
        return true;
    }

    protected void resetRemoteData() {
        try {
            unregisterReceiver(mBroadcastReceiver);
        }
        catch (IllegalArgumentException e) {
            Log.e(TAG,"Receiver not registered");
        }
        if(mAvrcpRemoteDevice != null) {
            mAvrcpRemoteDevice.cleanup();
            mAvrcpRemoteDevice = null;
        }
        if(mRemoteFileSystem != null) {
            mRemoteFileSystem.cleanup();
            mRemoteFileSystem = null;
        }
        if(mRemoteMediaPlayers != null) {
            mRemoteMediaPlayers.cleanup();
            mRemoteMediaPlayers = null;
        }
        if(mRemoteNowPlayingList != null) {
            mRemoteNowPlayingList.cleanup();
            mRemoteNowPlayingList = null;
        }
        if (mAppProperties != null)
            mAppProperties.resetAppProperties();
    }
    protected boolean stop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            Looper looper = mHandler.getLooper();
            if (looper != null) {
                looper.quit();
            }
        }
        resetRemoteData();
        return true;
    }

    protected boolean cleanup() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            Looper looper = mHandler.getLooper();
            if (looper != null) {
                looper.quit();
            }
        }
        resetRemoteData();
        clearAvrcpControllerService();
        cleanupNative();
        return true;
    }

    //API Methods

    public static synchronized AvrcpControllerService getAvrcpControllerService(){
        if (sAvrcpControllerService != null && sAvrcpControllerService.isAvailable()) {
            if (DBG) Log.d(TAG, "getAvrcpControllerService(): returning "
                    + sAvrcpControllerService);
            return sAvrcpControllerService;
        }
        if (DBG)  {
            if (sAvrcpControllerService == null) {
                Log.d(TAG, "getAvrcpControllerService(): service is NULL");
            } else if (!(sAvrcpControllerService.isAvailable())) {
                Log.d(TAG,"getAvrcpControllerService(): service is not available");
            }
        }
        return null;
    }

    private static synchronized void setAvrcpControllerService(AvrcpControllerService instance) {
        if (instance != null && instance.isAvailable()) {
            if (DBG) Log.d(TAG, "setAvrcpControllerService(): set to: " + sAvrcpControllerService);
            sAvrcpControllerService = instance;
        } else {
            if (DBG)  {
                if (sAvrcpControllerService == null) {
                    Log.d(TAG, "setAvrcpControllerService(): service not available");
                } else if (!sAvrcpControllerService.isAvailable()) {
                    Log.d(TAG,"setAvrcpControllerService(): service is cleaning up");
                }
            }
        }
    }

    private static synchronized void clearAvrcpControllerService() {
        sAvrcpControllerService = null;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return mConnectedDevices;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        for (int i = 0; i < states.length; i++) {
            if (states[i] == BluetoothProfile.STATE_CONNECTED) {
                return mConnectedDevices;
            }
        }
        return new ArrayList<BluetoothDevice>();
    }

    int getConnectionState(BluetoothDevice device) {
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return (mConnectedDevices.contains(device) ? BluetoothProfile.STATE_CONNECTED
                                                : BluetoothProfile.STATE_DISCONNECTED);
    }

    public void onBipConnected(BluetoothDevice mDevice) {
        Log.v(TAG," onBipConnected device = " + mDevice);
        if ((mAvrcpRemoteDevice != null) && (mAvrcpRemoteDevice.mBTDevice.equals(mDevice))) {
            mHandler.sendEmptyMessage(AvrcpControllerConstants.MESSAGE_PROCESS_BIP_CONNECTED);
        }
    }
    public void onBipDisconnected() {
        Log.d(TAG," onBipDisconnected");
        mHandler.sendEmptyMessage(AvrcpControllerConstants.MESSAGE_PROCESS_BIP_DISCONNECTED);
    }
    public void onThumbNailFetched(String mImageHandle, String mImageLocation) {
        Log.v(TAG," onBipImageFetched HDL = " + mImageHandle + " Location" + mImageLocation);
        if (mAvrcpRemoteDevice != null) {
            ArrayList<String> bipResults = new ArrayList<String>();
            bipResults.add(mImageHandle);
            bipResults.add(mImageLocation);
            Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                                     MESSAGE_PROCESS_THUMB_NAIL_FETCHED, bipResults);
            mHandler.sendMessage(msg);
        }
    }
    public void onImageFetched(String mImageHandle, String mImageLocation) {
        Log.v(TAG," onBipImageFetched HDL = " + mImageHandle + " Location " + mImageLocation);
        if (mAvrcpRemoteDevice != null) {
            ArrayList<String> bipResults = new ArrayList<String>();
            bipResults.add(mImageHandle);
            bipResults.add(mImageLocation);
            Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                                     MESSAGE_PROCESS_IMAGE_FETCHED, bipResults);
            mHandler.sendMessage(msg);
        }
    }
    public void sendGroupNavigationCmd(BluetoothDevice device, int keyCode, int keyState) {
        Log.v(TAG, "sendGroupNavigationCmd keyCode: " + keyCode + " keyState: " + keyState);
        if (device == null) {
            throw new NullPointerException("device == null");
        }
        if (!(mConnectedDevices.contains(device))) {
            Log.d(TAG," Device does not match");
            return;
        }
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_SEND_GROUP_NAVIGATION_CMD,keyCode, keyState, device);
        mHandler.sendMessage(msg);
    }

    public void sendPassThroughCmd(BluetoothDevice device, int keyCode, int keyState) {
        Log.v(TAG, "sendPassThroughCmd keyCode: " + keyCode + " keyState: " + keyState);
        if (device == null) {
            throw new NullPointerException("device == null");
        }
        if (!(mConnectedDevices.contains(device))) {
            Log.d(TAG," Device does not match");
            return;
        }
        if ((mAvrcpRemoteDevice == null)||
            (mAvrcpRemoteDevice.mRemoteFeatures == AvrcpControllerConstants.BTRC_FEAT_NONE)||
            (mRemoteMediaPlayers == null) ||
            (keyCode == AvrcpControllerConstants.PTS_GET_ELEMENT_ATTRIBUTE_ID)||
            (keyCode == AvrcpControllerConstants.PTS_GET_PLAY_STATUS_ID)||
            (mRemoteMediaPlayers.getAddressedPlayer() == null)){
            Log.d(TAG," Device connected but PlayState not present ");
            enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
            Message msg = mHandler.obtainMessage(AvrcpControllerConstants.MESSAGE_SEND_PASS_THROUGH_CMD,
                    keyCode, keyState, device);
            mHandler.sendMessage(msg);
            return;
        }
        boolean sendCommand = false;
        switch(keyCode) {
            case BluetoothAvrcpController.PASS_THRU_CMD_ID_PLAY:
                sendCommand  = (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_STOPPED)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_PAUSED);
                break;
            case BluetoothAvrcpController.PASS_THRU_CMD_ID_PAUSE:
            /*
             * allowing pause command in pause state to handle A2DP Sink Concurrency
             * If call is ongoing and Start is initiated from remote, we will send pause again
             * If acquireFocus fails, we will send Pause again
             * To Stop sending multiple Pause, check in BT-TestApp
             */
                sendCommand  = (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_PLAYING)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_FWD_SEEK)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_STOPPED)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_PAUSED)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_REV_SEEK);
                break;
            case BluetoothAvrcpController.PASS_THRU_CMD_ID_STOP:
                sendCommand  = (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_PLAYING)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_FWD_SEEK)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_REV_SEEK)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_STOPPED)||
                               (mRemoteMediaPlayers.getPlayStatus() == 
                                       AvrcpControllerConstants.PLAY_STATUS_PAUSED);
                break;
            case BluetoothAvrcpController.PASS_THRU_CMD_ID_BACKWARD:
            case BluetoothAvrcpController.PASS_THRU_CMD_ID_FORWARD:
            case BluetoothAvrcpController.PASS_THRU_CMD_ID_FF:
            case BluetoothAvrcpController.PASS_THRU_CMD_ID_REWIND:
                sendCommand = true; // we can send this command in all states
                break;
        }
        if (sendCommand) {
            enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
            Message msg = mHandler.obtainMessage(AvrcpControllerConstants.MESSAGE_SEND_PASS_THROUGH_CMD,
                keyCode, keyState, device);
            mHandler.sendMessage(msg);
        }
        else {
            Log.e(TAG," Not in right state, don't send Pass Thru cmd ");
        }
    }

    public MediaMetadata getMetaData(BluetoothDevice device) {
        Log.d(TAG, "getMetaData = ");
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if((mRemoteNowPlayingList != null) && (mRemoteNowPlayingList.getCurrentTrack() != null)) {
            return getCurrentMetaData(AvrcpControllerConstants.AVRCP_SCOPE_NOW_PLAYING, 0);
        }
        else
            return null;
    }
    public PlaybackState getPlaybackState(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getPlayBackState device = "+ device);
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return getCurrentPlayBackState();
    }
    public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getPlayerApplicationSetting ");
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return getCurrentPlayerAppSetting();
    }
    public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings plAppSetting) {
        if ((mAvrcpRemoteDevice == null)||(mRemoteMediaPlayers == null)) {
            return false;
        }
        enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        /*
         * We have to extract values from BluetoothAvrcpPlayerSettings
         */
        int mSettings = plAppSetting.getSettings();
        int numAttributes = 0;
        /* calculate number of attributes in request */
        while(mSettings > 0) {
            numAttributes += ((mSettings & 0x01)!= 0)?1: 0;
            mSettings = mSettings >> 1;
        }
        byte[] attribArray = new byte [2*numAttributes];
        mSettings = plAppSetting.getSettings();
        /*
         * Now we will flatten it <id, val>
         */
        int i = 0;
        if((mSettings & BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER) != 0) {
            attribArray[i++] = AvrcpControllerConstants.ATTRIB_EQUALIZER_STATUS;
            attribArray[i++] = (byte)AvrcpUtils.mapAvrcpPlayerSettingstoBTAttribVal(
                    BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER, plAppSetting.
                    getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_EQUALIZER));
        }
        if((mSettings & BluetoothAvrcpPlayerSettings.SETTING_REPEAT) != 0) {
            attribArray[i++] = AvrcpControllerConstants.ATTRIB_REPEAT_STATUS;
            attribArray[i++] = (byte)AvrcpUtils.mapAvrcpPlayerSettingstoBTAttribVal(
                    BluetoothAvrcpPlayerSettings.SETTING_REPEAT, plAppSetting.
                    getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_REPEAT));
        }
        if((mSettings & BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE) != 0) {
            attribArray[i++] = AvrcpControllerConstants.ATTRIB_SHUFFLE_STATUS;
            attribArray[i++] = (byte)AvrcpUtils.mapAvrcpPlayerSettingstoBTAttribVal(
                    BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE, plAppSetting.
                    getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SHUFFLE));
        }
        if((mSettings & BluetoothAvrcpPlayerSettings.SETTING_SCAN) != 0) {
            attribArray[i++] = AvrcpControllerConstants.ATTRIB_SCAN_STATUS;
            attribArray[i++] = (byte)AvrcpUtils.mapAvrcpPlayerSettingstoBTAttribVal(
                    BluetoothAvrcpPlayerSettings.SETTING_SCAN, plAppSetting.
                    getSettingValue(BluetoothAvrcpPlayerSettings.SETTING_SCAN));
        }
        boolean isSettingSupported = mRemoteMediaPlayers.getAddressedPlayer().
                                   isPlayerAppSettingSupported((byte)numAttributes, attribArray);
        if(isSettingSupported) {
            ByteBuffer bb = ByteBuffer.wrap(attribArray, 0, (2*numAttributes));
             Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_SEND_SET_CURRENT_PLAYER_APPLICATION_SETTINGS, numAttributes, 0, bb);
            mHandler.sendMessage(msg);
        }
        return isSettingSupported;
    }

    public void startFetchingAlbumArt(String mimeType, int height, int width, long maxSize) {
        Log.d(TAG," startFetchingAlbumArt mimeType " + mimeType + " pixel " + height + " * "
                + width + " maxSize: " + maxSize);
        if (mAppProperties == null) return;
        mAppProperties.isCoverArtRequested = true;
        mAppProperties.mSupportedCoverArtMimetype = mimeType;
        mAppProperties.mSupportedCoverArtWidth = width;
        mAppProperties.mSupportedCovertArtHeight = height;
        mAppProperties.mSupportedCoverArtMaxSize = maxSize;
        mHandler.sendEmptyMessage(AvrcpControllerConstants.MESSAGE_CONNECT_BIP);
    }
    //Binder object: Must be static class or memory leak may occur
    private static class BluetoothAvrcpControllerBinder extends IBluetoothAvrcpController.Stub
        implements IProfileServiceBinder {
        private AvrcpControllerService mService;

        private AvrcpControllerService getService() {
            if (!Utils.checkCaller()) {
                Log.w(TAG,"AVRCP call not allowed for non-active user");
                return null;
            }

            if (mService != null && mService.isAvailable()) {
                return mService;
            }
            return null;
        }

        BluetoothAvrcpControllerBinder(AvrcpControllerService svc) {
            mService = svc;
        }

        public boolean cleanup()  {
            mService = null;
            return true;
        }

        @Override
        public List<BluetoothDevice> getConnectedDevices() {
            AvrcpControllerService service = getService();
            if (service == null) return new ArrayList<BluetoothDevice>(0);
            return service.getConnectedDevices();
        }

        @Override
        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
            AvrcpControllerService service = getService();
            if (service == null) return new ArrayList<BluetoothDevice>(0);
            return service.getDevicesMatchingConnectionStates(states);
        }

        @Override
        public int getConnectionState(BluetoothDevice device) {
            AvrcpControllerService service = getService();
            if (service == null) return BluetoothProfile.STATE_DISCONNECTED;
            return service.getConnectionState(device);
        }

        @Override
        public void sendPassThroughCmd(BluetoothDevice device, int keyCode, int keyState) {
            Log.v(TAG,"Binder Call: sendPassThroughCmd");
            AvrcpControllerService service = getService();
            if (service == null) return;
            service.sendPassThroughCmd(device, keyCode, keyState);
        }
        public void sendGroupNavigationCmd(BluetoothDevice device, int keyCode, int keyState) {
            Log.v(TAG,"Binder Call: sendGroupNavigationCmd");
            AvrcpControllerService service = getService();
            if (service == null) return;
            service.sendGroupNavigationCmd(device, keyCode, keyState);
        }

        public MediaMetadata getMetadata(BluetoothDevice device) {
            Log.v(TAG,"Binder Call: getMetaData ");
            AvrcpControllerService service = getService();
            if (service == null) return null;
            return service.getMetaData(device);
        }
        public PlaybackState getPlaybackState(BluetoothDevice device) {
            Log.v(TAG,"Binder Call: getPlaybackState");
            AvrcpControllerService service = getService();
            if (service == null) return null;
            return service.getPlaybackState(device);
        }
        public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice device) {
            Log.v(TAG,"Binder Call: getPlayerApplicationSetting ");
            AvrcpControllerService service = getService();
            if (service == null) return null;
            return service.getPlayerSettings(device);
        }
        public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings plAppSetting) {
            Log.v(TAG,"Binder Call: setPlayerApplicationSetting " );
            AvrcpControllerService service = getService();
            if (service == null) return false;
            return service.setPlayerApplicationSetting(plAppSetting);
        }
        public void startFetchingAlbumArt(String mimeType, int height, int width, long maxSize) {
            AvrcpControllerService service = getService();
            if (service == null) return;
            service.startFetchingAlbumArt(mimeType, height, width, maxSize);
        }
    };

    private String utf8ToString(byte[] input)
    {
        Charset UTF8_CHARSET = Charset.forName("UTF-8");
        return new String(input,UTF8_CHARSET);
    }
    private int asciiToInt(int len, byte[] array)
    {
        return Integer.parseInt(utf8ToString(array));
    }
    private BluetoothAvrcpPlayerSettings getCurrentPlayerAppSetting() {
        if((mRemoteMediaPlayers == null) || (mRemoteMediaPlayers.getAddressedPlayer() == null))
            return null;
        return mRemoteMediaPlayers.getAddressedPlayer().getSupportedPlayerAppSetting();
    }
    private PlaybackState getCurrentPlayBackState() {
        if ((mRemoteMediaPlayers == null) || (mRemoteMediaPlayers.getAddressedPlayer() == null)) {
            return new PlaybackState.Builder().setState(PlaybackState.STATE_ERROR,
                                                        PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                                                        0).build();
        }
        return AvrcpUtils.mapBtPlayStatustoPlayBackState(
                mRemoteMediaPlayers.getAddressedPlayer().mPlayStatus,
                mRemoteMediaPlayers.getAddressedPlayer().mPlayTime);
    }
    private MediaMetadata getCurrentMetaData(int scope, int trackId) {
        /* if scope is now playing */
        if(scope == AvrcpControllerConstants.AVRCP_SCOPE_NOW_PLAYING) {
            if((mRemoteNowPlayingList == null) || (mRemoteNowPlayingList.
                                                           getTrackFromId(trackId) == null))
                return null;
            TrackInfo mNowPlayingTrack = mRemoteNowPlayingList.getTrackFromId(trackId);
            return AvrcpUtils.getMediaMetaData(mNowPlayingTrack);
        }
        /* if scope is now playing */
        else if(scope == AvrcpControllerConstants.AVRCP_SCOPE_VFS) {
            /* TODO for browsing */
        }
        return null;
    }
    private void broadcastMetaDataChanged(MediaMetadata mMetaData) {
        Intent intent = new Intent(BluetoothAvrcpController.ACTION_TRACK_EVENT);
        intent.putExtra(BluetoothAvrcpController.EXTRA_METADATA, mMetaData);
        if(DBG) Log.d(TAG," broadcastMetaDataChanged = " +
                                                   AvrcpUtils.displayMetaData(mMetaData));
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }
    private void broadcastPlayBackStateChanged(PlaybackState mState) {
        Intent intent = new Intent(BluetoothAvrcpController.ACTION_TRACK_EVENT);
        intent.putExtra(BluetoothAvrcpController.EXTRA_PLAYBACK, mState);
        if(DBG) Log.d(TAG," broadcastPlayBackStateChanged = " + mState.toString());
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }
    private void broadcastPlayerAppSettingChanged(BluetoothAvrcpPlayerSettings mPlAppSetting) {
        Intent intent = new Intent(BluetoothAvrcpController.ACTION_PLAYER_SETTING);
        intent.putExtra(BluetoothAvrcpController.EXTRA_PLAYER_SETTING, mPlAppSetting);
        if(DBG) Log.d(TAG," broadcastPlayerAppSettingChanged = " +
                AvrcpUtils.displayBluetoothAvrcpSettings(mPlAppSetting));
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }
    /** Handles Avrcp messages. */
    private final class AvrcpMessageHandler extends Handler {
        private AvrcpMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG," HandleMessage: "+ AvrcpControllerConstants.dumpMessageString(msg.what) +
                  " Remote Connected " + !mConnectedDevices.isEmpty());
            switch (msg.what) {
            case AvrcpControllerConstants.MESSAGE_SEND_PASS_THROUGH_CMD:
                BluetoothDevice device = (BluetoothDevice)msg.obj;
                if (msg.arg1 == AvrcpControllerConstants.PTS_GET_ELEMENT_ATTRIBUTE_ID) {
                    // Hack for PTS
                    byte numAttribs = 2;
                    byte[] attribs = new byte[numAttribs];
                    attribs[0] = AvrcpControllerConstants.MEDIA_ATTRIBUTE_TITLE;
                    attribs[1] = AvrcpControllerConstants.MEDIA_ATTRIBUTE_COVER_ART_HANDLE;
                    getElementAttributesNative(getByteAddress(device), numAttribs, attribs);
                    break;
                }
                sendPassThroughCommandNative(getByteAddress(device), msg.arg1, msg.arg2);
                break;
            case AvrcpControllerConstants.MESSAGE_SEND_GROUP_NAVIGATION_CMD:
                BluetoothDevice peerDevice = (BluetoothDevice)msg.obj;
                sendGroupNavigationCommandNative(getByteAddress(peerDevice), msg.arg1, msg.arg2);
                break;
            case AvrcpControllerConstants.MESSAGE_SEND_SET_CURRENT_PLAYER_APPLICATION_SETTINGS:
                byte numAttributes = (byte)msg.arg1;
                ByteBuffer bbRsp = (ByteBuffer)msg.obj;
                byte[] attributeIds = new byte [numAttributes];
                byte[] attributeVals = new byte [numAttributes];
                for(int i = 0; (bbRsp.hasRemaining())&&(i < numAttributes); i++) {
                    attributeIds[i] = bbRsp.get();
                    attributeVals[i] = bbRsp.get();
                }
                setPlayerApplicationSettingValuesNative(getByteAddress(mAvrcpRemoteDevice.mBTDevice),
                        numAttributes, attributeIds, attributeVals);
                break;
            case AvrcpControllerConstants.MESSAGE_CONNECT_BIP:
                /*
                 * If Avrcp Connection is UP and we have psm, try connecting BIP connection.
                 */
                if ((mAvrcpRemoteDevice == null) || (!mAvrcpRemoteDevice.isCoverArtSupported()))
                    return;
                if (!mAvrcpRemoteDevice.isBipConnected()) {
                    if (mAvrcpRemoteDevice.mBipL2capPsm != AvrcpControllerConstants.DEFAULT_PSM) {
                        mAvrcpRemoteDevice.connectBip(mAvrcpRemoteDevice.mBipL2capPsm);
                    }
                }
                else {
                    /*
                     * BIP is already connected in this case. Fetch Image
                     */
                    mHandler.sendEmptyMessage(AvrcpControllerConstants.
                                                        MESSAGE_PROCESS_BIP_CONNECTED);
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_CONNECTION_CHANGE:
                int newState = msg.arg1;
                int oldState = msg.arg2;
                BluetoothDevice rtDevice =  (BluetoothDevice)msg.obj;
                if ((newState == BluetoothProfile.STATE_CONNECTED) &&
                    (oldState == BluetoothProfile.STATE_DISCONNECTED)) {
                    /* We create RemoteDevice and MediaPlayerList here 
                     * Now playing list after RC features
                     */
                    if(mAvrcpRemoteDevice == null){
                        mAvrcpRemoteDevice =  new RemoteDevice(rtDevice);
                        /* Remote will have a player irrespective of AVRCP Version
                         * Create a Default player, we will add entries in case Browsing
                         * is supported by remote
                         */
                        if(mRemoteMediaPlayers == null) {
                            mRemoteMediaPlayers = new RemoteMediaPlayers(mAvrcpRemoteDevice);
                            PlayerInfo mPlayer = new PlayerInfo();
                            mPlayer.mPlayerId = 0;
                            mRemoteMediaPlayers.addPlayer(mPlayer);
                            mRemoteMediaPlayers.setAddressedPlayer(mPlayer);
                        }
                    }
                }
                else if ((newState == BluetoothProfile.STATE_DISCONNECTED) &&
                        (oldState == BluetoothProfile.STATE_CONNECTED)) /* connection down */
                {
                    resetRemoteData();
                    mHandler.removeCallbacksAndMessages(null);
                }
                /*
                 * Send intent now
                 */
                Intent intent = new Intent(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED);
                intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, oldState);
                intent.putExtra(BluetoothProfile.EXTRA_STATE, newState);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, rtDevice);
//              intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_RC_FEATURES:
                if(mAvrcpRemoteDevice == null)
                    break;
                Log.d(TAG," rc features " + msg.arg1);
                mAvrcpRemoteDevice.mRemoteFeatures = msg.arg1;
                /* in case of AVRCP version < 1.3, no need to add track info */
                if(mAvrcpRemoteDevice.isMetaDataSupported()) {
                    if(mRemoteNowPlayingList == null)
                        mRemoteNowPlayingList = new NowPlaying(mAvrcpRemoteDevice);
                    TrackInfo mTrack = new TrackInfo();
                    /* First element of NowPlayingList will be current Track 
                     * for 1.3 this will be the only song
                     * for >= 1.4, others songs will have non-zero UID
                     */
                    mTrack.mItemUid = 0;
                    mRemoteNowPlayingList.addTrack(mTrack);
                    mRemoteNowPlayingList.setCurrTrack(mTrack);
                    if(mAvrcpRemoteDevice.isCoverArtSupported()) {
                        mAvrcpRemoteDevice.mBipL2capPsm = msg.arg2;
                        Log.d(TAG," psm " + mAvrcpRemoteDevice.mBipL2capPsm);
                        /*
                         * We schedule BIP connect here, to get coverart Handle of current
                         * track in case App launched later connect at later point of time.
                         */
                         mAvrcpRemoteDevice.connectBip(mAvrcpRemoteDevice.mBipL2capPsm);
                    }
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                mAvrcpRemoteDevice.mAbsVolNotificationState = 
                                         AvrcpControllerConstants.DEFER_VOLUME_CHANGE_RSP;
                setAbsVolume(msg.arg1, msg.arg2);
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                /* start BroadcastReceiver now */
                IntentFilter filter = new IntentFilter(AudioManager.VOLUME_CHANGED_ACTION);
                mAvrcpRemoteDevice.mNotificationLabel = msg.arg1;
                mAvrcpRemoteDevice.mAbsVolNotificationState =
                        AvrcpControllerConstants.SEND_VOLUME_CHANGE_RSP;
                registerReceiver(mBroadcastReceiver, filter);
                int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int currIndex = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int percentageVol = ((currIndex* AvrcpControllerConstants.ABS_VOL_BASE)/maxVolume);
                Log.d(TAG," Sending Interim Response = "+ percentageVol + " label " + msg.arg1);
                sendRegisterAbsVolRspNative(getByteAddress(mAvrcpRemoteDevice.mBTDevice),
                        (byte)AvrcpControllerConstants.NOTIFICATION_RSP_TYPE_INTERIM, percentageVol,
                        mAvrcpRemoteDevice.mNotificationLabel);
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_TRACK_CHANGED:
                if(mRemoteNowPlayingList != null) {
                    mRemoteNowPlayingList.updateCurrentTrack((TrackInfo)msg.obj);
                    broadcastMetaDataChanged(AvrcpUtils.getMediaMetaData
                            (mRemoteNowPlayingList.getCurrentTrack()));
                    if ((!mAppProperties.isCoverArtRequested) ||
                            (!mAvrcpRemoteDevice.isCoverArtSupported()))
                        break;
                    if ((mAvrcpRemoteDevice != null) && (!mAvrcpRemoteDevice.isBipConnected())) {
                        /*
                         * If BIP not connected, try again.
                         */
                        mAvrcpRemoteDevice.connectBip(mAvrcpRemoteDevice.mBipL2capPsm);
                    }
                    else {
                        mRemoteNowPlayingList.fetchCoverArtImage(mAppProperties.
                                mSupportedCoverArtMimetype,
                                mAppProperties.mSupportedCovertArtHeight,
                                mAppProperties.mSupportedCoverArtWidth,
                                mAppProperties.mSupportedCoverArtMaxSize);
                    }
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_PLAY_POS_CHANGED:
                if(mRemoteMediaPlayers != null) {
                    mRemoteMediaPlayers.getAddressedPlayer().mPlayTime = msg.arg2;
                    broadcastPlayBackStateChanged(AvrcpUtils.mapBtPlayStatustoPlayBackState
                            (mRemoteMediaPlayers.getAddressedPlayer().mPlayStatus,
                                    mRemoteMediaPlayers.getAddressedPlayer().mPlayTime));
                }
                if(mRemoteNowPlayingList != null) {
                    mRemoteNowPlayingList.getCurrentTrack().mTrackLen = msg.arg1;
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                if(mRemoteMediaPlayers != null) {
                    mRemoteMediaPlayers.getAddressedPlayer().mPlayStatus = (byte)msg.arg1;
                    broadcastPlayBackStateChanged(AvrcpUtils.mapBtPlayStatustoPlayBackState
                            (mRemoteMediaPlayers.getAddressedPlayer().mPlayStatus, 
                                    mRemoteMediaPlayers.getAddressedPlayer().mPlayTime));
                    if(mRemoteMediaPlayers.getPlayStatus() == AvrcpControllerConstants.
                                                                        PLAY_STATUS_PLAYING) {
                        A2dpSinkService a2dpSinkService = A2dpSinkService.getA2dpSinkService();
                        if((a2dpSinkService != null)&&(!mConnectedDevices.isEmpty())) {
                            Log.d(TAG," State = PLAYING, inform A2DP SINK");
                            a2dpSinkService.informAvrcpStatePlaying(mConnectedDevices.get(0));
                        }
                    }
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_SUPPORTED_PLAYER_APP_SETTING:
                if(mRemoteMediaPlayers != null)
                    mRemoteMediaPlayers.getAddressedPlayer().
                                           setSupportedPlayerAppSetting((ByteBuffer)msg.obj);
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_PLAYER_APP_SETTING_CHANGED:
                if(mRemoteMediaPlayers != null) {
                    mRemoteMediaPlayers.getAddressedPlayer().
                                           updatePlayerAppSetting((ByteBuffer)msg.obj);
                    broadcastPlayerAppSettingChanged(getCurrentPlayerAppSetting());
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_BIP_CONNECTED:
                /*
                 * BIP Connected Start Image Fetch operation
                 */
                if (mRemoteNowPlayingList != null) {
                    int ret = mRemoteNowPlayingList.fetchCoverArtImage(mAppProperties.
                            mSupportedCoverArtMimetype,
                            mAppProperties.mSupportedCovertArtHeight,
                            mAppProperties.mSupportedCoverArtWidth,
                            mAppProperties.mSupportedCoverArtMaxSize);
                    if (ret == AvrcpControllerConstants.ERROR_BIP_HANDLE_NOT_VALID) {
                        /* Send a getElementAttrib command again */
                        getElementAttributesNative(getByteAddress(mAvrcpRemoteDevice.mBTDevice),
                                (byte)0, null);
                    }
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_BIP_DISCONNECTED:
                /*
                 * BIP Disconnected, We have to clear off imageLocation and
                 * ImageHandles.
                 */
                if (mRemoteNowPlayingList != null) {
                    mRemoteNowPlayingList.clearCoverArtData();
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_THUMB_NAIL_FETCHED:
                ArrayList<String> bipResults = (ArrayList<String>)msg.obj;
                String mImageHndl = bipResults.get(0);
                String mImageLoc = bipResults.get(1);
                if (mRemoteNowPlayingList != null) {
                    mRemoteNowPlayingList.updateThumbNail(mImageHndl, mImageLoc);
                    mRemoteNowPlayingList.fetchThumbNail();
                    if (mAppProperties.isCoverArtRequested)
                        broadcastMetaDataChanged(AvrcpUtils.getMediaMetaData
                                       (mRemoteNowPlayingList.getCurrentTrack()));
                }
                break;
            case AvrcpControllerConstants.MESSAGE_PROCESS_IMAGE_FETCHED:
                ArrayList<String> bipImageResults = (ArrayList<String>)msg.obj;
                String mImageHandle = bipImageResults.get(0);
                String mImageLocation = bipImageResults.get(1);
                if (mRemoteNowPlayingList != null) {
                    mRemoteNowPlayingList.updateImage(mImageHandle, mImageLocation);
                    if (mAppProperties.isCoverArtRequested)
                        broadcastMetaDataChanged(AvrcpUtils.getMediaMetaData
                            (mRemoteNowPlayingList.getCurrentTrack()));
                }
                break;
            }
        }
    }

    private void setAbsVolume(int absVol, int label)
    {
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currIndex = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int newIndex = (maxVolume*absVol)/AvrcpControllerConstants.ABS_VOL_BASE;
        Log.d(TAG," setAbsVolume ="+absVol + " maxVol = " + maxVolume + " cur = " + currIndex +
                                              " new = "+newIndex);
        /*
         * In some cases change in percentage is not sufficient enough to warrant
         * change in index values which are in range of 0-15. For such cases
         * no action is required
         */
        if (newIndex != currIndex) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newIndex,
                                                     AudioManager.FLAG_SHOW_UI);
        }
        sendAbsVolRspNative(getByteAddress(mAvrcpRemoteDevice.mBTDevice), absVol, label);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(AudioManager.VOLUME_CHANGED_ACTION)) {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType == AudioManager.STREAM_MUSIC) {
                    int streamValue = intent
                            .getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
                    int streamPrevValue = intent.getIntExtra(
                            AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, -1);
                    if (streamValue != -1 && streamValue != streamPrevValue) {
                        if ((mAvrcpRemoteDevice == null)
                            ||((mAvrcpRemoteDevice.mRemoteFeatures & 
                                    AvrcpControllerConstants.BTRC_FEAT_ABSOLUTE_VOLUME) == 0)
                            ||(mConnectedDevices.isEmpty()))
                            return;
                        if(mAvrcpRemoteDevice.mAbsVolNotificationState == 
                                AvrcpControllerConstants.SEND_VOLUME_CHANGE_RSP) {
                            int maxVol = mAudioManager.
                                                  getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            int currIndex = mAudioManager.
                                                  getStreamVolume(AudioManager.STREAM_MUSIC);
                            int percentageVol = ((currIndex*
                                            AvrcpControllerConstants.ABS_VOL_BASE)/maxVol);
                            Log.d(TAG," Abs Vol Notify Rsp Changed vol = "+ percentageVol);
                            sendRegisterAbsVolRspNative(getByteAddress(mAvrcpRemoteDevice.mBTDevice),
                                (byte)AvrcpControllerConstants.NOTIFICATION_RSP_TYPE_CHANGED,
                                    percentageVol, mAvrcpRemoteDevice.mNotificationLabel);
                        }
                        else if (mAvrcpRemoteDevice.mAbsVolNotificationState == 
                                AvrcpControllerConstants.DEFER_VOLUME_CHANGE_RSP) {
                            Log.d(TAG," Don't Complete Notification Rsp. ");
                            mAvrcpRemoteDevice.mAbsVolNotificationState = 
                                              AvrcpControllerConstants.SEND_VOLUME_CHANGE_RSP;
                        }
                    }
                }
            }
        }
    };

    private void handlePassthroughRsp(int id, int keyState) {
        Log.d(TAG, "passthrough response received as: key: "
                                + id + " state: " + keyState);
    }

    private void onConnectionStateChanged(boolean connected, byte[] address) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
            (Utils.getAddressStringFromByte(address));
        Log.d(TAG, "onConnectionStateChanged " + connected + " " + device+ " size "+
                    mConnectedDevices.size());
        if (device == null)
            return;
        int oldState = (mConnectedDevices.contains(device) ? BluetoothProfile.STATE_CONNECTED
                                                        : BluetoothProfile.STATE_DISCONNECTED);
        int newState = (connected ? BluetoothProfile.STATE_CONNECTED
                                  : BluetoothProfile.STATE_DISCONNECTED);

        if (connected && oldState == BluetoothProfile.STATE_DISCONNECTED) {
            /* AVRCPControllerService supports single connection */
            if(mConnectedDevices.size() > 0) {
                Log.d(TAG,"A Connection already exists, returning");
                return;
            }
            mConnectedDevices.add(device);
            Message msg =  mHandler.obtainMessage(
                    AvrcpControllerConstants.MESSAGE_PROCESS_CONNECTION_CHANGE, newState,
                        oldState, device);
            mHandler.sendMessage(msg);
        } else if (!connected && oldState == BluetoothProfile.STATE_CONNECTED) {
            mConnectedDevices.remove(device);
            Message msg =  mHandler.obtainMessage(
                    AvrcpControllerConstants.MESSAGE_PROCESS_CONNECTION_CHANGE, newState,
                        oldState, device);
            mHandler.sendMessage(msg);
        }
    }

    private void getRcFeatures(byte[] address, int features, int ca_psm) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
                (Utils.getAddressStringFromByte(address));
        Message msg = mHandler.obtainMessage(
                AvrcpControllerConstants.MESSAGE_PROCESS_RC_FEATURES, features, ca_psm, device);
        mHandler.sendMessage(msg);
    }
    private void setPlayerAppSettingRsp(byte[] address, byte accepted) {
              /* TODO do we need to do anything here */
    }
    private void handleRegisterNotificationAbsVol(byte[] address, byte label)
    {
        Log.d(TAG,"handleRegisterNotificationAbsVol ");
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
                (Utils.getAddressStringFromByte(address));
        if (!mConnectedDevices.contains(device))
            return;
        Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION, label, 0);
        mHandler.sendMessage(msg);
    }

    private void handleSetAbsVolume(byte[] address, byte absVol, byte label)
    {
        Log.d(TAG,"handleSetAbsVolume ");
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
                (Utils.getAddressStringFromByte(address));
        if (!mConnectedDevices.contains(device))
            return;
        Message msg = mHandler.obtainMessage(
                AvrcpControllerConstants.MESSAGE_PROCESS_SET_ABS_VOL_CMD, absVol, label);
        mHandler.sendMessage(msg);
    }

    private void onTrackChanged(byte[] address, byte numAttributes, int[] attributes,
                                               String[] attribVals)
    {
        Log.d(TAG,"onTrackChanged ");
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
                (Utils.getAddressStringFromByte(address));
        if (!mConnectedDevices.contains(device))
            return;
        TrackInfo mTrack = new TrackInfo(0, numAttributes, attributes, attribVals);
        Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_PROCESS_TRACK_CHANGED, numAttributes, 0, mTrack);
        mHandler.sendMessage(msg);
    }

    private void onElementAttributeUpdate(byte[] address, byte numAttributes, int[] attributes,
            String[] attribVals)
    {
        Log.d(TAG,"onTrackChanged ");
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
            (Utils.getAddressStringFromByte(address));
        if (!mConnectedDevices.contains(device))
            return;
        TrackInfo mTrack = new TrackInfo(0, numAttributes, attributes, attribVals);
        Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_PROCESS_TRACK_CHANGED, numAttributes, 0, mTrack);
        mHandler.sendMessage(msg);
     }

    private void onPlayPositionChanged(byte[] address, int songLen, int currSongPosition) {
        Log.d(TAG,"onPlayPositionChanged ");
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
                (Utils.getAddressStringFromByte(address));
        if (!mConnectedDevices.contains(device))
            return;
        Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_PROCESS_PLAY_POS_CHANGED, songLen, currSongPosition);
        mHandler.sendMessage(msg);
    }

    private void onPlayStatusChanged(byte[] address, byte playStatus) {
        if(DBG) Log.d(TAG,"onPlayStatusChanged " + playStatus);
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
                (Utils.getAddressStringFromByte(address));
        if (!mConnectedDevices.contains(device))
            return;
        Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_PROCESS_PLAY_STATUS_CHANGED, playStatus, 0);
        mHandler.sendMessage(msg);
    }

    private void handlePlayerAppSetting(byte[] address, byte[] playerAttribRsp, int rspLen) {
        Log.d(TAG,"handlePlayerAppSetting rspLen = " + rspLen);
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
                (Utils.getAddressStringFromByte(address));
        if (!mConnectedDevices.contains(device))
            return;
        ByteBuffer bb = ByteBuffer.wrap(playerAttribRsp, 0, rspLen);
        Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_PROCESS_SUPPORTED_PLAYER_APP_SETTING, 0, 0, bb);
        mHandler.sendMessage(msg);
    }

    private void onPlayerAppSettingChanged(byte[] address, byte[] playerAttribRsp, int rspLen) {
        Log.d(TAG,"onPlayerAppSettingChanged ");
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice
                (Utils.getAddressStringFromByte(address));
        if (!mConnectedDevices.contains(device))
            return;
        ByteBuffer bb = ByteBuffer.wrap(playerAttribRsp, 0, rspLen);
        Message msg = mHandler.obtainMessage(AvrcpControllerConstants.
                MESSAGE_PROCESS_PLAYER_APP_SETTING_CHANGED, 0, 0, bb);
        mHandler.sendMessage(msg);
    }

    private void handleGroupNavigationRsp(int id, int keyState) {
        Log.d(TAG, "group navigation response received as: key: "
                                + id + " state: " + keyState);
    }

    private byte[] getByteAddress(BluetoothDevice device) {
        return Utils.getBytesFromAddress(device.getAddress());
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
    }

    private native static void classInitNative();
    private native void initNative();
    private native void cleanupNative();
    private native boolean sendPassThroughCommandNative(byte[] address, int keyCode, int keyState);
    private native boolean sendGroupNavigationCommandNative(byte[] address, int keyCode,
                                                                                     int keyState);
    private native void setPlayerApplicationSettingValuesNative(byte[] address, byte numAttrib,
                                                    byte[] atttibIds, byte[]attribVal);
    /* This api is used to send response to SET_ABS_VOL_CMD */
    private native void sendAbsVolRspNative(byte[] address, int absVol, int label);
    /* This api is used to inform remote for any volume level changes */
    private native void sendRegisterAbsVolRspNative(byte[] address, byte rspType, int absVol,
                                                    int label);
    private native void getElementAttributesNative(byte[] address, byte numAttributes,
            byte[] attribIds);
}
