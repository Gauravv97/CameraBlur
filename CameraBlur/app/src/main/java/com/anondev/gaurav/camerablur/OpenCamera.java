

package com.anondev.gaurav.camerablur;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.fafaldo.fabtoolbar.widget.FABToolbarLayout;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public class OpenCamera extends AppCompatActivity implements SurfaceHolder.Callback,Camera.PictureCallback, Camera.PreviewCallback{
    private MediaPlayer _shootMP = null;
    SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private List<Size> mSupportedPreviewSizes;
    ImageButton capture;
    Camera mCamera;
    private boolean attemptToFocus = false;
    private FABToolbarLayout fabToolbarLayout;
    SharedPreferences mSharedPref;
    Boolean mFocused;
    Boolean mFlashMode;
    Boolean mBugRotate;
    float mDist;
    private Matrix matrix;
    private static final int REQ_FOR_SAVING=5;
    private int Request_for_Points=111;
    public static int Req_for_Marker=2,Req_for_Image=1;


    int total;
    Boolean safeToTakePicture;
    Boolean tmove;
    private OpenCamera mthis;
    FloatingActionButton fabToolbarButton;
    //public ImageProperties imagePropertiess[];
    SlideShowDialogFragment newFragment = SlideShowDialogFragment.newInstance();
    private FocusIndicatorView mFocusIndicaor;
    private int option;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mthis=this;
        setContentView(R.layout.camera);
        matrix= new Matrix();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        init();



    }


    private void init(){

        total=0;
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mFlashMode=false;
        tmove=true;
        capture=findViewById(R.id.Capture_Image);

        turnCameraOn();
        fabToolbarLayout=(FABToolbarLayout) findViewById(R.id.fabtoolbar);
        fabToolbarButton = (FloatingActionButton) findViewById(R.id.fabtoolbar_fab);
        fabToolbarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabToolbarLayout.show();
            }
        });
        final ImageView flashModeButton = (ImageView) findViewById(R.id.flash_button);
        flashModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mFlashMode = setFlash(!mFlashMode);
                ((ImageView)v).setColorFilter(mFlashMode ? 0xFFFFFFFF : 0x00000000);

            }
        });
        final ImageView hideButton = (ImageView) findViewById(R.id.hide_button);
        hideButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                fabToolbarLayout.hide();
            }
        });


        mFocusIndicaor=findViewById(R.id.af_indicator);
        mFocusIndicaor.showStart();
        mFocusIndicaor.setVisibility(View.GONE);


    }
    public void turnCameraOn() {

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.setVisibility(SurfaceView.VISIBLE);
        final GestureDetector gestureDetector=new GestureDetector(OpenCamera.this,new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }
        });
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Camera.Parameters params = mCamera.getParameters();
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                        mDist = event.getY();
                    }
                if (action == MotionEvent.ACTION_UP) {

                    if (tmove) {
                        handleFocus(event, params);
                    } else tmove = true;
                    return true;
                }

                if (action == MotionEvent.ACTION_MOVE) {
                    if (mDist!=event.getY()&&params.isZoomSupported()) {
                            mCamera.cancelAutoFocus();
                            handleZoom(event, params);
                            tmove = false;
                        }
                    return true;
                }
                    // handle single touch events
                return true;
            }

        });
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPicture();
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }




    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = event.getY();
        if (newDist < mDist) {
            // zoom in
            if (zoom < maxZoom)
                zoom+=2;
        } else if (newDist > mDist) {
            // zoom out
            if (zoom > 0)
                zoom-=2;
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
        attemptToFocus=false;
        safeToTakePicture=true;

    }

    public void handleFocus(MotionEvent event, Camera.Parameters parameters) {
        attemptToFocus=true;
        safeToTakePicture = false;

        mCamera.cancelAutoFocus();
        try {
            mFocusIndicaor.animate().cancel();
            mFocusIndicaor.animate().setListener(null);
        }catch (Exception e){

        }

        mFocusIndicaor.clearAnimation();
        mFocusIndicaor.setVisibility(View.VISIBLE);
        mFocusIndicaor.setAlpha(1);


        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(((int)event.getX() - 25),((int) event.getY() - 25), 0, 0);
        mFocusIndicaor.setTranslationX(event.getX() - 25);
        mFocusIndicaor.setTranslationY(event.getY()-25);
        mFocusIndicaor.animate().alpha(0.0f).setDuration(5000).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mFocusIndicaor.setVisibility(View.GONE);
            }
        });
        Rect focusRect = calculateTapArea(mSurfaceView,event.getX(), event.getY(), 1f);
        Rect meteringRect = calculateTapArea(mSurfaceView,event.getX(), event.getY(), 1.5f);
       // Toast.makeText(OpenCamera.this,event.getX()+" "+event.getY()+","+mSurfaceView.getWidth()+" "+mSurfaceView.getHeight(),Toast.LENGTH_SHORT).show();


        // check if parameters are set (handle RuntimeException: getParameters failed (empty parameters))
        if (parameters != null) {
            if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO))
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            List<Camera.Area> areas=new ArrayList<>();
            areas.add(new Camera.Area(focusRect, 1000));
            parameters.setFocusAreas(areas);

            if (parameters.getMaxNumMeteringAreas()>0) {
                areas=new ArrayList<>();
                areas.add(new Camera.Area(meteringRect, 1000));
                parameters.setMeteringAreas(areas);
            }

            try {
                mCamera.setParameters(parameters);

            } catch (Exception e) {

            }
        }
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {
                attemptToFocus=false;
                safeToTakePicture=true;
            }
        });
        mCamera.setAutoFocusMoveCallback(new Camera.AutoFocusMoveCallback() {
            @Override
            public void onAutoFocusMoving(boolean b, Camera camera) {

            }
        });

    }
    public  Rect calculateTapArea(View v, float x1, float y1, float coefficient) {
        float x =mSurfaceView.getWidth()-(mSurfaceView.getHeight()-y1)*mSurfaceView.getWidth()/mSurfaceView.getHeight() ;
        float y =mSurfaceView.getHeight()- x1* mSurfaceView.getHeight()/ mSurfaceView.getWidth();

        float focusAreaSize = 50;

        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / v.getWidth() * 2000 - 1000);
        int centerY = (int) (y / v.getHeight() * 2000 - 1000);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);


        return new Rect(left, top, right, bottom);
    }
    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();

    }

    public List<Size> getPictureResolutionList() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }
    public Size getMaxPreviewResolution() {
        int maxWidth=0;
        Size curRes=null;

        mCamera.lock();

        for ( Size r: getResolutionList() ) {
            if (r.width>maxWidth) {
                maxWidth=r.width;
                curRes=r;
            }
        }

        return curRes;
    }
    public Size getMaxPictureResolution(float previewRatio) {
        int maxPixels=0;
        int ratioMaxPixels=0;
        Size currentMaxRes=null;
        Size ratioCurrentMaxRes=null;
        for ( Size r: getPictureResolutionList() ) {
            if(option==Req_for_Marker){
                if(r.width<=800||r.height<=800){
                float pictureRatio = (float) r.width / r.height;
                int resolutionPixels = r.width * r.height;

                if (resolutionPixels>ratioMaxPixels && pictureRatio == previewRatio) {
                    ratioMaxPixels=resolutionPixels;
                    ratioCurrentMaxRes=r;
                }

                if (resolutionPixels>maxPixels) {
                    maxPixels=resolutionPixels;
                    currentMaxRes=r;
                }
                }

            }else{
            float pictureRatio = (float) r.width / r.height;
            int resolutionPixels = r.width * r.height;

            if (resolutionPixels>ratioMaxPixels && pictureRatio == previewRatio) {
                ratioMaxPixels=resolutionPixels;
                ratioCurrentMaxRes=r;
            }

            if (resolutionPixels>maxPixels) {
                maxPixels=resolutionPixels;
                currentMaxRes=r;
            }}
        }

        boolean matchAspect = mSharedPref.getBoolean("match_aspect", true);

        if (ratioCurrentMaxRes!=null && matchAspect) {
            return ratioCurrentMaxRes;
        }

        return currentMaxRes;
    }
    private int findBestCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
            cameraId = i;
        }
        return cameraId;
    }



    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try {
            int cameraId = findBestCamera();
            mCamera = Camera.open(cameraId);
        }

        catch (RuntimeException e) {
            return;
        }

        Camera.Parameters param;
        param = mCamera.getParameters();

        Size pSize = getMaxPreviewResolution();
        param.setPreviewSize(pSize.width, pSize.height);

        float previewRatio = (float) pSize.width / pSize.height;

        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getRealSize(size);

        int displayWidth = Math.min(size.y, size.x);
        int displayHeight = Math.max(size.y, size.x);

        float displayRatio =  (float) displayHeight / displayWidth;

        int previewHeight = displayHeight;

        if ( displayRatio > previewRatio ) {
            ViewGroup.LayoutParams surfaceParams = mSurfaceView.getLayoutParams();
            previewHeight = (int) ( (float) size.y/displayRatio*previewRatio);
            surfaceParams.height = previewHeight;
            mSurfaceView.setLayoutParams(surfaceParams);


        }

        int hotAreaWidth = displayWidth / 4;
        int hotAreaHeight = previewHeight / 2 - hotAreaWidth;

        Size maxRes = getMaxPictureResolution(previewRatio);
        if ( maxRes != null) {
            param.setPictureSize(maxRes.width, maxRes.height);
        }

        PackageManager pm = getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        } else {
            mFocused = true;

        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            param.setFlashMode(mFlashMode ? Camera.Parameters.FLASH_MODE_ON : Camera.Parameters.FLASH_MODE_OFF);
        }
        mSupportedPreviewSizes=getResolutionList();
        param.setRotation(90);
        param.setJpegQuality(75);
        mCamera.setParameters(param);

        mBugRotate = mSharedPref.getBoolean("bug_rotate", false);

        if (mBugRotate) {
            mCamera.setDisplayOrientation(270);
        } else {
            mCamera.setDisplayOrientation(90);
        }
        try {
            mCamera.setAutoFocusMoveCallback(new Camera.AutoFocusMoveCallback() {
                @Override
                public void onAutoFocusMoving(boolean start, Camera camera) {
                    mFocused = !start;

                }
            });
        } catch (Exception e) {
        }

        // some devices doesn't call the AutoFocusMoveCallback - fake the
        // focus to true at the start

        mFocused = true;
        safeToTakePicture = true;
    }



    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        refreshCamera();
        Matrix matrix = new Matrix();

        matrix.postScale(width / 2000f, height / 2000f);
        matrix.postTranslate(width / 2f, height / 2f);
        matrix.invert(this.matrix);
    }
    private void refreshCamera() {

        try {

            mCamera.stopPreview();
        }

        catch (Exception e) {
        }

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            mCamera.setPreviewCallbackWithBuffer(this);
        }
        catch (Exception e) {
        }
        safeToTakePicture=true;
        attemptToFocus=false;
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }

    }
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    public boolean requestPicture() {

        if (safeToTakePicture&&!(newFragment!=null &&  newFragment.getDialog()!=null
                && newFragment.getDialog().isShowing())) {
            if(!safeToTakePicture)
                return true;
            safeToTakePicture = false;
            mCamera.cancelAutoFocus();
            mCamera.takePicture(null,null,mthis);
            return true;
        }
        return false;
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        shootSound();
        String path;
        Size pictureSize = camera.getParameters().getPictureSize();
        path=Save();

        FileOutputStream fos = null;
        File file=new File(path);
        try {

            fos = new FileOutputStream(file);
            // Writes bytes from the specified byte array to this file output stream
            try {
                fos.write(bytes);
            }catch (IOException e){}
        }
        catch (FileNotFoundException e) {}
        bytes=null;
        int width=pictureSize.width,height=pictureSize.height;
        /*Intent intent=new Intent();
        intent.putExtra("path",path);
        intent.putExtra("height",height);
        intent.putExtra("width",width);
        setResult(RESULT_OK,intent);
        finish();*/
        String paths[]=new String[1];
        paths[0]=path;
        StartFragment(paths,1,1);
        refreshCamera();
        safeToTakePicture = true;
    }
    public String Save(){
        try {File image = File.createTempFile(
                "Orig",  /* prefix */
                ".jpg",         /* suffix */
                getExternalFilesDir("Pictures")      /* directory */
        );
            return image.getAbsolutePath();
        } catch (IOException ex) {
            // Error occurred while creating the File
        }
        return null;
    }


    private void shootSound()
    {
        AudioManager meng = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if (volume != 0)
        {
            if (_shootMP == null) {
                _shootMP = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            }
            if (_shootMP != null) {
                _shootMP.start();
            }
        }
    }

    public void results_from_camera(String path)
    {newFragment.onStop();
        Intent intent=new Intent();
        intent.putExtra("path",path);
        setResult(RESULT_OK,intent);
        finish();

    }
    public boolean setFlash(boolean stateFlash) {
        PackageManager pm = getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Camera.Parameters par = mCamera.getParameters();
            if(par.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_ON))
            {
                par.setFlashMode(stateFlash ? Camera.Parameters.FLASH_MODE_ON : Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(par);
            }else {
                par.setFlashMode(stateFlash ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(par);
            }
            return stateFlash;
        }
        return false;
    }

    public void StartFragment(String[] path,int position,int total){
        FragmentTransaction ft=getSupportFragmentManager().beginTransaction();
        Bundle bundle=new Bundle();
        bundle.putStringArray("path",path);
        bundle.putInt("position",position);
        bundle.putInt("total",total);
        newFragment.setArguments(bundle);
        newFragment.show(ft, "slideshow");
    }
    public void tost(String x){
        Toast.makeText(this,x,Toast.LENGTH_SHORT).show();
    }

}
