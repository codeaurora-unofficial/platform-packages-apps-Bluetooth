/*
 * Copyright (C) 2016 The Android Open Source Project
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


package com.android.bluetooth.avrcpcontroller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/*
 * Contains information about tracks that either currently playing or maintained in playlist
 * This is used as a local repository for information that will be passed on as MediaMetadata to the
 * MediaSessionServicve
 */
class TrackInfo {
    private static final String TAG = "AvrcpTrackInfo";
    private static final boolean DBG = true;

    /*
     * Default values for each of the items from JNI
     */
    private static final int TRACK_NUM_INVALID = -1;
    private static final int TOTAL_TRACKS_INVALID = -1;
    private static final int TOTAL_TRACK_TIME_INVALID = -1;
    private static final String UNPOPULATED_ATTRIBUTE = "";

    /*
     *Element Id Values for GetMetaData  from JNI
     */
    private static final int MEDIA_ATTRIBUTE_TITLE = 0x01;
    private static final int MEDIA_ATTRIBUTE_ARTIST_NAME = 0x02;
    private static final int MEDIA_ATTRIBUTE_ALBUM_NAME = 0x03;
    private static final int MEDIA_ATTRIBUTE_TRACK_NUMBER = 0x04;
    private static final int MEDIA_ATTRIBUTE_TOTAL_TRACK_NUMBER = 0x05;
    private static final int MEDIA_ATTRIBUTE_GENRE = 0x06;
    private static final int MEDIA_ATTRIBUTE_PLAYING_TIME = 0x07;
    private static final int MEDIA_ATTRIBUTE_COVER_ART_HANDLE = 0x08;


    private final String mArtistName;
    private final String mTrackTitle;
    private final String mAlbumTitle;
    private final String mGenre;
    private final long mTrackNum; // number of audio file on original recording.
    private final long mTotalTracks;// total number of tracks on original recording
    private final long mTrackLen;// full length of AudioFile.
    /* bip values are not final. Obex can disconnect in between and as per spec
       we should clear all CA related data in that case. */
    private String mCoverArtHandle;
    private String mImageLocation;
    private String mThumbNailLocation;

    public TrackInfo() {
        this(new ArrayList<Integer>(), new ArrayList<String>());
    }

    public TrackInfo(List<Integer> attrIds, List<String> attrMap) {
        Map<Integer, String> attributeMap = new HashMap<>();
        for (int i = 0; i < attrIds.size(); i++) {
            attributeMap.put(attrIds.get(i), attrMap.get(i));
        }

        String attribute;
        mTrackTitle = attributeMap.getOrDefault(MEDIA_ATTRIBUTE_TITLE, UNPOPULATED_ATTRIBUTE);

        mArtistName = attributeMap.getOrDefault(MEDIA_ATTRIBUTE_ARTIST_NAME, UNPOPULATED_ATTRIBUTE);

        mAlbumTitle = attributeMap.getOrDefault(MEDIA_ATTRIBUTE_ALBUM_NAME, UNPOPULATED_ATTRIBUTE);

        attribute = attributeMap.get(MEDIA_ATTRIBUTE_TRACK_NUMBER);
        mTrackNum = (attribute != null && !attribute.isEmpty()) ? Long.valueOf(attribute) : TRACK_NUM_INVALID;

        attribute = attributeMap.get(MEDIA_ATTRIBUTE_TOTAL_TRACK_NUMBER);
        mTotalTracks = (attribute != null && !attribute.isEmpty()) ? Long.valueOf(attribute) : TOTAL_TRACKS_INVALID;

        mGenre = attributeMap.getOrDefault(MEDIA_ATTRIBUTE_GENRE, UNPOPULATED_ATTRIBUTE);

        attribute = attributeMap.get(MEDIA_ATTRIBUTE_PLAYING_TIME);
        mTrackLen = (attribute != null && !attribute.isEmpty()) ? Long.valueOf(attribute) : TOTAL_TRACK_TIME_INVALID;

        mCoverArtHandle = attributeMap.getOrDefault(MEDIA_ATTRIBUTE_COVER_ART_HANDLE,
                UNPOPULATED_ATTRIBUTE);
        mImageLocation = UNPOPULATED_ATTRIBUTE;
        mThumbNailLocation = UNPOPULATED_ATTRIBUTE;
    }

