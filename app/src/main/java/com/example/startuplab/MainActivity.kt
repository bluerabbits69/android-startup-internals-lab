package com.example.startuplab

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logLifecycle("Activity", "onCreate", this)
        setContentView(TextView(this).apply { text = "StartupLab" })
    }

    override fun onStart() {
        super.onStart()
        logLifecycle("Activity", "onStart", this)
    }

    override fun onResume() {
        super.onResume()
        logLifecycle("Activity", "onResume", this)
    }

    override fun onPause() {
        super.onPause()
        logLifecycle("Activity", "onPause", this)
    }

    override fun onStop() {
        super.onStop()
        logLifecycle("Activity", "onStop", this)
    }

    override fun onRestart() {
        super.onRestart()
        logLifecycle("Activity", "onRestart", this)
    }

    override fun onDestroy() {
        super.onDestroy()
        logLifecycle("Activity", "onDestroy", this)
    }
}
