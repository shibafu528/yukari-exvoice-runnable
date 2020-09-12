package info.shibafu528.yukari.exvoice.runnable

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.annotation.RawRes
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import info.shibafu528.yukari.exvoice.MRuby
import info.shibafu528.yukari.exvoice.MRubyPointer
import info.shibafu528.yukari.exvoice.pluggaloid.Plugin
import twitter4j.Status
import twitter4j.TwitterObjectFactory

class MainActivity : AppCompatActivity() {
    var mRuby: MRuby? = null
    var mRubyThread: Thread? = null
    var mRubyPointers: Set<MRubyPointer> = mutableSetOf()

    val stdoutView: TextView by lazy { findViewById(R.id.text) as TextView }
    val printCallbackHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val text = msg.obj.toString()
            stdoutView.append("\n" + text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        mRuby.loadString("Android.require_assets 'bootstrap.rb'")

        // Load Java Plugin
        mRuby.registerPlugin(SamplePlugin::class.java)

        // Load Ruby Plugin
        mRuby.loadString("""
Plugin.create :sample_2 do
  on_sample2 do |v|
    Android::Log.d 'on_sample2: call Log.d'
    puts 'on_sample2: call puts'
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

        mRuby.loadString("__printstr__ 'call __printstr__'")
        mRuby.loadString("puts 'call puts'")

        // Update UserConfig
        mRuby.loadString("puts 'Last Startup: ' + UserConfig['startup_timestamp'].to_s")
        mRuby.loadString("UserConfig['startup_timestamp'] = " + System.currentTimeMillis())
        mRuby.loadString("puts 'Startup: ' + UserConfig['startup_timestamp'].to_s")

        // Status to Message
        fun createStatusFromRes(@RawRes resId: Int) =
                TwitterObjectFactory.createStatus(resources.openRawResource(resId).use { it.bufferedReader().readText() })
        fun Status.toMessage(mRuby: MRuby) =
                StatusConverter.toMessage(mRuby, this)

        val messages = arrayOf(
                createStatusFromRes(R.raw.tweets_215401020602322945).toMessage(mRuby).apply { mRubyPointers += this },
                createStatusFromRes(R.raw.tweets_870941180665540608).toMessage(mRuby).apply { mRubyPointers += this }
        )
        Plugin.call(mRuby, "appear", messages)

        // Run
        val mRubyThread = Thread(Runnable {
            try {
                while (true) {
                    Plugin.call(mRuby, "sample")
                    Plugin.call(mRuby, "sample2")
                    mRuby.callTopLevelProc("tick")
                    Thread.sleep(500)
                }
            } catch (e: InterruptedException) {
                Log.d("ExVoiceRunner", "Interrupt!")
            }
        }, "ExVoiceRunner")
        mRubyThread.start()

        this.mRuby = mRuby
        this.mRubyThread = mRubyThread
    }

    override fun onDestroy() {
        super.onDestroy()
        mRubyThread?.interrupt()
        mRubyPointers.forEach { it.dispose() }
        mRubyPointers = mutableSetOf()
        mRuby?.close()
    }
}
