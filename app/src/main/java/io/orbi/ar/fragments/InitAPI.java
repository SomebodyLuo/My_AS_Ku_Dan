package io.orbi.ar.fragments;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;

import io.orbi.ar.fragments.NativeTracker;

/**
 * Created by pc on 2018/1/11.
 */

public class InitAPI {

    private Size mPreviewSize;
    private Context mContext = null;

    public InitAPI(Context context, Size previewSize)
    {
        mPreviewSize = previewSize;
        mContext = context;
    }

    public void Initialise()
    {
        // Get the KudanCV API key from the Android Manifest.
        String apiKey = getAPIKey();

        // Initialise the native tracking objects.
        NativeTracker.initialiseImageTracker(apiKey, mPreviewSize.getWidth(), mPreviewSize.getHeight());
        NativeTracker.initialiseArbiTracker(apiKey, mPreviewSize.getWidth(), mPreviewSize.getHeight());

    }

    //-------------------------
    //API Key
    //-------------------------
    private String getAPIKey()
    {

        String appPackageName = mContext.getPackageName();

        try
        {
            ApplicationInfo app = mContext.getPackageManager().getApplicationInfo(appPackageName, PackageManager.GET_META_DATA);

            Bundle bundle = app.metaData;

            String apiKeyID = "eu.kudan.ar.API_KEY";

            if (bundle == null) {
                throw new RuntimeException("No manifest meta-data tags exist.\n\nMake sure the AndroidManifest.xml file contains a <meta-data\n\tandroid:name=\"" + apiKeyID + "\"\n\tandroid:value=\"${YOUR_API_KEY}\"></meta-data>\n");
            }

            String apiKey = bundle.getString(apiKeyID);

            if (apiKey == null) {
                throw new RuntimeException("Could not get API Key from Android Manifest meta-data.\n\nMake sure the AndroidManifest.xml file contains a <meta-data\n\tandroid:name=\"" + apiKeyID + "\"\n\tandroid:value=\"${YOUR_API_KEY}\"></meta-data>\n");
            }

            if (apiKey.isEmpty()) {
                throw new RuntimeException("Your API Key from Android Manifest meta-data appears to be empty.\n\nMake sure the AndroidManifest.xml file contains a <meta-data\n\tandroid:name=\"" + apiKeyID + "\"\n\tandroid:value=\"${YOUR_API_KEY}\"></meta-data>\n");
            }

            return apiKey;

        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Cannot find Package with name \"" + appPackageName + "\". Cannot load API key.");
        }
    }

}
