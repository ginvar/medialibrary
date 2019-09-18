package com.ginvar.medialibrary;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        // TextView tv = (TextView) findViewById(R.id.sample_text);
        // tv.setText(stringFromJNI());


        LinearLayout mLayout = (LinearLayout) findViewById(R.id.buttonLayout);

        for (DemoClassDescription demo : mDemos) {
            DemoButton btn = new DemoButton(this);
            btn.setDemo(demo);
            mLayout.addView(btn);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public static class DemoClassDescription {
        String activityName;
        String title;

        DemoClassDescription(String _name, String _title) {
            activityName = _name;
            title = _title;
        }
    }

    private static final DemoClassDescription mDemos[] = new DemoClassDescription[]{
            new DemoClassDescription("CameraDemoActivity", "Camera Filter Demo"),
    };

    public class DemoButton extends Button implements View.OnClickListener {
        private DemoClassDescription mDemo;

        public void setDemo(DemoClassDescription demo) {
            mDemo = demo;
            setAllCaps(false);
            setText(mDemo.title);
            setOnClickListener(this);
        }

        DemoButton(Context context) {
            super(context);
        }

        @Override
        public void onClick(final View v) {
            // Log.i(LOG_TAG, String.format("%s is clicked!", mDemo.title));
            Class cls;
            try {
                cls = Class.forName("com.ginvar.medialibrary." + mDemo.activityName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }

            try {
                if (cls != null)
                    startActivity(new Intent(MainActivity.this, cls));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
