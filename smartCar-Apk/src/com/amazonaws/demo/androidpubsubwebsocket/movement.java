package com.amazonaws.demo.androidpubsubwebsocket;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import static java.lang.Math.abs;


public class movement extends AppCompatActivity implements SensorEventListener {
    Button btnboost;
    AWSIotMqttManager myMqttManager;
    final String topic = "car";

    private static boolean gyro =true;
    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();
    SeekBar speedbar;
    SeekBar dirbar;
    WebView web_video;
    Switch Sgyro;
    Button spinLift;
    Button spinRight;
    //final VideoView videoView;


    //gyro fields:
    // System sensor manager instance.
    private SensorManager mSensorManager;
    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;
    // Current data from accelerometer & magnetometer.  The arrays hold values
    // for X, Y, and Z.
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    // System display. Need this for determining rotation.
    private Display mDisplay;
    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;
    float pitch = (float) 45.0;
    float roll = (float) 45.0;
    float first_pitch = (float) 45.0;
    float first_roll = (float) 45.0;
    float limit1 = (float) 45.0;
    float limit2 = (float) 45.0;
    boolean first = true;
    float lastpitch = (float)45.0;
    float lastroll =(float)45.0;
    float [] last_meas = {45, 45};
    float [] curr_meas = {45, 45};

