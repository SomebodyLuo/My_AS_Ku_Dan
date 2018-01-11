package io.orbi.ar.fragments;

import android.graphics.Color;
import android.util.Log;

/**
 * Created by pc on 2018/1/11.
 */

public class FrameRate {


    private float	mRealTimeFrame; //实时帧率
    private float	mAverageFrame; //平均帧率
    private float	mAllFrame;
    private int		mFrameCount;
    private long	mLastTime = 0;
    String framerates;

    private void updateFrameRates()
    {

        long time = System.currentTimeMillis();
        if(mLastTime == 0){

            this.mRealTimeFrame = 0;
            this.mAverageFrame = 0;

        }else{

            this.mRealTimeFrame = 1000f /(time - mLastTime);
            this.mAllFrame += mRealTimeFrame;
            this.mFrameCount ++;
            this.mAverageFrame = mAllFrame/mFrameCount;
            if(mFrameCount > 400 ){
                mFrameCount = 0;
                mAllFrame = 0;
            }

        }
        mLastTime = time;
//        if(mFrameCount % 100 == 0){
//
//            framerates = "" + (int)(mAverageFrame*100)/100f;
//            Log.d("frame","frame = " + framerates);
//        }

        framerates = "" + (int)(mRealTimeFrame*100)/100f;
        Log.d("frame","frame = " + framerates);

        // Update Android GUI.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                info.setText(framerates);
                info.setTextColor(Color.RED);
            }
        });
    }
}
