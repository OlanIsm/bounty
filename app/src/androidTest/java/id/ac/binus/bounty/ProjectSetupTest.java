package id.ac.binus.bounty;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import id.ac.binus.bounty.activities.SplashActivity;
import id.ac.binus.bounty.utils.SessionManager;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ProjectSetupTest {
    @Test
    public void launcherDisplaysBountyAndHasInternetPermission() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        assertEquals("Bounty", context.getString(R.string.app_name));
        assertEquals(PackageManager.PERMISSION_GRANTED,
                context.checkSelfPermission(Manifest.permission.INTERNET));
        Intent launcher = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertEquals(SplashActivity.class.getName(), launcher.getComponent().getClassName());
    }
}
