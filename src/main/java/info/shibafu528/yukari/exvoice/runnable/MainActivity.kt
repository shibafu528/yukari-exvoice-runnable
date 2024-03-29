package info.shibafu528.yukari.exvoice.runnable

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import info.shibafu528.yukari.exvoice.MRuby
import info.shibafu528.yukari.exvoice.MRubyPointer
import info.shibafu528.yukari.exvoice.miquire.Miquire
import info.shibafu528.yukari.exvoice.pluggaloid.Plugin
import twitter4j.TwitterObjectFactory

class MainActivity : AppCompatActivity() {
    var mRuby: MRuby? = null
    var mRubyPointers: Set<MRubyPointer> = mutableSetOf()

    lateinit var stdoutView: TextView
    val printCallbackHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val text = msg.obj.toString()
            stdoutView.append("\n" + text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        stdoutView = findViewById(R.id.text)

        // Construct mruby
        val mRuby = MRuby(applicationContext)

        // Set stdout callback
        mRuby.setPrintCallback(MRuby.PrintCallback { value ->
            if (value == null || value.isEmpty() || "\n" == value) {
                return@PrintCallback
            }
            Log.d("ExVoice (stdout)", value)
            printCallbackHandler.sendMessage(printCallbackHandler.obtainMessage(0, value))
        })

        // Load bootstrap
        mRuby.requireAssets("bootstrap.rb")

        Miquire.loadAll(mRuby)
        Plugin.call(mRuby, "boot", null)

        // Load Java Plugin
        mRuby.registerPlugin(SamplePlugin::class.java)

        // Load Ruby Plugin
        mRuby.loadString("""
Plugin.create :sample_2 do
  on_sample2 do |v|
    Android::Log.d 'on_sample2: call Log.d'
    puts 'on_sample2: call puts'
    notice 'on_sample2: call notice'
  end
end
""")
        mRuby.loadString("""
Plugin.create :sample_3 do
  on_sample do |v|
    Delayer::Deferred.new {
      puts "sample_3:0"
    }.next {
      puts "sample_3:1"
    }
  end
end
""")
        mRuby.loadString("""
Plugin.create :message_receiver do
  on_appear do |msgs|
    msgs.each {|msg| puts msg.inspect }
  end
end
""")
        mRuby.loadString("""
            class VirtualWorld < Diva::Model
                register :virtual_world
            end
            
            Plugin.create :message_composer do
                on_appear do |msgs|
                    world, = Plugin.filtering(:world_current, nil)
                    msgs.each { |msg| compose(world, msg) }
                end
                
                vworld = VirtualWorld.new({})
                filter_world_current do |result|
                    if result
                        [result]
                    else
                        [vworld]
                    end
                end
                
                on_boot do
                    compose(vworld, body: '(^o^)アゲハ蝶歌います！ (^o^)しじん ヾ(＠⌒ー⌒＠)ノておくれ〜').trap do |e|
                        error e
                    end
                end
            end
        """.trimIndent())

        mRuby.loadString("__printstr__ 'call __printstr__'")
        mRuby.loadString("puts 'call puts'")

        // Update UserConfig
        mRuby.loadString("puts 'Last Startup: ' + UserConfig['startup_timestamp'].to_s")
        mRuby.loadString("UserConfig['startup_timestamp'] = " + System.currentTimeMillis())
        mRuby.loadString("puts 'Startup: ' + UserConfig['startup_timestamp'].to_s")

        // Status to Message
        fun createStatusFromRes(@RawRes resId: Int) =
                TwitterObjectFactory.createStatus(resources.openRawResource(resId).use { it.bufferedReader().readText() })
        val messages = arrayOf(
                createStatusFromRes(R.raw.tweets_215401020602322945).toMessage(mRuby).apply { mRubyPointers += this },
                createStatusFromRes(R.raw.tweets_870941180665540608).toMessage(mRuby).apply { mRubyPointers += this }
        )
        Plugin.call(mRuby, "appear", messages)

        this.mRuby = mRuby
    }

    override fun onDestroy() {
        super.onDestroy()
        mRubyPointers.forEach { it.dispose() }
        mRubyPointers = mutableSetOf()
        mRuby?.close()
    }
}
