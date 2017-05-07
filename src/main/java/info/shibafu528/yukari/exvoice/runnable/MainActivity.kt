package info.shibafu528.yukari.exvoice.runnable

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import info.shibafu528.yukari.exvoice.MRuby
import info.shibafu528.yukari.exvoice.Plugin

class MainActivity : AppCompatActivity() {
    var mRuby: MRuby? = null
    var mRubyThread: Thread? = null

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

        // Redefine mruby-print
        mRuby.loadString("""
##
# Kernel
#
# ISO 15.3.1
module Kernel
  ##
  # Invoke method +print+ on STDOUT and passing +*args+
  #
  # ISO 15.3.1.2.10
  def print(*args)
    i = 0
    len = args.size
    while i < len
      __printstr__ args[i].to_s
      i += 1
    end
  end

  ##
  # Invoke method +puts+ on STDOUT and passing +*args*+
  #
  # ISO 15.3.1.2.11
  def puts(*args)
    i = 0
    len = args.size
    while i < len
      s = args[i].to_s
      __printstr__ s
      __printstr__ "\n" if (s[-1] != "\n")
      i += 1
    end
    __printstr__ "\n" if len == 0
    nil
  end

  ##
  # Print human readable object description
  #
  # ISO 15.3.1.3.34
  def p(*args)
    i = 0
    len = args.size
    while i < len
      __printstr__ args[i].inspect
      __printstr__ "\n"
      i += 1
    end
    args[0]
  end

  unless Kernel.respond_to?(:sprintf)
    def printf(*args)
      raise NotImplementedError.new('printf not available')
    end
    def sprintf(*args)
      raise NotImplementedError.new('sprintf not available')
    end
  else
    def printf(*args)
      __printstr__(sprintf(*args))
      nil
    end
  end
end

""")

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
        mRuby.loadString("__printstr__ 'call __printstr__'")
        mRuby.loadString("puts 'call puts'")

        // Run
        val mRubyThread = Thread(Runnable {
            try {

                while (true) {
                    Plugin.call(mRuby, "sample")
                    Plugin.call(mRuby, "sample2")
                    mRuby.callTopLevelFunc("tick")
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
        mRuby?.close()
    }
}
