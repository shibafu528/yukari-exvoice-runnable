package info.shibafu528.yukari.exvoice.runnable

import android.util.Log
import info.shibafu528.yukari.exvoice.MRuby
import info.shibafu528.yukari.exvoice.pluggaloid.*

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

    @Spell("compose", constraints = ["virtual_world"])
    fun compose(world: Any, @Keyword("body") body: String?, @RestKeywords options: Map<String, Any?>) {
        Log.d("SamplePlugin", "compose[virtual_world] called! (body: $body)")
    }

    @Spell("compose", constraints = ["virtual_world", "twitter_tweet"])
    fun compose(world: Any, tweet: Map<String, Any?>, @Keyword("body") body: String?, @RestKeywords options: Map<String, Any?>) {
        Log.d("SamplePlugin", "compose[virtual_world, twitter_tweet] called! (body: $body)")
    }
}
