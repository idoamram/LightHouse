package flashlight.idevs.com.flashlight;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Remove the below line after defining your own ad unit ID.
    private static final String TOAST_TEXT = "Test ads are being shown. "
            + "To show live ads, replace the ad unit ID in res/values/strings.xml with your own ad unit ID.";
    public static final String AUTO_START_PREF = "auto_start_pref";
    private boolean mIsScreenLightOn;
    private boolean mIsFlashLightOn;
    private Camera mCamera;
    private View mRootView;
    private FloatingActionButton mFabFlash;
    private FloatingActionButton mFabScreen;
    private Switch mAutoStartSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load an ad into the AdMob banner view.
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        adView.loadAd(adRequest);

        // Toasts the test ad message on the screen. Remove this after defining your own ad unit ID.
        Toast.makeText(this, TOAST_TEXT, Toast.LENGTH_LONG).show();

        mRootView = findViewById(R.id.rootView);

        mAutoStartSwitch = (Switch) findViewById(R.id.autoStartSwitch);
        mAutoStartSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(AUTO_START_PREF, b).apply();
            }
        });

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean autoStartFlash = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(AUTO_START_PREF, true);
        mAutoStartSwitch.setChecked(autoStartFlash);
        if (autoStartFlash && !mIsFlashLightOn) {
            MainActivityPermissionsDispatcher.startFlashWithCheck(this);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