    public boolean updateImageLocation(String mCAHandle, String mLocation) {
        if (DBG) Log.d(TAG, " updateImageLocation hndl " + mCAHandle + " location " + mLocation);
        if (!mCAHandle.equals(mCoverArtHandle) || (mLocation == null)) {
            return false;
        }
        mImageLocation = mLocation;
        return true;
    }

    public boolean updateThumbNailLocation(String mCAHandle, String mLocation) {
        if (DBG) Log.d(TAG, " mCAHandle " + mCAHandle + " location " + mLocation);
        if (!mCAHandle.equals(mCoverArtHandle) || (mLocation == null)) {
            return false;
        }
        mThumbNailLocation = mLocation;
        return true;
    }

    public String getCoverArtHandle() {
        return mCoverArtHandle;
    }

    public void clearCoverArtData() {
        mCoverArtHandle = UNPOPULATED_ATTRIBUTE;
        mImageLocation = UNPOPULATED_ATTRIBUTE;
        mThumbNailLocation = UNPOPULATED_ATTRIBUTE;
    }

    public String toString() {
        return "TrackInfo [mArtistName=" + mArtistName + ", mTrackTitle=" + mTrackTitle
               + ", mAlbumTitle=" + mAlbumTitle + ", mGenre=" + mGenre + ", mTrackNum="
               + mTrackNum + ", mTotalTracks=" + mTotalTracks + ", mTrackLen="
               + mTrackLen + ", mCoverArtHandle=" + mCoverArtHandle
               + ", mImageLocation=" + mImageLocation + ", mThumbNailLocation="
               + mThumbNailLocation + "]";
    }

    public MediaMetadata getMediaMetaData() {
        if (DBG) Log.d(TAG, " TrackInfo " + toString());
        MediaMetadata.Builder mMetaDataBuilder = new MediaMetadata.Builder();
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST,
            mArtistName);
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE,
            mTrackTitle);
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM,
            mAlbumTitle);
        mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_GENRE,
            mGenre);
        mMetaDataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER,
            mTrackNum);
        mMetaDataBuilder.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS,
            mTotalTracks);
        mMetaDataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION,
            mTrackLen);
        if (mImageLocation != UNPOPULATED_ATTRIBUTE) {
            Uri imageUri = Uri.parse(mImageLocation);
            if (DBG) Log.d(TAG," updating image uri = " + imageUri.toString());
            mMetaDataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                    imageUri.toString());
        }
        if (mThumbNailLocation != UNPOPULATED_ATTRIBUTE) {
            Bitmap mThumbNailBitmap = BitmapFactory.decodeFile(mThumbNailLocation);
            mMetaDataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, mThumbNailBitmap);
        }
        return mMetaDataBuilder.build();
    }


    public String displayMetaData() {
        MediaMetadata metaData = getMediaMetaData();
        StringBuffer sb = new StringBuffer();
        /* getDescription only contains artist, title and album */
        sb.append(metaData.getDescription().toString() + " ");
        if(metaData.containsKey(MediaMetadata.METADATA_KEY_GENRE))
            sb.append(metaData.getString(MediaMetadata.METADATA_KEY_GENRE) + " ");
        if(metaData.containsKey(MediaMetadata.METADATA_KEY_MEDIA_ID))
            sb.append(metaData.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) + " ");
        if(metaData.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
            sb.append(Long.toString(metaData.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER)) + " ");
        if(metaData.containsKey(MediaMetadata.METADATA_KEY_NUM_TRACKS))
            sb.append(Long.toString(metaData.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS)) + " ");
        if(metaData.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
            sb.append(Long.toString(metaData.getLong(MediaMetadata.METADATA_KEY_DURATION)) + " ");
        if(metaData.containsKey(MediaMetadata.METADATA_KEY_TRACK_NUMBER))
            sb.append(Long.toString(metaData.getLong(MediaMetadata.METADATA_KEY_DURATION)) + " ");
        return sb.toString();
    }
}