    long curr_time = System.currentTimeMillis() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable Javascript


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movement);

         Sgyro = (Switch) findViewById(R.id.gyro);

        btnboost = (Button) findViewById(R.id.btnBoost);
        btnboost.setOnClickListener(publishClick);

        spinLift = (Button) findViewById(R.id.spinl);
        spinLift.setOnClickListener(spinItLeft);
        spinRight = (Button) findViewById(R.id.spinR);
        spinRight.setOnClickListener(spinItRight);


        // web view set up
        web_video = (WebView) findViewById(R.id.video);

        web_video.getSettings().setJavaScriptEnabled(true);

        web_video.setWebViewClient(new InsideWebViewClient());
        web_video.setWebChromeClient(new WebChromeClient(){
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        web_video.loadUrl("https://gosmartcar.net/SecCam.html");


        myMqttManager =PubSubActivity.mqttManager;

        speedbar = (SeekBar) findViewById(R.id.speedBar);
        speedbar.setMax(10);
        speedbar.setProgress(5);

        speedbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                final String topic = "car";
                final String msg = "{\"cmd\": \"Speed\" , \"val\" :"+ Integer.toString(progress) + "}";

                publishAWS(topic,msg);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                speedbar.setProgress(5);

                final String topic = "car";
                final String msg = "{\"cmd\": \"Speed\" , \"val\" :"+ Integer.toString(5) + "}";

                publishAWS(topic,msg);

            }
        });
        dirbar = (SeekBar) findViewById(R.id.dirBar);
        dirbar.setMax(10);
        dirbar.setProgress(5);


        // gyro integration starts here

        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        mSensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);

        // Get the display from the window manager (for rotation).
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();

        dirbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                final String topic = "car";
                final String msg = "{\"cmd\": \"Dir\" , \"val\" :"+ Integer.toString(progress) + "}";

                publishAWS(topic,msg);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                dirbar.setProgress(5);

                final String topic = "car";
                final String msg = "{\"cmd\": \"Dir\" , \"val\" :"+ Integer.toString(5) + "}";

                publishAWS(topic,msg);
            }
        });
    }


    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (Sgyro.isChecked()) {
            // The sensor type (as defined in the Sensor class).
            int sensorType = sensorEvent.sensor.getType();

            // The sensorEvent object is reused across calls to onSensorChanged().
            // clone() gets a copy so the data doesn't change out from under us
            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccelerometerData = sensorEvent.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagnetometerData = sensorEvent.values.clone();
                    break;
                default:
                    return;
            }
            // Compute the rotation matrix: merges and translates the data
            // from the accelerometer and magnetometer, in the device coordinate
            // system, into a matrix in the world's coordinate system.
            //
            // The second argument is an inclination matrix, which isn't
            // used in this example.
            float[] rotationMatrix = new float[9];
            boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                    null, mAccelerometerData, mMagnetometerData);

            // Remap the matrix based on current device/activity rotation.
            float[] rotationMatrixAdjusted = new float[9];
            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_0:
                    rotationMatrixAdjusted = rotationMatrix.clone();
                    break;
                case Surface.ROTATION_90:
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                            SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                            rotationMatrixAdjusted);
                    break;
                case Surface.ROTATION_180:
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                            SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                            rotationMatrixAdjusted);
                    break;
                case Surface.ROTATION_270:
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                            SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                            rotationMatrixAdjusted);
                    break;
            }

            // Get the orientation of the device (azimuth, pitch, roll) based
            // on the rotation matrix. Output units are radians.
            float orientationValues[] = new float[3];
            if (rotationOK) {
                SensorManager.getOrientation(rotationMatrixAdjusted,
                        orientationValues);
            }
            if (true == first) {
                first_pitch = (float) (orientationValues[1] * 57.295779513);
                if (first_pitch < 0) first_pitch = first_pitch * -1;
                else first_pitch = 360 - first_pitch;

                first_roll = (float) (orientationValues[2] * 57.295779513);
                limit1 = (float) first_roll + 180 + 45;
                if (limit1 < 0) limit1 = limit1 + 360;
                if (limit1 > 360) limit1 = limit1 % 360;
                limit2 = (float) first_roll + 180 - 45;
                if (limit2 < 0) limit2 = limit2 + 360;
                if (limit2 > 360) limit2 = limit2 % 360;

                if (first_roll > 90) first_roll = 180 - first_roll;
                if (first_roll < -90) first_roll = -(180 + first_roll);

                if (first_roll < 0) first_roll = first_roll * -1;
                else first_roll = 360 - first_roll;
                lastpitch = first_pitch;
                pitch = first_pitch;
                lastroll = first_roll;
                roll = first_roll;
                if ((first_roll > 0) && (first_roll < 360)) first = false;
            }
            float tmplastpitch = pitch;
            float tmplastroll = roll;

            float min, max = 0;
            if (limit1 > limit2) {
                max = limit1;
                min = limit2;
            } else {
                max = limit2;
                min = limit1;
            }


            // Pull out the individual values from the array.
            float azimuth = (float) (orientationValues[0] * 57.295779513);

            pitch = (float) (orientationValues[1] * 57.295779513);
            float orig_pitch = pitch - first_pitch;
            if (pitch < 0) pitch = pitch * -1;
            else pitch = 360 - pitch;
            pitch = pitch - first_pitch + 45;
            if (pitch < 0) pitch = pitch + 360;
            if (pitch > 360) pitch = pitch % 360;

            roll = (float) (orientationValues[2] * 57.295779513);

            float limitcurr = roll + 180;
            if (limitcurr < 0) limitcurr = limitcurr + 360;
            if (limitcurr > 360) limitcurr = limitcurr % 360;

            //if ((limitcurr < max) && (limitcurr > min))
            //    lastroll = tmplastroll;

            if (roll > 90) roll = 180 - roll;
            if (roll < -90) roll = -(180 + roll);

            if (roll < 0) roll = roll * -1;
            else roll = 360 - roll;
            float orig_roll = roll - first_roll;
            roll = roll - first_roll + 45;
            if (roll < 0) roll = roll + 360;
            if (roll > 360) roll = roll % 360;

            if (((abs(orig_pitch) < 45)) || ((abs(orig_roll) < 90) || (abs(orig_roll) > 270))) {
                String s = null;
                if ((limitcurr < max) && (limitcurr > min)){
//                    s = Float.toString((90 - roll));
                    curr_meas[0] = 90 - roll;
                }
                else if (limitcurr > max){
//                    s = "90";
                    curr_meas[0] = 90;
                }
                else {
//                    s = "0";
                    curr_meas[0] = 0 ;
                }
                if ((pitch >= 0) && (pitch <= 90)){
//                    s = s + " " + Float.toString(pitch);
                    curr_meas[1] = pitch;
                }
                else if ((pitch > 90) && (pitch < 180)){
//                    s = s + " 90";
                    curr_meas[1] = 90;
                }
                else{
//                    s = s + " 0";
                    curr_meas[1] = 0;

                }
                //s = /*Float.toString((90 - roll)) +*/ "45 " +Float.toString(pitch);
                long last_time = curr_time;
                long curr_time = System.currentTimeMillis();
                if ((curr_time - last_time > 100) && ((abs(pitch - lastpitch) > 4.0) || (abs(roll - lastroll) > 6.0))) {
                    float[] meas_diff = {curr_meas[0] - last_meas[0],curr_meas[1] - last_meas[1]};
                    if (abs(meas_diff[0]) > 10 || abs(meas_diff[1]) > 10){
                        if (abs(meas_diff[0]) > 10) {
                            s = Float.toString(last_meas[0] + Math.signum(meas_diff[0])*5);
                            last_meas[0] = last_meas[0] + Math.signum(meas_diff[0])*5;
                        } else {

                            s = Float.toString(last_meas[0]);
                        }
                        if (abs(meas_diff[1]) > 10) {
                            s = s + " " + Float.toString(last_meas[1] + Math.signum(meas_diff[1])*5);
                            last_meas[1] = last_meas[1] + Math.signum(meas_diff[1])*5;
                        } else {
                            s = s + " " + Float.toString(last_meas[1]);
                        }
                        publishAWS(topic, "{\"cmd\": \"MovePiCam\" , \"val\" :" + "\"" + s + "\"" + "}");
                        lastpitch = tmplastpitch;
                        lastroll = tmplastroll;
                    }
                }
            }

            // Pitch and roll values that are close to but not 0 cause the
            // animation to flash a lot. Adjust pitch and roll to 0 for very
            // small values (as defined by VALUE_DRIFT).
            if (abs(pitch) < VALUE_DRIFT) {
                pitch = 0;
            }
            if (abs(roll) < VALUE_DRIFT) {
                roll = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class InsideWebViewClient extends WebViewClient {
        @Override
        // Force links to be opened inside WebView and not in Default Browser
        // Thanks http://stackoverflow.com/a/33681975/1815624
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed(); // Ignore SSL certificate errors
        }


    }

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            final String msg = "{\"cmd\": \"Speed\" , \"val\" :"+ Integer.toString(10) + "}";

            publishAWS(topic,msg);

        }
    };
    View.OnClickListener spinItLeft = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            final String msg = "{\"cmd\": \"Speed\" , \"val\" :"+ Integer.toString(10) + "}";

            publishAWS(topic,msg);

            final String msg2 = "{\"cmd\": \"Dir\" , \"val\" :"+ Integer.toString(0) + "}";

            publishAWS(topic,msg2);

        }
    };
    View.OnClickListener spinItRight = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            final String msg = "{\"cmd\": \"Speed\" , \"val\" :"+ Integer.toString(10) + "}";

            publishAWS(topic,msg);

            final String msg2 = "{\"cmd\": \"Dir\" , \"val\" :"+ Integer.toString(10) + "}";

            publishAWS(topic,msg2);

        }
    };

    void publishAWS(String topic, String msg){
        try {
            myMqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }
}
