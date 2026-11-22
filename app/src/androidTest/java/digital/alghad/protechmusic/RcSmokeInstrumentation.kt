package digital.alghad.protechmusic

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log

/**
 * Native Android platform instrumentation, with no extra downloaded testing library.
 * Non-destructive: only package metadata and launcher window are inspected.
 */
class RcSmokeInstrumentation : Instrumentation() {
    private var requestedMode: String? = null

    override fun onCreate(arguments: Bundle?) {
        requestedMode = arguments?.getString("mode")
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        super.onStart()
        if (requestedMode == "playback") {
            RcPlaybackHarness(this).run()
            return
        }
        val report = Bundle()
        var launched: Activity? = null
        try {
            val context = targetContext
            val packageName = context.packageName
            check(packageName == "digital.alghad.protechmusic") {
                "Unexpected target: $packageName"
            }

            val manager = context.packageManager
            val intent = manager.getLaunchIntentForPackage(packageName)
                ?: error("No launcher intent")
            val activityInfo = manager.getActivityInfo(
                ComponentName(packageName, "$packageName.MainActivity"), 0
            )
            val serviceInfo = manager.getServiceInfo(
                ComponentName(packageName, "$packageName.MusicService"), 0
            )
            check(activityInfo.exported) { "Launcher is not exported" }
            check(!serviceInfo.exported) { "Audio service unexpectedly exported" }

            @Suppress("DEPRECATION")
            val packageInfo = manager.getPackageInfo(
                packageName, PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions?.toList().orEmpty()
            check("android.permission.READ_MEDIA_AUDIO" in permissions)
            check("android.permission.FOREGROUND_SERVICE" in permissions)
            check("android.permission.READ_CONTACTS" !in permissions)
            check("android.permission.READ_SMS" !in permissions)

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launched = startActivitySync(intent)
            waitForIdleSync()
            SystemClock.sleep(350)
            val current = launched ?: error("Main activity not started")
            check(!current.isFinishing && !current.isDestroyed) {
                "MainActivity terminated during smoke test"
            }
            val decor = current.window.decorView
            check(decor.isAttachedToWindow && decor.width > 0 && decor.height > 0) {
                "MainActivity window not attached or rendered"
            }
            report.putString(
                "stream",
                "RC_SMOKE_PASS: Activity launch, private service, permissions, drawn UI"
            )
            report.putInt("width", decor.width)
            report.putInt("height", decor.height)
            sendStatus(0, report)
            finish(Activity.RESULT_OK, report)
        } catch (exception: Throwable) {
            Log.e("ProTechRcSmoke", "Android instrumentation failed", exception)
            report.putString("stream", "RC_SMOKE_FAIL: ${exception.stackTraceToString()}")
            sendStatus(-1, report)
            finish(Activity.RESULT_CANCELED, report)
        } finally {
            val current = launched
            current?.runOnUiThread {
                if (!current.isFinishing) current.finish()
            }
        }
    }
}
