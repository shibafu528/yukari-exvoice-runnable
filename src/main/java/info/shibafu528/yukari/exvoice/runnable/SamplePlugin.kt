package info.shibafu528.yukari.exvoice.runnable

import android.util.Log
import info.shibafu528.yukari.exvoice.Event
import info.shibafu528.yukari.exvoice.MRuby
import info.shibafu528.yukari.exvoice.Plugin

/**
 * Pluggaloid-Javaプラグインのサンプル
 */
class SamplePlugin(mRuby: MRuby) : Plugin(mRuby, "sample") {

    @Event("sample")
    fun onSample() {
        Log.d("SamplePlugin", "called!")
    }
}
