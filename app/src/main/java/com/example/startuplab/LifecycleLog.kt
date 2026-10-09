package com.example.startuplab

import android.os.Process
import android.util.Log

private const val TAG = "StartupLab"

/** contracts/log-format.md の形式でライフサイクルのイベントを1行出力する。 */
fun logLifecycle(source: String, event: String, instance: Any) {
    val pid = Process.myPid()
    val id = Integer.toHexString(System.identityHashCode(instance))
    Log.i(TAG, "source=$source event=$event pid=$pid instance=$id")
}
