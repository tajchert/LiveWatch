package pl.tajchert.livewatch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.ImageView;

import java.util.Random;

public class ActivityWatch extends Activity  {
    private static final String TAG = ActivityWatch.class.getSimpleName();
    private SensorManager mSensorManager;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity
    private int mAccelPrevious;

    private static final double PROBABILITY = 0.3;
    private static final int SQUARE_SIZE = 20;
    private static final int RES_SAMSUNG_GEAR_LIVE = 320;
    private ImageView backgroundView;
    private boolean mActive;

    private Bitmap.Config conf;
    private Bitmap bitmapLive;
    private Canvas canvas;
    private Paint p;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                backgroundView = (ImageView) stub.findViewById(R.id.liveBackground);
            }
        });
        createBackground();


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    private void createBackground(){
        conf = Bitmap.Config.ARGB_8888; // see other conf types
        bitmapLive = Bitmap.createBitmap(RES_SAMSUNG_GEAR_LIVE, RES_SAMSUNG_GEAR_LIVE, conf); // this creates a MUTABLE bitmap
        canvas = new Canvas(bitmapLive);
        p = new Paint();
        p.setColor(Color.GRAY);
    }

    protected void onResume() {
        super.onResume();
        mActive = true;
        if(bitmapLive == null){
            createBackground();
        }
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }
    protected void onPause() {
        super.onPause();
        mActive = false;
        mSensorManager.unregisterListener(mSensorListener);
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        //thanks to Thilo answer @ stackoverflow.com/questions/2317428/android-i-want-to-shake-it
        public void onSensorChanged(SensorEvent se) {
            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta; // perform low-cut filter
            if(bitmapLive != null && p!= null) {
                paintBackground(mAccel);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private void paintBackground(float acceleration){
        int accelerationAdd = (int) Math.abs(acceleration);
        if(accelerationAdd - mAccelPrevious > 10){
            //some wrong peaks - ignore
            return;
        }
        if(accelerationAdd < 2){
            canvas.drawColor(Color.BLACK);
            backgroundView.setImageBitmap(bitmapLive);
            return;
        }
        mAccelPrevious = accelerationAdd;
        if(canvas == null){
            createBackground();
        }
        accelerationAdd = normalizeAccelerometer(accelerationAdd);
        canvas.drawColor(Color.BLACK);
        Random randomGenerator = new Random();
        for(int y = 0 ; y < RES_SAMSUNG_GEAR_LIVE ; y =y + SQUARE_SIZE){
            for(int x = 0 ; x < RES_SAMSUNG_GEAR_LIVE ; x = x + SQUARE_SIZE){
                if(randomGenerator.nextInt(10) <= PROBABILITY * accelerationAdd) {
                    canvas.drawRect(x, y, (x + SQUARE_SIZE), (y + SQUARE_SIZE), p);
                }
            }
        }
        backgroundView.setImageBitmap(bitmapLive);
    }

    private int normalizeAccelerometer(int accelerometer){
        int newAcc;
        if(accelerometer > 20){
            return 10;
        }
        newAcc = (int) Math.round(accelerometer * 0.5);//to range 1-10
        return newAcc;
    }
}
