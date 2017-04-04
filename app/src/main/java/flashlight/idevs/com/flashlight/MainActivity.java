package flashlight.idevs.com.flashlight;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.squareup.seismic.ShakeDetector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements ShakeDetector.Listener {
    private static final String TAG = "MainActivity";

    // Remove the below line after defining your own ad unit ID.
    private static final String TOAST_TEXT = "Test ads are being shown. "
            + "To show live ads, replace the ad unit ID in res/values/strings.xml with your own ad unit ID.";

    public static final String AD_MOB_AGENT = "Flash Light House";

    private static final int SETTINGS_REQUEST_CODE = 5342;
    private static final long SHAKING_DELAY = 1000;

    private boolean mIsScreenLightOn;
    private boolean mIsFlashLightOn;
    private Camera mCamera;
    private View mRootView;
    private FloatingActionButton mFabFlash;
    private FloatingActionButton mFabScreen;
    private ImageButton mBtnKeepOnPause;
    private ImageButton mBtnShaking;
    private ImageButton mBtnAutoFlash;
    private long mLastShakingTimestamp;
    private boolean mIsFirstCreate = true;
    private boolean mStartFlashOnResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLastShakingTimestamp = System.currentTimeMillis();

        // Load an ad into the AdMob banner view.
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent(AD_MOB_AGENT)
                .addTestDevice("B2341FCC46F9A437CE957D4F9F961373")
                .build();
        adView.loadAd(adRequest);

        mRootView = findViewById(R.id.rootView);

        mFabFlash = (FloatingActionButton) findViewById(R.id.fabFlash);
        mFabScreen = (FloatingActionButton) findViewById(R.id.fabScreen);
        if (hasFlashOnDevice()) {
            mFabFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mIsFlashLightOn) {
                        MainActivityPermissionsDispatcher.stopFlashWithCheck(MainActivity.this);
                    } else {
                        MainActivityPermissionsDispatcher.startFlashWithCheck(MainActivity.this);
                    }
                }
            });
            mFabScreen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mIsScreenLightOn) {
                        mRootView.setBackgroundResource(R.color.gray800);
                        mIsScreenLightOn = false;
                        mFabScreen.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.black));
                    } else {
                        mRootView.setBackgroundResource(R.color.white);
                        mIsScreenLightOn = true;
                        mFabScreen.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.white));
                    }
                }
            });
        }

        mBtnAutoFlash = (ImageButton) findViewById(R.id.btnAutoFlash);
        mBtnAutoFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(getString(R.string.pref_auto_start_flash),
                        !PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(getString(R.string.pref_auto_start_flash), true))
                        .apply();
                initAutoFlashIcon();
            }
        });

        mBtnShaking = (ImageButton) findViewById(R.id.btnShaking);
        mBtnShaking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(getString(R.string.pref_use_device_shaking),
                        !PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(getString(R.string.pref_use_device_shaking), true))
                        .apply();
                initShakingIcon();
            }
        });

        mBtnKeepOnPause = (ImageButton) findViewById(R.id.btnKeepOnPause);
        mBtnKeepOnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(getString(R.string.pref_keep_flash_on_pause),
                        !PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(getString(R.string.pref_keep_flash_on_pause), true))
                        .apply();
                initKeepOnPauseIcon();
            }
        });

        initButtons();

        boolean autoStartFlash = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_auto_start_flash), true);
        if (autoStartFlash && !mIsFlashLightOn) {
            MainActivityPermissionsDispatcher.startFlashWithCheck(this);
        }

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ShakeDetector sd = new ShakeDetector(this);
        sd.start(sensorManager);
    }

    private void initButtons() {
        initAutoFlashIcon();

        initShakingIcon();

        initKeepOnPauseIcon();
    }

    private void initKeepOnPauseIcon() {
        Drawable keepOnPauseDrawable = mBtnKeepOnPause.getDrawable().mutate();
        if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(getString(R.string.pref_keep_flash_on_pause), true)) {
            DrawableCompat.setTint(keepOnPauseDrawable, ContextCompat.getColor(this, R.color.green400));
        } else {
            DrawableCompat.setTint(keepOnPauseDrawable, ContextCompat.getColor(this, R.color.gray800));
        }
        mBtnKeepOnPause.setImageDrawable(keepOnPauseDrawable);
    }

    private void initShakingIcon() {
        Drawable shakingDrawable = mBtnShaking.getDrawable().mutate();
        if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(getString(R.string.pref_use_device_shaking), true)) {
            DrawableCompat.setTint(shakingDrawable, ContextCompat.getColor(this, R.color.green400));
        } else {
            DrawableCompat.setTint(shakingDrawable, ContextCompat.getColor(this, R.color.gray800));
        }
        mBtnShaking.setImageDrawable(shakingDrawable);
    }

    private void initAutoFlashIcon() {
        Drawable autoFlashDrawable = mBtnAutoFlash.getDrawable().mutate();
        if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(getString(R.string.pref_auto_start_flash), true)) {
            DrawableCompat.setTint(autoFlashDrawable, ContextCompat.getColor(this, R.color.green400));
        } else {
            DrawableCompat.setTint(autoFlashDrawable, ContextCompat.getColor(this, R.color.gray800));
        }
        mBtnAutoFlash.setImageDrawable(autoFlashDrawable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean keepOnPause = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_keep_flash_on_pause), true);
        boolean autoFlash = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_auto_start_flash), true);
        if (mIsFirstCreate) {
            if (autoFlash) {
                MainActivityPermissionsDispatcher.startFlashWithCheck(this);
            }
        } else {
            if (!keepOnPause && !mIsFlashLightOn && mStartFlashOnResume) {
                MainActivityPermissionsDispatcher.startFlashWithCheck(this);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsFirstCreate = false;
        mStartFlashOnResume = mIsFlashLightOn;
        boolean keepOnPause = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_keep_flash_on_pause), true);
        if (!keepOnPause) {
            MainActivityPermissionsDispatcher.stopFlashWithCheck(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission(value = Manifest.permission.CAMERA)
    public void startFlash() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                manager.setTorchMode(manager.getCameraIdList()[0], true);
                mIsFlashLightOn = true;
                mFabFlash.setColorFilter(ContextCompat.getColor(this, R.color.white));
            } catch (CameraAccessException cae) {
                Log.e(TAG, cae.getMessage());
                cae.printStackTrace();
            }
        } else {
            try {
                mCamera = Camera.open();
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                mIsFlashLightOn = true;
                mFabFlash.setColorFilter(ContextCompat.getColor(this, R.color.white));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @NeedsPermission(value = Manifest.permission.CAMERA)
    public void stopFlash() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                manager.setTorchMode(manager.getCameraIdList()[0], false);
                mIsFlashLightOn = false;
                mFabFlash.setColorFilter(ContextCompat.getColor(this, R.color.black));
            } catch (CameraAccessException cae) {
                Log.e(TAG, cae.getMessage());
                cae.printStackTrace();
            }
        } else {
            try {
                mCamera = Camera.open();
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                mCamera.stopPreview();
                mCamera.release();
                mIsFlashLightOn = false;
                mFabFlash.setColorFilter(ContextCompat.getColor(this, R.color.black));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean hasFlashOnDevice() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), SETTINGS_REQUEST_CODE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == SETTINGS_REQUEST_CODE) {
                initButtons();
            }
        }
    }

    @Override
    public void hearShake() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_use_device_shaking), true) &&
                (System.currentTimeMillis() - mLastShakingTimestamp) > SHAKING_DELAY) {
            mLastShakingTimestamp = System.currentTimeMillis();
            if (mIsFlashLightOn) {
                MainActivityPermissionsDispatcher.stopFlashWithCheck(this);
            } else {
                MainActivityPermissionsDispatcher.startFlashWithCheck(this);
            }
        }
    }
}
