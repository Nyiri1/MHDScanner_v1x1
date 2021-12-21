package aaa.mhdscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.se.omapi.Session;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.xw.repo.BubbleSeekBar;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Activity_MHDScan extends AppCompatActivity {

    BubbleSeekBar mBbubbleSeekBar;
    private int pictureCounter = 0;

    /*Variable definitions*/
    /* Layout */
    private TextureView mTextureView; //Variable vom Typ der Klasse TextureView

    /*Transparenz*/
    private TextView mBoxBottom;
    private TextView mBoxTop;
    private TextView mBoxRight;
    private TextView mBoxLeft;
    private TextView mCounter;
    private TextView mAvgSpeed;


    /*Threads*/
    private HandlerThread mBackgroundThread; // can be local but must not be
    private Handler mBackgroundHandler;

    /*Preview*/
    private Size mPreviewSize;

    /* Image capture */
    private ImageReader mImageReader;
    private boolean mImageClosed;
    private boolean mImageSaverReady;
    private boolean mIncreaseCounter;
    private boolean mFileAvailableAndCaptureStarted;
    private float mCropFactor;
    private int mRecover;

    /* Camera lock */
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mIsRecording = false;
    private int mTimeElapsed; // Accuracy: 2,000seconds = 2000milliseconds : 0,001 milliseconds
    private float mAvgSpeedVariable;

    /* Camera */
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private int mTotalRotation;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private int mCaptureState = STATE_PREVIEW;

    private CaptureRequest.Builder mCaptureRequestBuilder; // The
    private CameraCaptureSession mPreviewCaptureSession; //mPreviewSession
    private CaptureRequest mPreviewRequest;

    /*String variables*/
    private File mImageFolder;
    private String mImageFileName;

    private ToggleButton btnStartStopVideo;
    private Button btnTakePicture, btnToBarcode;

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final String TAG = "FINDTHELOG"; //for take picture

    /* Orientations */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public boolean mIsRecordingVideo;
    private int counter = 0;
    private boolean TimerExists = false;

    int mLastWidth=0;
    int mLastHeight=0;
    int mDesiredWidth=0;
    int mDesiredHeight=0;



    /*Android Lifecycle operations*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("Debug_ALL[onCreate]", "Start");
        setContentView(R.layout.layout_mhdscan);

        mTextureView = findViewById(R.id.texture);

        mBbubbleSeekBar = (BubbleSeekBar) findViewById(R.id.bubbleSeekBar);
        mBbubbleSeekBar.setProgress(2000);
        SeekbarListener();

        /*Transparenz*/
        mBoxTop = findViewById(R.id.box_Top);
        mBoxBottom = findViewById(R.id.box_Bottom);
        mBoxRight = findViewById(R.id.box_Right);
        mBoxLeft = findViewById(R.id.box_Left);

        mIsRecordingVideo = false;
        mImageClosed = true;
        mImageSaverReady = true;
        mIncreaseCounter = false;
        mFileAvailableAndCaptureStarted = false;
        mRecover = 0;
        pictureCounter = 0;
        mTimeElapsed = 0;

        mCounter = (TextView) findViewById(R.id.textView_ShowCount);
        mCounter.setText("0");
        mAvgSpeed = (TextView) findViewById(R.id.textView_avgSpeed);
        mAvgSpeed.setText("After session");

        /* OnClickListeners */
        btnTakePicture = (Button) findViewById(R.id.take_picture);
        btnTakePicture.setOnClickListener(view -> {
            lockFocus();
        });


        btnStartStopVideo = (ToggleButton) findViewById(R.id.start_stop_video);
        btnStartStopVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v("Debug_ALL[OnCheckedChanged]", "Start");
                Log.v("Debug_ALL[OnCheckedChanged]", "IsChecked?");
                if(isChecked) {
                    Log.v("Debug_ALL[OnCheckedChanged]", "Yes");
                    Log.v("Debug_ALL[OnCheckedChanged]", "changeFlagsA3()");
                    changeFlagsA3();
                    Log.v("Debug_ALL[OnCheckedChanged]", "startTimer()");
                    startTimer();
                }else{
                    Log.v("Debug_ALL[OnCheckedChanged]", "No");
                    try{
                        Log.v("Debug_ALL[OnCheckedChanged]", "changeFlagsA4()");
                        changeFlagsA4();
                        Log.v("Debug_ALL[OnCheckedChanged]", "updateAvgShootingSpeedGUI()");
                        updateAvgShootingSpeedGUI();
                        Log.v("Debug_ALL[OnCheckedChanged]", "stopTimer()");
                        stopTimer();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                Log.v("Debug_ALL[OnCheckedChanged]", "END");
            }
        });

        btnToBarcode = (Button) findViewById(R.id.button_to_barcode);
        btnToBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBarcodeScanner();
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createImageFolder2();
        }else{
            createImageFolder();
        }
    }//End onCreate(Bundle savedInstanceState)

    private void printFlags(){
        Log.v("DebuxFlags", "mIsRecordingVideo: " + mIsRecordingVideo
        + "\nmImageClodes: " + mImageClosed + "\nmImageSaverReady: " + mImageSaverReady + "\nmFileAvailableAndCaptureStarted: " + mFileAvailableAndCaptureStarted);
    }

    /* Timer section for interval still image capture */
    private Timer mTimer;
    private TimerTask timerTask;
    private Handler mTimerHandler = new Handler(Looper.getMainLooper());
    private int variableIntervall = 2000;
    private int variableIntervallFixedForSession = 2000;
    private String text = "";
    private void startTimer() {
        Log.v("Debug_ALL_startTimer()", "Start");
        Log.v("Debug_ALL_startTimer()", "changeFlagsA1()");
        changeFlagsA1();
        Log.v("Debug_ALL_startTimer()", "new Timer()");
        mTimer = new Timer();
        Log.v("Debug_ALL_startTimer()", "new TimerTask()");
        timerTask = new TimerTask() {
            public void run() {
                //mTimeToCheck = false;
                Log.v("Debug_ALL[TimerTask])", "Triggered");
                Log.v("Debug_ALL[TimerTask]", "pictureCounter>0?");
                if (pictureCounter > 0) {
                    Log.v("Debug_ALL[TimerTask]", "Yes");
                    Log.v("Debug_ALL[TimerTask]", "SumUpElapsedTime");
                    mTimeElapsed = mTimeElapsed + variableIntervallFixedForSession;
                }else{
                    Log.v("Debug_ALL[TimerTask])", "No");
                }

                Log.v("Debug_ALL[TimerTask]", "mImageClosed==false?");
                if (mImageClosed == true) {
                    Log.v("Debug_ALL[TimerTask]", "No");
                    Log.v("Debug_ALL[TimerTask]", "mRecover==1?");
                    if (mRecover == 1) {
                        Log.v("Debug_ALL[TimerTask])", "Yes");
                        Log.v("Debug_ALL[TimerTask]", "mRecover=0");
                        mRecover = 0;
                    } else {
                        Log.v("Debug_ALL[TimerTask]", "No");
                        Log.v("Debug_ALL[TimerTask]", "changeFlagsA5()");
                        changeFlagsA5();
                        Log.v("Debug_ALL[TimerTask]", "post new runnable to timerHandler");
                        mTimerHandler.post(new Runnable() {
                            public void run() {
                                Log.v("Debug_ALL[TimerTask](runnable)", "triggered");
                                Log.v("Debug_ALL[TimerTask](runnable)", "increaseCounter()");
                                increaseCounter();
                                Log.v("Debug_ALL[TimerTask](runnable)", "lockFocus()");
                                lockFocus();
                                Log.v("Debug_ALL[TimerTask](runnable)", "end");
                            }
                        });
                    }
                }else{
                    Log.v("Debug_ALL", "Yes");
                }
                Log.v("Debug_ALL", "End");
            }
        };
        mTimer.schedule(timerTask, 1, variableIntervall); //0,625 s is minimum since 0,6 s crashes
        variableIntervallFixedForSession = variableIntervall;
        Log.v("Debug_ALL_startTimer()", "End");
    }

    private void changeFlagsA1(){
        Log.v("Debug_ALL_startTimer()_changeFlagsA1", "Start");
        Log.v("Debug_ALL_startTimer()_changeFlagsA1", "TimerExists=true");
        TimerExists = true;
        Log.v("Debug_ALL_startTimer()_changeFlagsA1", "End");
    }

    private void changeFlagsA2(){
        Log.v("Debug_ALL_stopTimer()_changeFlagsA2", "Start");
        Log.v("Debug_ALL_stopTimer()", "TimerExists=false");
        TimerExists = false;
        Log.v("Debug_ALL_stopTimer()", "pictureCounter=0");
        pictureCounter = 0;
        Log.v("Debug_ALL_stopTimer()", "mTimeElapsed=0");
        mTimeElapsed = 0;
        Log.v("Debug_ALL_stopTimer()_changeFlagsA2", "End");
    }

    private void changeFlagsA3() {
        Log.v("Debug_ALL_changeFlagsA3()", "Start");
        Log.v("Debug_ALL_changeFlagsA3()", "mIsRecordingVideo = true");
        mIsRecordingVideo = true;
        Log.v("Debug_ALL_changeFlagsA3()", "End");
    }

    private void changeFlagsA4() {
        Log.v("Debug_ALL_changeFlagsA4()", "Start");
        Log.v("Debug_ALL_changeFlagsA4()", "mIsRecordingVideo = false");
        mIsRecordingVideo = false;
        Log.v("Debug_ALL_changeFlagsA4()", "End");
    }

    private void changeFlagsA5(){
        Log.v("Debug_ALL_changeFlagsA5()", "Start");
        Log.v("Debug_ALL_changeFlagsA5()", "mImageClosed = false");
        mImageClosed = false;
        Log.v("Debug_ALL_changeFlagsA5()", "End");
    }

    private void changeFlagsA6(){
        Log.v("Debug_ALL_changeFlagsA6()", "Start");
        Log.v("Debug_ALL_changeFlagsA6()", "mImageClosed = true");
        mImageClosed = true;
        Log.v("Debug_ALL_changeFlagsA6()", "End");
    }

    private void changeFlagsA8() {
        Log.v("Debug_ALL_changeFlagsA8()", "Start");
        Log.v("Debug_ALL_changeFlagsA8()", "mFileAvailableAndCaptureStarted = true");
        mFileAvailableAndCaptureStarted = true;
        Log.v("Debug_ALL_changeFlagsA8()", "End");
    }

    private void changeFlagsA9() {
        Log.v("Debug_ALL_changeFlagsA9()", "Start");
        Log.v("Debug_ALL_changeFlagsA9()", "mImageSaverReady = false");
        mImageSaverReady = false;
        Log.v("Debug_ALL_changeFlagsA9()", "mImageSaverReady = false");
        mFileAvailableAndCaptureStarted = false;
        Log.v("Debug_ALL_changeFlagsA9()", "End");
    }

    private void changeFlagsA10(){
        Log.v("Debug_ALL_changeFlagsA10()", "Start");
        Log.v("Debug_ALL_changeFlagsA10()", "mIncreaseCounter = true");
        mIncreaseCounter=true;
        Log.v("Debug_ALL_changeFlagsA10()", "mImageClosed = true");
        mImageClosed = true;
        Log.v("Debug_ALL_changeFlagsA10()", "mImageSaverReady = true");
        mImageSaverReady = true;
        Log.v("Debug_ALL_changeFlagsA10()", "End");
    }




    private void increaseCounter(){
        Log.v("Debug_ALL_increaseCounter", "Start");
        if (mIncreaseCounter == true) {
            Log.v("Debug_ALL_increaseCounter", "true");
            Log.v("Debug_ALL_increaseCounter", "mIncreaseCounter = false");
            mIncreaseCounter = false;
            Log.v("Debug_ALL_increaseCounter", "pictureCounter = pictrueCounter + 1");
            pictureCounter = pictureCounter + 1;
            Log.v("Debug_ALL_increaseCounter", "updatePictureCounterGUI");
            updatePictureCounterGUI(false);
        }else{
            Log.v("Debug_ALL_increaseCounter", "false");
        }
        Log.v("Debug_ALL_increaseCounter", "End");
    }

    private void updateAvgShootingSpeedGUI(){
        Log.v("Debug_ALL_updateAvgShootingSpeedGUI()", "Start");
        Log.v("Debug_ALL_updateAvgShootingSpeedGUI()", "PictureCounter>=5?");
        if(pictureCounter>=5) {
            Log.v("Debug_ALL_updateAvgShootingSpeedGUI()", "Yes");
            Log.v("Debug_ALL_updateAvgShootingSpeedGUI()", "SetToAvg");
            mAvgSpeedVariable = (float) mTimeElapsed / (pictureCounter - 1);
            text = String.valueOf(mAvgSpeedVariable);
            mAvgSpeed.setText(text);
        }else{
            Log.v("Debug_ALL_updateAvgShootingSpeedGUI()", "No");
            Log.v("Debug_ALL_updateAvgShootingSpeedGUI()", "SetToInfo");
            mAvgSpeed.setText(">5 pics nec.");
        }
        Log.v("Debug_ALL_updateAvgShootingSpeedGUI()", "End");
    }

    private void stopTimer(){
        Log.v("Debug_ALL_stopTimer()", "Start");
        Log.v("Debug_ALL_stopTimer()", "mTimer!=null?");
        if(mTimer != null){
            Log.v("Debug_ALL_stopTimer()", "Yes");
            Log.v("Debug_ALL_stopTimer()", "changeFlagsA2()");
            changeFlagsA2();
            Log.v("Debug_ALL_stopTimer()", "endTimerAndTask()");
            endTimerAndTask();
            Log.v("Debug_ALL_stopTimer()", "updatePictureCounterGUI()");
            updatePictureCounterGUI(true);
        }else{
            Log.v("Debug_ALL_stopTimer()", "No");
        }
        Log.v("Debug_ALL_stopTimer()", "End");
    }

    private void updatePictureCounterGUI(boolean reset){
        Log.v("Debug_ALL_updatePictureCounterGUI", "start");
        Log.v("Debug_ALL_updatePictureCounterGUI", "reset?");
        if(reset){
            Log.v("Debug_ALL_updatePictureCounterGUI", "true");
            Log.v("Debug_ALL_updatePictureCounterGUI", "SetTo0");
            text = String.valueOf(0);
            mCounter.setText(text);
        }else {
            Log.v("Debug_ALL_updatePictureCounterGUI", "false");
            Log.v("Debug_ALL_updatePictureCounterGUI", "SetToPictureCounter");
            text = String.valueOf(pictureCounter);
            mCounter.setText(text);
        }
        Log.v("Debug_ALL_updatePictureCounterGUI", "end");
    }

    private void endTimerAndTask(){
        Log.v("Debug_ALL_startstopTimer()", "endTimerAndTask()");
        mTimer.cancel();
        mTimer.purge();
        timerTask.cancel();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.v("Debug1", "onStart");
        //Do nothing
    }

    //In onResume the UI will be visible, so adding UI in onStart has no effect
    @Override
    protected void onResume() {
        super.onResume();
        Log.v("Debug_ALL_onResume()", "start");
        Log.v("Debug_ALL_onResume()", "startBackgroundThread()");
        startBackgroundThread();
        Log.v("Debug_All_onResume()", "TextureView available?");
        if (mTextureView.isAvailable()) {
            Log.v("Debug_All_onResume()", "yes");
            /*Transparenz für das Layout */
            //createTransparency()
            Log.v("Debug_All_onResume()", "initialiseLastAndActualWidthHeight()");
            initialiseLastAndActualWidthHeight();
            Log.v("Debug_All_onResume()", "openCamera(mTextureView.Width="+mTextureView.getWidth()+", mTextureView.Height="+mTextureView.getHeight()+")");
            openCamera(mTextureView.getWidth(), mTextureView.getHeight()); // Höhe und Breite vom TextureView selbe wie in Zeile 593
        } else {
            Log.v("Debug_All_onResume()", "no");
            Log.v("Debug_All_onResume()", "Setting Surface Texture Listener");
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        Log.v("Debug_All_onResume()", "Timer does not exist & mIsRecordingVideo is true?");
        if(!TimerExists & mIsRecordingVideo) {
            Log.v("Debug_All_onResume()", "yes");
            Log.v("Debug_All_onResume()", "startTimer()");
            startTimer();
        }else{
            Log.v("Debug_All_onResume()", "no");
        }
        Log.v("Debug_All_onResume()", "end");
    }

    private void initialiseLastAndActualWidthHeight() {
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "start");
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "getLastDesiredWidth()");
        mDesiredWidth = getLastDesiredWidth(this);
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "mDesiredWidth set to " + mDesiredWidth);
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "getLastDesiredHeight()");
        mDesiredHeight = getLastDesiredHeight(this);
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "mDesiredHeight set to " + mDesiredHeight);
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "getLastWidth()");
        mLastWidth = getLastWidth(this);
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "mLastWidth set to " + mLastWidth);
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "getLastHeight()");
        mLastHeight = getLastHeight(this);
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "mLastHeight set to " + mLastHeight);
        Log.v("Debug_ALL_initialiseLastAndActualWidthHeight()", "end");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.v("Debug_ALL_onPause()", "start");
        Log.v("Debug_ALL_onPause()", "releasing OpenCloseLock");
        mCameraOpenCloseLock.release();
        Log.v("Debug_ALL_onPause()", "closeCamera()");
        closeCamera();
        Log.v("Debug_ALL_onPause()", "stopBackgroundThread()");
        stopBackgroundThread();

        Log.v("Debug_ALL_onPause()", "TimerExists?");
        if(TimerExists) {
            Log.v("Debug_ALL_onPause()", "Yes");
            stopTimer();
        }else{
            Log.v("Debug_ALL_onPause()", "No");
        }
        Log.v("Debug_ALL_onPause()", "end");
    }

    private void triggerAE(){
        try{
            Log.v("Debug1", "triggerAE()");
            mCaptureState = STATE_WAITING_PRECAPTURE;
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            Log.v("Debug20", "From AE mPreviewCaptureSession.capture(PreviewCaptureCallback)");
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void lockFocus() {
        Log.v("Debug_ALL_lockFocus()", "Start");
        Log.v("Debug_ALL_lockFocus()", "mCaptureState = STATE_WAIT_LOCK");
        mCaptureState = STATE_WAIT_LOCK;
        Log.v("Debug_ALL_lockFocus()", "mCaptureRequestBuilder.set(§Start Autofocus§)");
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        try {
            Log.v("Debug_ALL_lockFocus()", "mPreviewCaptureSession.capture(§With Autofocus§)");
            int test = mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
            Log.v("Debug_ALL_lockFocus()", "mPreviewCaptureSession.capture returned: " + test);
        } catch (CameraAccessException e) {
            Log.v("Debug_ALL_lockFocus()", "CameraAccessException");
            e.printStackTrace();
        }
        Log.v("Debug_ALL_lockFocus()", "End");
    }

    private void unlockFocus() {
        Log.v("Debug_ALL_unlockFocus()", "Start");
        Log.v("Debug_ALL_unlockFocus()", "mCaptureState = STATE_PREVIEW");
        mCaptureState = STATE_PREVIEW;
        Log.v("Debug_ALL_unlockFocus()", "mCaptureRequestBuilder.set(§Cancel Autofocus§)");
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
        try {
            Log.v("Debug_ALL_unlockFocus()", "mPreviewCaptureSession.capture(§With canceled Autofocus§)");
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e){
            Log.v("Debug_ALL_unlockFocus()", "CameraAccessException");
            e.printStackTrace();
        }
        Log.v("Debug_ALL_unlockFocus()", "End");
    }





    /*SurfaceTextureListener*/
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int TextureViewWidth, int TextureViewHeight) { // Höhe und Breite vom TextureView selbe wie in Zeile 509
            //Do something
            //createTransparency();
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureAvailable()", "Start");
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureAvailable()", "initialiseLastAndActualWidthHeight()");
            initialiseLastAndActualWidthHeight();
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureAvailable()", "openCamera(TextureViewWidth="+TextureViewWidth+", TextureViewHeight="+TextureViewHeight+")");
            openCamera(TextureViewWidth, TextureViewHeight);
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureAvailable()", "End");
        }
        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int TextureViewWidth, int TextureViewHeight) {
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureSizeChanged()", "Start");
            //Do something
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureSizeChanged()", "End");
        }
        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureDestroyed()", "Start");
            //Do something
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureDestroyed()", "End");
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureDestroyed()", "Start");
            //Do something
            Log.v("Debug_ALL_TextureView.SurfaceTextureListener_onSurfaceTextureDestroyed()", "End");
        }
    };


    /* CameraDevice.StateCallback is called when CameraDevice changed its state */
    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.v("Debug_ALL_CameraDevice.StateCallback_onOpened", "Start");
            //This method is called when the camera is opened. We start camera preview here.
            Log.v("Debug_ALL_CameraDevice.StateCallback_onOpened", "mCameraOpenCloseLock.release()");
            mCameraOpenCloseLock.release();//release semaphore after camera has been opened
            Log.v("Debug_ALL_CameraDevice.StateCallback_onOpened", "Assigning cameraDevice to variable mCameraDevice");
            mCameraDevice = cameraDevice;//set the cameraDevice from input parameters as the new private Member of the class for camera
            Log.v("Debug_ALL_CameraDevice.StateCallback_onOpened", "createCameraPreviewSession()");
            createCameraPreviewSession();//Function for introducing the preview
            Log.v("Debug_ALL_CameraDevice.StateCallback_onOpened", "End");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.v("Debug_ALL_CameraDevice.StateCallback_onDisconnected", "Start");
            //This method is called when the camera disconnects e.g. by closing task
            Log.v("Debug_ALL_CameraDevice.StateCallback_onDisconnected", "mCameraOpenCloseLock.release()");
            mCameraOpenCloseLock.release();//release semaphore after camera has been closed
            Log.v("Debug_ALL_CameraDevice.StateCallback_onDisconnected", "closeCamera()");
            closeCamera();
            Log.v("Debug_ALL_CameraDevice.StateCallback_onDisconnected", "End");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.v("Debug_ALL_CameraDevice.StateCallback_onError", "Start");
            //This method is called when the camera has been shut down because of an error
            Log.v("Debug_ALL_CameraDevice.StateCallback_onError", "mCameraOpenCloseLock.release()");
            mCameraOpenCloseLock.release(); //release semaphore after camera has been shut down
            Log.v("Debug_ALL_CameraDevice.StateCallback_onError", "cameraDevice.close()");
            cameraDevice.close();//NewAPI //Close the camera from private member area aswell because the camera has been shut down
            Log.v("Debug_ALL_CameraDevice.StateCallback_onError", "mCameraDevice = null");
            mCameraDevice = null;//Set the camera from private member area to null
            Log.v("Debug_ALL_CameraDevice.StateCallback_onError", "stopBackgroundThread()");
            stopBackgroundThread();
            Log.v("Debug_ALL_CameraDevice.StateCallback_onError", "finish()");
            finish();//calls the onDestroy() method and destroys the activity
            Log.v("Debug_ALL_CameraDevice.StateCallback_onError", "End");
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.v("Debug_ALL_ImageReader.OnImageAvailableListener_onImageAvailable", "Start");
                    Log.v("Debug_ALL_ImageReader.OnImageAvailableListener_onImageAvailable", "mImagesSaverRdy & mFileAvailableAndCaptureStarted");
                    if(mImageSaverReady == true && mFileAvailableAndCaptureStarted == true){
                        Log.v("Debug_ALL_ImageReader.OnImageAvailableListener_onImageAvailable", "Yes");
                        Log.v("Debug_ALL_ImageReader.OnImageAvailableListener_onImageAvailable", "changeFlagsA9()");
                        changeFlagsA9();
                        Log.v("Debug_ALL_ImageReader.OnImageAvailableListener_onImageAvailable", "new ImageSaver(reader.aquireLatestImage");
                        mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
                    }else if(mImageSaverReady == false){
                        Log.v("Debug_ALL_ImageReader.OnImageAvailableListener_onImageAvailable", "mImageSaver not ready");
                    }else{
                        Log.v("Debug_ALL_ImageReader.OnImageAvailableListener_onImageAvailable", "File not prepared");
                    }
                    Log.v("Debug_ALL_ImageReader.OnImageAvailableListener_onImageAvailable", "End");
                }
            };

    private final Object lock = new Object();


    private class ImageSaver implements Runnable {

        private final Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            Log.v("Debug_ALL_ImageSaver_run()", "Start");
            Log.v("Debug_ALL_ImageSaver_run()", "mImage.width="+mImage.getWidth()+" mImage.height="+mImage.getHeight());
            Log.v("Debug_ALL_ImageSaver_run()", "ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer()");
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            Log.v("Debug_ALL_ImageSaver_run()", "byte[] bytes = new byte[byteBuffer.remaining()]");
            byte[] bytes = new byte[byteBuffer.remaining()]; //buffer.capacity() evt. exchange beides gut remaining, wieviele bytes von jetziger position noch übrig sind
            Log.v("Debug_ALL_ImageSaver_run()", "byteBuffer.get(bytes)");
            byteBuffer.get(bytes);

            //If Resolution of Bitmap image is changed from last time then change resolution in perm storage
            Log.v("Debug_ALL_ImageSaver_run()", "Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null)");
            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            Log.v("Debug_ALL_ImageSaver_run()", "bitmapImage.getWidth():"+bitmapImage.getWidth()+"!=mLastWidth:"+mLastWidth+"?");
            if(bitmapImage.getWidth()!= mLastWidth){
                Log.v("Debug_ALL_ImageSaver_run()", "Yes");
                Log.v("Debug_ALL_ImageSaver_run()", "setLastWidth(bitmapImage.getWidth()="+bitmapImage.getWidth()+")");
                setLastWidth(bitmapImage.getWidth());
                mLastWidth = bitmapImage.getWidth();
                Log.v("Debug_ALL_ImageSaver_run()", "mLastWidth = bitmapImage.getWidth() = " + bitmapImage.getWidth());
                Toast.makeText(Activity_MHDScan.this, "Resolution changed since last time, please reopen activity", Toast.LENGTH_LONG).show();
            }else{
                Log.v("Debug_ALL_ImageSaver_run()", "No");
                Log.v("Debug_ALL_ImageSaver_run()", "Do nothing");
            }
            Log.v("Debug_ALL_ImageSaver_run()", "bitmapImage.getHeight():"+bitmapImage.getHeight()+"!=mLastHeight:"+mLastHeight+"?");
            if(bitmapImage.getHeight() != mLastHeight){
                Log.v("Debug_ALL_ImageSaver_run()", "Yes");
                Log.v("Debug_ALL_ImageSaver_run()", "setLastHeight(bitmapImage.getHeight()="+bitmapImage.getHeight()+")");
                setLastHeight(bitmapImage.getHeight());
                mLastHeight = bitmapImage.getHeight();
                Log.v("Debug_ALL_ImageSaver_run()", "mLastHeight = bitmapImage.getHeight() = " + bitmapImage.getHeight());
                Toast.makeText(Activity_MHDScan.this, "Resolution changed since last time, please reopen activity", Toast.LENGTH_LONG).show();
            }else{
                Log.v("Debug_ALL_ImageSaver_run()", "No");
                Log.v("Debug_ALL_ImageSaver_run()", "Do nothing");
            }

            Log.v("Debug_ALL_ImageSaver_run()", "int x = (int)((double)mLastWidth*(1-(double)mDesiredWidth/(double)mLastWidth)/2)");
            int x = (int)((double)mLastWidth*(1-(double)mDesiredWidth/(double)mLastWidth)/2);
            Log.v("Debug_ALL_ImageSaver_run()", "int "+x+" = (int)((double)"+mLastWidth+"*(1-(double)"+mDesiredWidth+"/(double)"+mLastWidth+")/2)");
            Log.v("Debug_ALL_ImageSaver_run()", "int y = (int)((double)mLastHeight*(1-(double)mDesiredHeight/(double)mLastHeight)/2)");
            int y = (int)((double)mLastHeight*(1-(double)mDesiredHeight/(double)mLastHeight)/2);
            Log.v("Debug_ALL_ImageSaver_run()", "int "+y+" = (int)((double)"+mLastHeight+"*(1-(double)"+mDesiredHeight+"/(double)"+mLastHeight+")/2)");

            //createBitmap(Starting Position x, Starting Position y, Width, Height);
            Log.v("Debug_ALL_ImageSaver_run()", "Bitmap croppedBitmap = Bitmap.createBitmap(bitmapImage, x="+x+", y="+y+", mDesiredWidth="+mDesiredWidth+", mDesiredHeight="+mDesiredHeight+");");
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmapImage, x, y, mDesiredWidth, mDesiredHeight);
            //Bitmap croppedBitmap = Bitmap.createBitmap(bitmapImage, (int) ((double) bitmapImage.getWidth() * .30781f), (int) ((double) bitmapImage.getHeight() / 2 - (double) bitmapImage.getHeight() / 19), (int) ((double) bitmapImage.getWidth() * .38438f), (int) ((double) bitmapImage.getHeight() / 9.5));
            Log.v("Debug_ALL_ImageSaver_run()", "ByteArrayOutputStream stream = new ByteArrayOutputStream()");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Log.v("Debug_ALL_ImageSaver_run()", "croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)");
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            Log.v("Debug_ALL_ImageSaver_run()", "bytes = stream.toByteArray()");
            bytes = stream.toByteArray();

            Log.v("Debug_ALL_ImageSaver_run()", "stream != null?");
            if(stream != null) {
                Log.v("Debug_ALL_ImageSaver_run()", "Yes");
                try {
                    Log.v("Debug_ALL_ImageSaver_run()", "stream.close()");
                    stream.close();
                } catch (IOException e) {
                    Log.v("Debug_ALL_ImageSaver_run()", "e.printStackTrace()");
                    e.printStackTrace();
                }
            }else{
                Log.v("Debug_ALL_ImageSaver_run()", "No");
                Log.v("Debug_ALL_ImageSaver_run()", "Do nothing");
            }

            Log.v("Debug_ALL_ImageSaver_run()", "croppedBitmap.recycle()");
            croppedBitmap.recycle();
            Log.v("Debug_ALL_ImageSaver_run()", "bitmapImage.recycle()");
            bitmapImage.recycle();
            //end new

            Log.v("Debug_ALL_ImageSaver_run()", "FileOutputStream fileOutputStream = null");
            FileOutputStream fileOutputStream = null;
            try {
                Log.v("Debug_ALL_ImageSaver_run()", "fileOutputStream = new FileOutputStream(mImageFileName="+mImageFileName+")");
                fileOutputStream = new FileOutputStream(mImageFileName);
                Log.v("Debug_ALL_ImageSaver_run()", "fileOutputStream.write(bytes)");
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                Log.v("Debug_ALL_ImageSaver_run()", "e.printStackTrace()");
                e.printStackTrace();
            } finally {
                Log.v("Debug_ALL_ImageSaver_run()", "mImage.close()");
                mImage.close();
                Log.v("Debug_ALL_ImageSaver_run()", "fileOutputStream != null?");
                if (fileOutputStream != null) {
                    Log.v("Debug_ALL_ImageSaver_run()", "Yes");
                    try {
                        Log.v("Debug_ALL_ImageSaver_run()", "fileOutputStream.close()");
                        fileOutputStream.close();
                    } catch (IOException e) {
                        Log.v("Debug_ALL_ImageSaver_run()", "e.printStackTrace()");
                        e.printStackTrace();
                    }
                }else{
                    Log.v("Debug_ALL_ImageSaver_run()", "No");
                    Log.v("Debug_ALL_ImageSaver_run()", "Do nothing");
                }
            }

            Log.v("Debug_ALL_startStillCaptureRequest[mImageSaver]", "changeFlagsA10");
            changeFlagsA10();

            Log.v("Debug_ALL_startStillCaptureRequest[mImageSaver]", "End");
        }
    }


    //CONTROL_AE_STATE_INACTIVE = 0
    //CONTROL_AE_STATE_SEARCHING = 1
    //CONTROL_AE_STATE_CONVERGED = 2
    //CONTROL_AE_STATE_LOCKED = 3
    //CONTROL_AE_STATE_FLASH_REQUIRED = 4
    //CONTROL_AE_STATE_PRECAPTURE = 5

    //Lifecycle:
    // 1->onCaptureProgressed
    // 2->onCaptureStarted
    // 3->onCaptureCompleted
    // 4->onCaptureSequenceCompleted
    final CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult captureResult){
            Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "Start");
            Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "Switch (mCaptureState)");
            switch (mCaptureState){
                case STATE_PREVIEW: {
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "mCaptureState==STATE_PREVIEW");
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "Do nothing");
                    // Do nothing
                    break;
                }
                case STATE_WAIT_LOCK: {
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "mCaptureState==STATE_WAIT_LOCK");
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "mCaptureState set to STATE_PREVIEW");
                    mCaptureState = STATE_PREVIEW;
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "AF Locked?");
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "Yes");
                        Toast.makeText(getApplicationContext(), "AF Locked!", Toast.LENGTH_SHORT).show();
                        Integer aeState = captureResult.get(CaptureResult.CONTROL_AE_STATE);
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "AE_STATE_CONVERGED?");
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "aeState:"+aeState.toString()+"==CaptureResult.CONTROL_AE_STATE_CONVERGED:"+CaptureResult.CONTROL_AE_STATE_CONVERGED+"?");
                        if (aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "Yes");
                            Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "startStillCaptureRequest()");
                            startStillCaptureRequest();
                        } else {
                            Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "No");
                            Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "redoPicture()");
                            redoPicture(1);
                            //triggerAE();
                        }
                    } else {
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "No");
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "redoPicture()");
                        redoPicture(0);
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "mCaptureState==STATE_WAITING_PRECAPTURE");
                    Integer aeState = captureResult.get(CaptureResult.CONTROL_AE_STATE);
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED?");
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "aeState:"+aeState+" == null || aeState:"+aeState+" == CaptureResult.CONTROL_AE_STATE_PRECAPTURE:"+CaptureResult.CONTROL_AE_STATE_PRECAPTURE+" || aeState:"+aeState+" == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED:"+CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED+"?");
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "Yes");
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "mCaptureState set to STATE_WAITING_NON_PRECAPTURE");
                        mCaptureState = STATE_WAITING_NON_PRECAPTURE;
                        //now we can use the flash but we do not do it here so we can go into next state
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "BackToSwitch");
                        process(captureResult); //instead the flash will lighten up the picture slowly but surely and repeating requests would lead to statechange
                    } else{
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "No");
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "redoPicture()");
                        redoPicture(0);
                    }
                    break;
                }

                case STATE_WAITING_NON_PRECAPTURE: {
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "mCaptureState==STATE_WAITING_NON_PRECAPTURE");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = captureResult.get(CaptureResult.CONTROL_AE_STATE);
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE?");
                    Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process-AESTATE", aeState.toString());
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "Yes");
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "mCaptureState = STATE_PREVIEW");
                        mCaptureState = STATE_PREVIEW;
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "startStillCaptureRequest()");
                        startStillCaptureRequest();
                    } else{
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "No");
                        Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "redoPicture");
                        redoPicture(0);
                    }
                    break;
                }
            }
            Log.v("Debug_ALL_CameraCaptureSession.CaptureCallback_process", "End");
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.v("Debug20", "onCaptureCompleted");
            process(result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.v("Debug20", "onCaptureFailed");
            showToast("Please stop shaking display");
            // Reason == 0 Error in the Framework
            // Reason == 1 from User interaction e.g. abort capture
            Log.v("Debug20", "Reason: " + failure.getReason());
            redoPicture(2);
        }

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            Log.v("Debug20", "onCaptureStarted");
        }

        @Override
        public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request, Surface target, long frameNumber) {
            super.onCaptureBufferLost(session, request, target, frameNumber);
            Log.v("Debug20", "onCaptureBufferLost");
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.v("Debug20", "onCaptureProgressed");
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
            Log.v("Debug20", "onCaptureSequenceAborted");
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            Log.v("Debug20", "onCaptureSequenceCompleted");
        }
    };

    // state = 0 is fine
    // state = 1 error
    // state = 2 restart activity ( too many errora)
    private void redoPicture(int state){
        state = 1; //temporary fixvalue
        Log.v("Debug_ALL_lockFocus()[redoPicture]", "Start");
        //mNotPhotosTakenInCurrentSession == true
        Log.v("Debug_ALL_lockFocus()[redoPicture]", "switch");
        if (state == 0) {
            Log.v("Debug_ALL_lockFocus()[redoPicture]", "state == 0");
            Log.v("Debug_ALL_lockFocus()[redoPicture]", "unlockFocus()");
            unlockFocus();
            Log.v("Debug_ALL_lockFocus()[redoPicture]", "lockFocus()");
            lockFocus();
        } else if (state == 1) {
            Log.v("Debug_ALL_lockFocus()[redoPicture]", "state == 1");
            Log.v("Debug_ALL_lockFocus()[redoPicture]", "changeFlagsA6()");
            changeFlagsA6();
            Log.v("Debug_ALL_lockFocus()[redoPicture]", "unlockFocus()");
            unlockFocus();
        } else if (state == 2) {
            Log.v("Debug_ALL_lockFocus()[redoPicture]", "state == 2");
            Log.v("Debug_ALL_lockFocus()[redoPicture]", "restartActivity()");
            restartActivity();
        }
        Log.v("Debug_ALL_lockFocus()[redoPicture]", "End");
    }

    private void restartActivity(){
        Log.v("Debug_ALL_restartActivity()", "Start");
        Log.v("Debug_ALL_restartActivity()", "grabbingIntent");
        Intent intent = getIntent();
        Log.v("Debug_ALL_restartActivity()", "Finishing");
        finish();
        Log.v("Debug_ALL_restartActivity()", "StartWithIntent");
        startActivity(intent);
        Log.v("Debug_ALL_restartActivity()", "End");
    }

    private File createImageFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "IMAGE_" + timestamp + "_";
        File imageFile = File.createTempFile(prepend, ".png", mImageFolder);
        mImageFileName = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void createImageFolder() {
        Log.v("Debug2", "Trying to create Image Folder API 28");
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        Uri uri = Uri.fromFile(imageFile);
        Log.v("debug2", "URI: " + uri);

        mImageFolder = new File(imageFile, "camera2VideoImage");
        if(!mImageFolder.exists()) {
            Log.v("Debug2", "Image folder can be created");
            mImageFolder.mkdirs();
        }else{
            Log.v("Debug2", "Image folder exists already");
        }
    }

    private void createImageFolder2(){
        // Sadly not in gallery but works for 29+
        File imageFile = getExternalFilesDir(null);
        mImageFolder = new File(imageFile, "camera2VideoImage");
        if(!mImageFolder.exists()) {
            Log.v("Debug2", "Does not exist, we create now");
            if(mImageFolder.mkdirs()){
                Log.v("Debug2", "Creation successfull");
            }else{
                Log.v("Debug2", "Creation failed");
            }
        }else{
            Log.v("Debug2", "Image folder exists already");
        }
    }


    private void startStillCaptureRequest() {
        try {
            Log.v("Debug_ALL_startStillCaptureRequest", "Start");
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(mTotalRotation));

            CameraCaptureSession.CaptureCallback stillCaptureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                            Log.v("Debug_ALL_StillCaptureCallback", "onCaptureStarted");
                        }
                    };

            //Log.v("DEBUG1", "mPreviewCaptureSession.capture(stillCaptureCallback)");
            Log.v("Debug1", "Trying to capture");
            try {
                Log.v("Debug_ALL_startStillCaptureRequest", "createImageFileName()");
                createImageFileName();
            }catch (IOException e){
                e.printStackTrace();
            }
            Log.v("Debug_ALL_startStillCaptureRequest", "ChangeFlagsA8()");
            changeFlagsA8();

            Log.v("Debug_ALL_startStillCaptureRequest", "mPreviewCaptureSession(stillCaptureCallback)");
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }




    /*Handle events on a texture view*/
    /* Background thread / handler methods*/
    private void startBackgroundThread() {
        // Snackbar.make(thisAView, "Initialising Background Handler", 2000).show();
        mBackgroundThread = new HandlerThread("CameraBackgroundThread");
        mBackgroundThread.start(); //es existiert ein Thread der im Hintergrund laeuft
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        //Wir haben nun einen HandlerThread und einen Handler
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //falls noch keine Erlaubnis erfolgt ist wird diese hier nun angefordert
            Log.v("MXXX", "OpeningCamera");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission(); //Anfrage der Permission
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermissionRead();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestRecordAudioPermission();
                return;
            }
        }

        setUpCameraOutputs(width, height); //legt mVideoSize, mPreviewSize und mCameraId fest
        configureTransform(width, height); //transforms the view matching to the phone orientation?!?! influences mTextureView

        //Oeffnen der Kamera über den Kamera Manager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //OpenCloseLock Absicherung
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            //Explizites öffnen der Kamera

            Log.v("Debug 3", "Trying to open Camera Nr.: " + mCameraId);
            manager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            Log.v("Debug3", "Camera Number: " + mCameraId + " has been successfully opened");

        } catch (CameraAccessException e) {
            Log.v("Debug3", "Could not open Camera");
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    //Erlaubnis fuer die Kameranutzung anfordern
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(Activity_MHDScan.this).setMessage("R string request permission").setPositiveButton(android.R.string.ok, (dialog, which) -> ActivityCompat.requestPermissions(Activity_MHDScan.this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS)).setNegativeButton(android.R.string.cancel, (dialog, which) -> finish()).create();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
        }
    }

    //Erlaubnis für das Beschreiben vom Speicherbereich anfordern
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(Activity_MHDScan.this).setMessage("R string request permission").setPositiveButton(android.R.string.ok, (dialog, which) -> ActivityCompat.requestPermissions(Activity_MHDScan.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS)).setNegativeButton(android.R.string.cancel, (dialog, which) -> finish()).create();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        }
    }

    //Erlaubnis für das Lesen vom Speicherbereich anfordern
    private void requestStoragePermissionRead() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(Activity_MHDScan.this).setMessage("R string request permission").setPositiveButton(android.R.string.ok, (dialog, which) -> ActivityCompat.requestPermissions(Activity_MHDScan.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS)).setNegativeButton(android.R.string.cancel, (dialog, which) -> finish()).create();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        }
    }

    private void requestRecordAudioPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            new AlertDialog.Builder(Activity_MHDScan.this).setMessage("R string request permission").setPositiveButton(android.R.string.ok, (dialog, which) -> ActivityCompat.requestPermissions(Activity_MHDScan.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS)).setNegativeButton(android.R.string.cancel, (dialog, which) -> finish()).create();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrienatation + deviceOrientation + 360) % 360;
    }


    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //Iteriert durch alle verbauten Kameras
            for (String cameraId : manager.getCameraIdList()) {
                //Extrahiert die Camera Charakteristics fuer die aktuell iterierte Kamera
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                //We don't use front facing camera in this sample.



                //Extrahiert die Facing Eigenschaft der eingebauten Kameras
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                //Alle iterierten Frontkameras werden übersprungen
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue; //Befehl zum Überspringen
                }

                //Extrahiert die Output Formate der aktuell iterierten Kamera
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //Falls die StreamConfigurationMap leer ist wird die aktuell iterierte Kamera übersprungen
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                //Extrahiert von allen Output Formaten das Größte (ANNOTATE Compare Sizes By Area)
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea()
                );
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2
                );
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                Point displaySize = new Point();

                int maxPreviewWidth = 0;
                int maxPreviewHeight = 0;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                    Insets insets = windowMetrics.getWindowInsets()
                            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                    maxPreviewWidth = windowMetrics.getBounds().width();
                    maxPreviewHeight = windowMetrics.getBounds().height();
                    Log.v("Debug2", "1 Method returned Width/Height: " + maxPreviewWidth+"/"+maxPreviewHeight);

                    maxPreviewWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
                    maxPreviewHeight = windowMetrics.getBounds().height() - insets.top - insets.bottom;
                    Log.v("Debug2", "2 Method returned Width/Height: " + maxPreviewWidth+"/"+maxPreviewHeight);
                }else{
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    maxPreviewWidth = displayMetrics.widthPixels;
                    maxPreviewHeight = displayMetrics.heightPixels;
                    Log.v("Debug2", "New Method returned Width/Height: " + maxPreviewWidth+"/"+maxPreviewHeight);

                    getWindowManager().getDefaultDisplay().getSize(displaySize);
                    maxPreviewWidth = displaySize.x;
                    maxPreviewHeight = displaySize.y;
                    Log.v("Debug2", "Old Method returned Width/Height: " + maxPreviewWidth+"/"+maxPreviewHeight);


                }



                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }


                //Danger! Attemting to use too large a preview size coulld exceed camera
                //bus' bandwith limitation, resulting in gorgeous previews but the storage of
                //garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, maxPreviewWidth
                        , maxPreviewHeight, largest);

                //mPreviewSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), width, height, maxPreviewWidth, maxPreviewHeight, mVideoSize);  inserts 16:9 or 4:3 as aspect ratio and returns an available size now for video format basically 4:3 or 16:9

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //Currently on NPE is thrown when the Camera2API is used but not supported on the
            //device this code runs.
            Toast.makeText(Activity_MHDScan.this, "Camera2 API not supported on this device", Toast.LENGTH_LONG).show();
        }
    }//end set camera outputs


    /*Method to configure the neccesary Matrix transformation to mTextureView*/
    private void configureTransform(int viewWidth, int viewHeight) {
        if (mTextureView == null || mPreviewSize == null) {
            return;
        }

        Display d;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            d = this.getDisplay();
        }else {
            d = getWindowManager().getDefaultDisplay();
        }

        int rotation = d.getRotation();
        Log.v("Debug2", "Chosen rotation was: " + rotation);


        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            mTotalRotation = rotation;
        }catch(Exception e){
            e.printStackTrace();
        }

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture(); //gets SurfaceTexture of View
            assert texture != null;


            //We configure the size of the default buffer to be the size of camera preview we want.
            Log.v("Debug4", "Configure Default Buffer Size for SurfaceTexture");
            Log.v("Debug4", "width: " + mPreviewSize.getWidth() + "height: " + mPreviewSize.getHeight());
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            //We set up a CaptureRequest.Builder with the output Surface.
            mCaptureRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            int AvailableWidth = mImageReader.getWidth(); //Image Reader is landscape mode
            int AvailableHeight = mImageReader.getHeight(); // Image Reader is landscape mode
            int DesiredHeightLandscape = mDesiredWidth;
            int DesiredWidthLandscape = mDesiredHeight;
            Log.v("Debug10", "Available Width is: " + AvailableWidth + "Available Height is: " + AvailableHeight);
            Log.v("Debug10", "Cropping Window width is: " + mDesiredWidth + " Cropping Window Height is: " + mDesiredHeight);

            float actualIntakingPercentage = 0;
            float minimumWidth = 0;
            float minimumHeight = 0;

            int left = 0, top = 0, right = 0, bottom = 0;
            mCropFactor = 0;
            if (AvailableWidth > (2 * DesiredWidthLandscape) && AvailableHeight > (2 * DesiredHeightLandscape)) {
                float confusionvalue1 = DesiredWidthLandscape / (float) AvailableWidth;
                float confusionvalue2 = DesiredHeightLandscape / (float) AvailableHeight;
                Log.v("Debug10", "Confusionvalue1: " + confusionvalue1 + " Confusionvalue2: " + confusionvalue2);
                if ((DesiredWidthLandscape / (float) AvailableWidth) > (DesiredHeightLandscape / (float) AvailableHeight)) {
                    actualIntakingPercentage = DesiredWidthLandscape * 100 / (float) AvailableWidth;
                    mCropFactor = actualIntakingPercentage / 50;
                } else {
                    actualIntakingPercentage = DesiredHeightLandscape * 100 / (float) AvailableHeight;
                    mCropFactor = actualIntakingPercentage / 50;
                }
            } else {
                mCropFactor = 1;
            }
            minimumWidth = AvailableWidth * mCropFactor;
            minimumHeight = AvailableHeight * mCropFactor;

            Log.v("Debug10", "Crop Factor has been saved: " + (mCropFactor * 100000));
            setmCropFactor((int) (mCropFactor * 100000));

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            float maxzoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;

            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            Log.v("Debug9", "Rectm: lefttoprightbot: " + m.left + "," + m.top + "," + m.right + "," + m.bottom);
            //Das reale Array geht lefttoprightbot








            left = (AvailableWidth - (int) minimumWidth) / 2;
            top = (AvailableHeight - (int) minimumHeight) / 2;
            right = AvailableWidth - left;
            bottom = AvailableHeight - top;


            Log.v("Debug10", "Rectm: lefttoprightbot: " + left + "," + top + "," + right + "," + bottom);
            Rect zoom = new Rect(left, top, right, bottom);
            mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);


            //This is the output Surface we need to start preview.
            Surface previewSurface = new Surface(texture);
            mCaptureRequestBuilder.addTarget(previewSurface);

            //Here we create a CameraCaptureSession for camera preview.s
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                //int SessionType SESSSION_REGULAR non high speed operations
                // List<OutputConfiguration> outputs
                List<OutputConfiguration> configurations = new ArrayList<>();
                OutputConfiguration outputConfiguration = new OutputConfiguration(previewSurface);
                configurations.add(outputConfiguration);

                OutputConfiguration outputConfiguration2 = new OutputConfiguration(mImageReader.getSurface());
                configurations.add(outputConfiguration2);

                SessionConfiguration config = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR, configurations, getMainExecutor() , new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        //The camera is already closed
                        if (mCameraDevice == null) {
                            return;
                        }

                        //When the session is ready, we start displaying the preview.
                        mPreviewCaptureSession = cameraCaptureSession;
                        try {
                            //Auto focus should be continuous for camera preview.
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                            //Finally we start displaying the camera preview.
                            mPreviewRequest = mCaptureRequestBuilder.build();
                            mPreviewCaptureSession.setRepeatingRequest(mPreviewRequest,
                                    null, mBackgroundHandler);


                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(
                            @NonNull CameraCaptureSession cameraCaptureSession) {
                        showToast("Failed");
                    }
                });

                // new SessionConfiguration()
                // int sessionType, List <OutputConfiguration> outputs, Executor executor, StateCallback cb
                mCameraDevice.createCaptureSession(config);

            }else {
                //mCameraDevice.createCaptureSession;
                //List <Surface> outputs, StateCallback callback, Handler handler)
                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                //The camera is already closed
                                if (mCameraDevice == null) {
                                    return;
                                }

                                //When the session is ready, we start displaying the preview.
                                mPreviewCaptureSession = cameraCaptureSession;
                                try {
                                    //Auto focus should be continuous for camera preview.
                                    //mGeneralRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    //      CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                    //Finally we start displaying the camera preview.
                                    mPreviewRequest = mCaptureRequestBuilder.build();
                                    mPreviewCaptureSession.setRepeatingRequest(mPreviewRequest,
                                            null, mBackgroundHandler);

                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(
                                    @NonNull CameraCaptureSession cameraCaptureSession) {
                                showToast("Failed");
                            }
                        }, null
                );
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    private void closePreviewSession() {
        if (mPreviewCaptureSession != null) {
            mPreviewCaptureSession.close();
            mPreviewCaptureSession = null;
        }
    }




    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        //Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        //Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        //Pick the smallest of those big enough. If there is no one big enough, pick the
        //largest of those not big enough.
        if(bigEnough.size()>0){
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if(notBigEnough.size() > 0){
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("Camera 2", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /*This class compares two Sizes based on their areas.*/
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs){
            //We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }




    private void showToast(final String text) {
        runOnUiThread(new Runnable(){
            @Override public void run(){
                Toast.makeText(Activity_MHDScan.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void openBarcodeScanner(){
        //Intent intent = new Intent(this, BarcodeScannerActivity.class);
        //startActivity(intent);
    }




    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void SeekbarListener(){
        mBbubbleSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                variableIntervall = progress;
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                //tvShowInfoWarningLevel.setText("getProgressOnFinally");
            }
        });
    }

    private void setmCropFactor(int value){
        SharedPreferences prefs = this.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("CropFactor", value);
        editor.apply();
    }

    private void setLastWidth(Integer value){
        SharedPreferences prefs = this.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("LastWidth", value);
        editor.apply();
    }

    private void setLastHeight(Integer value){
        SharedPreferences prefs = this.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("LastHeight", value);
        editor.apply();
    }

    static public int getLastDesiredWidth(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastDesWidth", 100);
    }

    static public int getLastDesiredHeight(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastDesHeight", 100);
    }

    static public int getLastWidth(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastWidth", 0);
    }

    static public int getLastHeight(Context context){
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getInt("LastHeight", 0);
    }

}