/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.benchmark.perfetto

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Outputs
import androidx.benchmark.Outputs.dateToFileName
import androidx.benchmark.PropOverride

/**
 * Wrapper for [PerfettoCapture] which does nothing below L.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PerfettoCaptureWrapper {
    private var capture: PerfettoCapture? = null
    private val TRACE_ENABLE_PROP = "persist.traced.enable"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            capture = PerfettoCapture()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun start(packages: List<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(PerfettoHelper.LOG_TAG, "Recording perfetto trace")
            capture?.start(packages)
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun stop(benchmarkName: String, iteration: Int): String {
        val iterString = iteration.toString().padStart(3, '0')
        val traceName = "${benchmarkName}_iter${iterString}_${dateToFileName()}.perfetto-trace"
        return Outputs.writeFile(fileName = traceName, reportKey = "perfetto_trace_$iterString") {
            capture!!.stop(it.absolutePath)
        }
    }

    fun record(
        benchmarkName: String,
        iteration: Int,
        packages: List<String>,
        block: () -> Unit
    ): String? {
        if (Build.VERSION.SDK_INT < 21) {
            return null // tracing not supported
        }

        // Prior to Android 11 (R), a shell property must be set to enable perfetto tracing, see
        // https://perfetto.dev/docs/quickstart/android-tracing#starting-the-tracing-services
        val propOverride = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            PropOverride(TRACE_ENABLE_PROP, "1")
        } else null
        try {
            propOverride?.forceValue()
            start(packages)
            block()
            return stop(benchmarkName, iteration)
        } finally {
            propOverride?.resetIfOverridden()
        }
    }
}
