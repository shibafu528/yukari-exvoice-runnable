package info.shibafu528.yukari.exvoice.runnable

import android.util.Log
import info.shibafu528.yukari.exvoice.pluggaloid.Event
import info.shibafu528.yukari.exvoice.MRuby
import info.shibafu528.yukari.exvoice.pluggaloid.Plugin

/**
 * Pluggaloid-Javaプラグインのサンプル
 */
class SamplePlugin(mRuby: MRuby) : Plugin(mRuby, "sample") {

    @Event("sample")
    fun onSample() {
        Log.d("SamplePlugin", "called!")
    }

    @Event("period")
    fun onPeriod() {
        mRuby.delayerHandler.post {
            call(mRuby, "sample")
            call(mRuby, "sample2")
        }
    }
}
