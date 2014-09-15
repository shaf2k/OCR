package com.shaf2kstudios.ocr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CameraPreviewActivity extends Activity {

    private SurfaceView preview;
    private Button captureButton;
    private SurfaceHolder previewHolder;
    private Camera camera=null;
    private boolean inPreview=false;
    private boolean cameraConfigured=false;
    private Camera.PictureCallback jpegPictureCallback;
    private Camera.Size size;
    protected String _path;
    protected MediaPlayer _shootMP;
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ocr/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        preview = (SurfaceView) findViewById(R.id.preview);
        preview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        previewHolder=preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        captureButton = (Button)findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AudioManager meng = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
                int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

                if (volume != 0){
                    if (_shootMP == null)
                        _shootMP = MediaPlayer.create(getBaseContext(), Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
                    if (_shootMP != null)
                        _shootMP.start();
                }
                camera.takePicture(null, null, jpegPictureCallback);
            }
        });

        jpegPictureCallback = new Camera.PictureCallback(){
            public void onPictureTaken(byte[] data, Camera arg1){
                // Save the picture.
                try {
                    _path = DATA_PATH + "/ocr.jpg";
                    File file = new File(_path);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                    Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 1000, 2000);
                    FileOutputStream out = new FileOutputStream(file);
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

                    Intent intent = getIntent();
                    setResult(-1,intent);
                    finish();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        camera=Camera.open();
        startPreview();
    }

    @Override
    public void onPause() {
        if (inPreview) {
            camera.stopPreview();
        }

        camera.release();
        camera=null;
        inPreview=false;

        super.onPause();
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }

        return(result);
    }

    private void initPreview(int width, int height) {
        if (camera!=null && previewHolder.getSurface()!=null) {
            try {
                camera.setPreviewDisplay(previewHolder);
            }
            catch (Throwable t) {
                Log.e("PreviewDemo-surfaceCallback",
                        "Exception in setPreviewDisplay()", t);
                Toast.makeText(CameraPreviewActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters=camera.getParameters();
                size=getBestPreviewSize(width, height,
                        parameters);

                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                }
                camera.setDisplayOrientation(90);
                parameters.set("jpeg-quality", 90);
                List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                size = sizes.get(0);
                for(int i=0;i<sizes.size();i++){
                    if(sizes.get(i).width > size.width)
                        size = sizes.get(i);
                }
                System.out.println("setting size to "+size.width+" x "+size.height);
                parameters.setPictureSize(size.width, size.height);

                camera.setParameters(parameters);
                cameraConfigured=true;
            }
        }
    }

    private void startPreview() {
        if (cameraConfigured && camera!=null) {
            camera.startPreview();
            inPreview=true;
        }
    }

    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width,
                                   int height) {
            initPreview(width, height);
            startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    };

    Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback(){
        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            if (arg0){
                //buttonTakePicture.setEnabled(true);
                camera.cancelAutoFocus();
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        float touchMajor = event.getTouchMajor();
        float touchMinor = event.getTouchMinor();

        Rect touchRect = new Rect(
                (int)(x - touchMajor/2),
                (int)(y - touchMinor/2),
                (int)(x + touchMajor/2),
                (int)(y + touchMinor/2));

        final Rect targetFocusRect = new Rect(
                touchRect.left * 2000/preview.getWidth() - 1000,
                touchRect.top * 2000/preview.getHeight() - 1000,
                touchRect.right * 2000/preview.getWidth() - 1000,
                touchRect.bottom * 2000/preview.getHeight() - 1000);

        final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
        Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
        focusList.add(focusArea);

        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusAreas(focusList);
        parameters.setMeteringAreas(focusList);
        camera.setParameters(parameters);

        camera.autoFocus(myAutoFocusCallback);

        return true;
    }
}
