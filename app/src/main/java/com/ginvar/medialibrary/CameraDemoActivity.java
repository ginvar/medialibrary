package com.ginvar.medialibrary;

import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;

import com.ginvar.library.mediarecord.VideoRecordSession;
// import com.ginvar.library.view.CameraRecordGLSurfaceView;

/**
 * Created by ginvar on 2019/8/8.
 */

public class CameraDemoActivity extends AppCompatActivity {

    // private CameraRecordGLSurfaceView mCameraView;
    private VideoRecordSession mVideoRecordSession;

    private SurfaceView mCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_demo);

//         mCameraView = (CameraRecordGLSurfaceView) findViewById(R.id.myGLSurfaceView);
//
//         mCameraView.presetRecordingSize(480, 640);
// //        mCameraView.presetRecordingSize(720, 1280);
//
//         //Taking picture size.
//         // mCameraView.setPictureSize(2048, 2048, true); // > 4MP
//         mCameraView.setZOrderOnTop(false);
//         mCameraView.setZOrderMediaOverlay(true);

        mCameraView = (SurfaceView) findViewById(R.id.myGLSurfaceView);
        mVideoRecordSession = new VideoRecordSession(this, mCameraView);
        // mVideoRecordSession.startPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mVideoRecordSession.onResume();
    }


}
