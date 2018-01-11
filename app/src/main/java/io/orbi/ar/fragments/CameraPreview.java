package io.orbi.ar.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.util.Size;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;

/**
 * Created by pc on 2018/1/11.
 */

public class CameraPreview {

    private String TAG = "CameraPreview";

    // 用于渲染相机预览画面
    private CameraManager mCameraManager;//摄像头管理器
    private Handler childHandler, mainHandler;
    private String mCameraID;//摄像头Id 0 为后  1 为前

    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession mCaptureSession;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Context mContext = null;
    private Size mPreviewSize = null;
    private void CameraPreview(Context context, Size previewSize)
    {
        mContext = context;
        mPreviewSize = previewSize;
    }

    /**
     * 摄像头创建监听
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            mCameraDevice = camera;
            //开启预览
            takePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {//关闭摄像头
            if (null != mCameraDevice) {
                Log.i("CameraDevice", "CameraDevice Disconnected.");

                // Release the Semaphore to allow the CameraDevice to be closed.
                mCameraOpenCloseLock.release();

                camera.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {//发生错误
//            Toast.makeText(mContext, "摄像头开启失败", Toast.LENGTH_SHORT).show();
            Log.e("CameraDevice", "CameraDevice Error.");

            // Release the Semaphore to allow the CameraDevice to be closed.
            mCameraOpenCloseLock.release();

            camera.close();
            mCameraDevice = null;

            // Stop the activity.
            Activity activity = getActivity();

            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * Request code for camera permissions.
     */
    private static final int REQUEST_CAMERA_PERMISSIONS = 1;

    /**
     * Permissions required to take a picture.
     */
    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,

    };

    /**
     * 初始化Camera2
     */
//	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCamera2() {


        //setup camera manager
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);


        try {
            String[] cameras = manager.getCameraIdList();

            // Find back-facing camera.
            for (String camera : cameras) {

                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(camera);

                // Reject all cameras but the back-facing camera.
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) != LENS_FACING_BACK) {
                    continue;
                }

                try {
                    if (!mCameraOpenCloseLock.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException(("Camera lock cannot be acquired during opening."));
                    }

                    if (mContext.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        // Open camera. Events are sent to the mStateCallback listener and handled on the background thread.
                        manager.openCamera(camera, mStateCallback, mBackgroundHandler);
                    }


                    // Open one camera only.
                    return;

                } catch (InterruptedException e) {
                    throw new RuntimeException("Camera open/close semaphore cannot be acquired");
                }
            }

        } catch (CameraAccessException e) {
            throw new RuntimeException("Cannot access camera.");
        }
    }


//region Setup and Teardown Methods

    //--------------------------------
    //Sets up a new background thread
    // and it's Handler.
    //--------------------------------
    private void setupBackgroundThread()
    {

        mBackgroundThread = new HandlerThread("BackgroundCameraThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }







    //---------------------------------------
    // Stops the background thread and handler.
    //-----------------------------------------
    private void teardownBackgroundThread() {

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //-------------------------------------------------------
    //Tears down and closes the camera device and session.
    //------------------------------------------------------
    private void teardownCamera() {

        try {
            // Prevent the teardown from occuring at the same time as setup.
            mCameraOpenCloseLock.acquire();

            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * 开始预览
     */
    private void takePreview() {
        try
        {

            // Create an ImageReader instance that buffers two camera images so there is always room for most recent camera frame.
            // mImageReader = ImageReader.newInstance(mCameraPreviewSize.getWidth(), mCameraPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);

            // Handle all new camera frames received on the background thread.
            // mImageAvailListener非常关键！
            mImageReader.setOnImageAvailableListener(mImageAvailListener, mBackgroundHandler);

            // Set up a CaptureRequest.Builder with the output Surface of the ImageReader.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // 将mImageReader加入到Preview Request队列中
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

            // Create the camera preview CameraCaptureSession.
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
                        {
                            // The camera is already closed
                            if (mCameraDevice == null)
                            {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;

                            try
                            {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);//CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE

                                // Finally, start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);

                                // Release the Semaphore to allow the CameraDevice to be closed.
                                mCameraOpenCloseLock.release();

                            } catch (CameraAccessException e) {
                                throw new RuntimeException("Cannot access camera during CameraCaptureSession setup.");
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            throw new RuntimeException("Camera capture session configuration failed.");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            throw new RuntimeException("Cannot access camera during CameraCaptureSession setup.");
        }
    }

    /**
     * 拍照
     */
    private void takePicture() {
        if (mCameraDevice == null) return;
        // 创建拍照需要的CaptureRequest.Builder
//		final CaptureRequest.Builder captureRequestBuilder;
//		try {
//			captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//			// 将imageReader的surface作为CaptureRequest.Builder的目标
//			captureRequestBuilder.addTarget(mImageReader.getSurface());
//			// 自动对焦
//			captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//			// 自动曝光
//			captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//			// 获取手机方向
//			int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//			// 根据设备方向计算设置照片的方向
//			captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
//			//拍照
//			CaptureRequest mCaptureRequest = captureRequestBuilder.build();
//			mCameraCaptureSession.capture(mCaptureRequest, null, childHandler);
//		} catch (CameraAccessException e) {
//			e.printStackTrace();
//		}
    }

    private void initListener() {
//		mCatture.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				takePicture();
//			}
//		});
    }
}
