package io.orbi.ar.fragments;

import android.graphics.Color;
import android.util.Log;

/**
 * Created by pc on 2018/1/11.
 */

public class FrameRate {


    private static float	mRealTimeFrame; //实时帧率
    private static float	mAverageFrame; //平均帧率
    private static float	mAllFrame;
    private static int		mFrameCount;
    private static long	mLastTime = 0;
    private static String framerates;

    public static String updateFrameRates()
    {

        long time = System.currentTimeMillis();
        if(mLastTime == 0){

            mRealTimeFrame = 0;
            mAverageFrame = 0;

        }else{

            mRealTimeFrame = 1000f /(time - mLastTime);
            mAllFrame += mRealTimeFrame;
            mFrameCount ++;
            mAverageFrame = mAllFrame/mFrameCount;
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

        return framerates;
    }
}
