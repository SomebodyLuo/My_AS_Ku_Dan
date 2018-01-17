package io.orbi.ar.fragments;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;

import java.io.IOException;

/**
 * 该实例中使用MediaPlayer完成播放，同时界面使用SurfaceView来实现
 *
 * 这里我们实现MediaPlayer中很多状态变化时的监听器
 *
 * 使用Mediaplayer时，也可以使用MediaController类，但是需要实现MediaController.mediaController接口
 * 实现一些控制方法。
 *
 * 然后，设置controller.setMediaPlayer(),setAnchorView(),setEnabled(),show()就可以了，这里不再实现
 * @author Administrator
 *
 */
public class VideoPreview implements MediaPlayer.OnCompletionListener,MediaPlayer.OnErrorListener,MediaPlayer.OnInfoListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnVideoSizeChangedListener{

    private final String TAG = "luoyouren";

    private Size mViewSize;
    private int videoWidth, videoHeight;

    private MediaPlayer player;

    // 获取视频的帧数据
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;


    ImageReader.OnImageAvailableListener mImageAvailListener;
    VideoPrepareListener mVideoPrepareListener;

    private Context mContext = null;
    public VideoPreview(Context context)
    {
        mContext = context;

        player = new MediaPlayer();
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnInfoListener(this);
        player.setOnPreparedListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnVideoSizeChangedListener(this);
    }

    public void InitMediaPlayer(Size size, ImageReader.OnImageAvailableListener listener, VideoPrepareListener videoPrepareListener)
    {
        mViewSize = size;
        mImageAvailListener = listener;
        mVideoPrepareListener = videoPrepareListener;

        //下面开始实例化MediaPlayer对象
        Log.i(TAG, "InitMediaPlayer called");
        //然后指定需要播放文件的路径，初始化MediaPlayer
        String dataPath = Environment.getExternalStorageDirectory().getPath() + "/muliujia.mp4";
        //muliujia.mp4  ar_video.mp4  roof_video  VID20180114141330.mp4  VID20180115210204.mp4  MVI_9442.MOV  DJI_0060.MOV

        Log.i(TAG, dataPath);
        try {
            player.setDataSource(dataPath);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.setLooping(true);

        //在指定了MediaPlayer播放的容器后，我们就可以使用prepare或者prepareAsync来准备播放了
        player.prepareAsync();
    }

    public Size getVideoSize()
    {
        return  new Size(videoWidth, videoHeight);
    }

    //--------------------------------
    //Sets up a new background thread
    // and it's Handler.
    //--------------------------------
    public void setupBackgroundThread()
    {

        mBackgroundThread = new HandlerThread("BackgroundCameraThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    //---------------------------------------
    // Stops the background thread and handler.
    //-----------------------------------------
    public void  teardownBackgroundThread() {

        player.stop();

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ================================ MediaPlayer的回调 =========================================
    @Override
    public void onVideoSizeChanged(MediaPlayer arg0, int arg1, int arg2) {
        // 当video大小改变时触发
        //这个方法在设置player的source后至少触发一次
        Log.i(TAG, "onVideoSizeChanged called: width = " + arg1 + "; height = " + arg2);

    }
    @Override
    public void onSeekComplete(MediaPlayer arg0) {
        // seek操作完成时触发
        Log.i(TAG, "onSeekComplete called");

    }
    @Override
    public void onPrepared(MediaPlayer player) {

        Log.i(TAG, "onPrepared called");

        // 当prepare完成后，该方法触发，在这里我们播放视频

        //1. 首先取得video的宽和高
        videoWidth = player.getVideoWidth();
        videoHeight = player.getVideoHeight();

        // 2. 返回video的宽和高
        if (null != mVideoPrepareListener)
        {
            mVideoPrepareListener.onVideoPrepare(getVideoSize());
        }

//        if(videoWidth > displayRect.width() || videoHeight > displayRect.height()){
//            //如果video的宽或者高超出了当前屏幕的大小，则要进行缩放
//            float wRatio = (float) videoWidth /(float)displayRect.width();
//            float hRatio = (float) videoHeight /(float)displayRect.height();
//
//            //选择大的一个进行缩放
//            float ratio = Math.max(wRatio, hRatio);
//
//            videoWidth = (int)Math.ceil((float) videoWidth /ratio);
//            videoHeight = (int)Math.ceil((float) videoHeight /ratio);
//
//            //设置surfaceView的布局参数
//            surfaceView.setLayoutParams(new LinearLayout.LayoutParams(videoWidth, videoHeight));
//
//
//        }
        Log.i(TAG, "videoWidth = " + videoWidth + "; videoHeight = " + videoHeight);
        Log.i(TAG, "ViewWidth = " + mViewSize.getWidth() + "; ViewHeight = " + mViewSize.getHeight());

        if (true) {
            // 3. 视频文件播放到ImageReader
            // Create an ImageReader instance that buffers two camera images so there is always room for most recent camera frame.
            mImageReader = ImageReader.newInstance(videoWidth, videoHeight, ImageFormat.YUV_420_888, 2);

            // Handle all new camera frames received on the background thread.
            // mImageAvailListener非常关键！
            mImageReader.setOnImageAvailableListener(mImageAvailListener, mBackgroundHandler);

            //在这里我们指定MediaPlayer在当前的Surface中进行播放
            player.setSurface(mImageReader.getSurface());

        } else {
            // 当SurfaceView中的Surface被创建的时候被调用
            //在这里我们指定MediaPlayer在当前的Surface中进行播放
//            player.setDisplay(holder);
        }

        //然后开始播放视频
        player.start();
        Log.i(TAG, "player.start()");

    }
    @Override
    public boolean onInfo(MediaPlayer player, int whatInfo, int extra) {
        // 当一些特定信息出现或者警告时触发
        switch(whatInfo){
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                break;
        }
        return false;
    }
    @Override
    public boolean onError(MediaPlayer player, int whatError, int extra) {
        Log.i(TAG, "onError called");
        switch (whatError) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.i(TAG, "MEDIA_ERROR_SERVER_DIED");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.i(TAG, "MEDIA_ERROR_UNKNOWN");
                break;
            default:
                break;
        }
        return false;
    }
    @Override
    public void onCompletion(MediaPlayer player) {
        // 当MediaPlayer播放完成后触发
        Log.i(TAG, "onComletion called");
    }
}
