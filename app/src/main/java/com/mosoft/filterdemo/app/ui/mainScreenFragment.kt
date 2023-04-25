package com.mosoft.filterdemo.app.ui

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator
import be.tarsos.dsp.util.FFMPEGDownloader
import be.tarsos.dsp.writer.WriterProcessor
import com.mosoft.filterdemo.R
import com.mosoft.filterdemo.app.baseFragment.baseFragment
import com.mosoft.filterdemo.databinding.FragmentMainScreenBinding
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.SampleBuffer
import java.io.*
import java.lang.Integer.min
import java.lang.Math.max


class mainScreenFragment: baseFragment() {

    var mBufferSize = 64000
    var mOverlap = 32000

    lateinit var binding : FragmentMainScreenBinding
    lateinit var playerOriginal : MediaPlayer
    lateinit var playerFiltered : MediaPlayer
    lateinit var playerOriginalViz : Visualizer
    lateinit var playerFilteredViz : Visualizer
    lateinit var tmpFileOriginal : File
    lateinit var tmpFileFiltered : File
    lateinit var mediaExtractor : MediaExtractor
    lateinit var outputFile : RandomAccessFile
    lateinit var mediaFormat : MediaFormat
    lateinit var pcmArray : ShortArray
    var isPlaying = false //For Handling onPause and onResume continuity

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_main_screen, container, false)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.btnRightMenuClose.performClick()
            }
        }

        tmpFileOriginal = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
                + "/"
                + ""
                + "Original.wav")
        if(tmpFileOriginal.exists())
            tmpFileOriginal.delete()
        tmpFileOriginal.createNewFile()
        tmpFileFiltered = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
                + "/"
                + ""
                + "Filter-Results.wav")
        if(tmpFileFiltered.exists())
            tmpFileFiltered.delete()
        outputFile = RandomAccessFile(tmpFileFiltered.path, "rw")

        //requireActivity().onBackPressedDispatcher.addCallback(this,callback)

        //requireActivity().onBackPressedDispatcher.OnBackPressedCallback(false)

        binding.btnShowMenu.setOnClickListener {
            if(it.isClickable) {
                binding.btnShowMenu.isClickable = false
                binding.btnRightMenuClose.isClickable = true
                binding.lyRightMenuMaster.visibility = View.VISIBLE
                //requireActivity().onBackPressedDispatcher.hasEnabledCallbacks(true)
            }
        }

        binding.btnRightMenuClose.setOnClickListener {
            if(it.isClickable) {
                binding.btnRightMenuClose.isClickable = false
                binding.btnShowMenu.isClickable = true
                binding.lyRightMenuMaster.visibility = View.GONE
                //requireActivity().onBackPressedDispatcher.hasEnabledCallbacks(false)
            }
        }

        binding.lyRightMenuOver.setOnClickListener {
            binding.btnRightMenuClose.performClick()
        }

        //Player

        var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("DEBUG HERE", "mUri Read")
                val mUri : Uri = result.data?.data!!

                Log.d("DEBUG HERE", "crate file")
                createFileFromStream(requireContext().contentResolver.openInputStream(mUri)!!,tmpFileOriginal)
                Log.d("DEBUG HERE", "crate file done")

                mediaExtractor = MediaExtractor()
                mediaExtractor.setDataSource(tmpFileOriginal.path)
                mediaFormat = mediaExtractor.getTrackFormat(0)
                Log.d("DEBUG HERE", "medexctr don")

                setupAudio(tmpFileOriginal, 800f)
                Log.d("DEBUG HERE", "setup audio don")

                val originalUri = Uri.parse(tmpFileOriginal.path)
                val filteredUri = Uri.parse(tmpFileFiltered.path)

                playerOriginal = MediaPlayer.create(context, originalUri)
                playerFiltered = MediaPlayer.create(context, filteredUri)

                playerOriginal.setVolume(0f,0f)
            }
        }

        binding.btnLoadFile.setOnClickListener {
            val mIntent = Intent(Intent.ACTION_GET_CONTENT)
            mIntent.type = "audio/*"
            resultLauncher.launch(Intent.createChooser(mIntent,"Select Audio File"))
        }

        binding.btnToggle.setOnTouchListener { _, motionEvent ->
            playerFiltered.seekTo(playerOriginal.currentPosition)
            if(motionEvent.action == MotionEvent.ACTION_DOWN) {
                playerOriginal.setVolume(1f,1f)
                playerFiltered.setVolume(0f,0f)

                //MakeToast("Listening to Unfiltered Audio")  //DEBUG
            } else if(motionEvent.action == MotionEvent.ACTION_UP) {
                playerOriginal.setVolume(0f,0f)
                playerFiltered.setVolume(1f,1f)

                //MakeToast("Listening to Filtered Audio")    //DEBUG
            }

            return@setOnTouchListener true
        }

        //Media Control

        binding.btnPlayPause.setOnClickListener {
            if(isPlaying) {
                pauseMediaPlayer()
                binding.btnPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_play_arrow))
            } else {
                playMediaPlayer()
                binding.btnPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_pause))
            }
        }

        binding.btnBackward.setOnClickListener {
            playerOriginal.seekTo(max(playerOriginal.currentPosition - 5000,0))
            playerFiltered.seekTo(playerOriginal.currentPosition)
        }

        binding.btnForward.setOnClickListener {
            playerOriginal.seekTo(min(playerOriginal.currentPosition + 5000,playerOriginal.duration))
            playerFiltered.seekTo(playerOriginal.currentPosition)
        }

        binding.btnPrevious.setOnClickListener {
            playerOriginal.seekTo(0)
            playerFiltered.seekTo(0)
        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()

        if(isPlaying) {
            playerOriginal.pause()
            playerFiltered.pause()
        }
    }

    override fun onResume() {
        super.onResume()

        if(isPlaying) {
            playerOriginal.start()
            playerFiltered.start()
        }
    }

    fun playMediaPlayer() {
        if(!isPlaying) {
            playerOriginal.start()
            playerFiltered.start()
            playerFiltered.seekTo(playerOriginal.currentPosition)
            isPlaying = true
        }
    }

    fun pauseMediaPlayer() {
        if(isPlaying) {
            playerOriginal.pause()
            playerFiltered.pause()
            isPlaying = false
        }
    }

    private fun decode(input : InputStream) {
        val output = ArrayList<Short>(1024)
        val bitstream = Bitstream(input)
        val decoder = javazoom.jl.decoder.Decoder()
        var total_ms = 0f
        var nextNotify = -1f
        while (true) {
            val frameHeader: javazoom.jl.decoder.Header? = bitstream.readFrame()
            if (frameHeader == null) {
                break
            } else {
                total_ms += frameHeader.ms_per_frame()
                val buffer: SampleBuffer =
                    decoder.decodeFrame(frameHeader, bitstream) as SampleBuffer // CPU intense
                val pcm: ShortArray = buffer.getBuffer()
                var i = 0
                while (i < pcm.size - 1) {
                    val l = pcm[i]
                    val r = pcm[i + 1]
                    val mono = ((l + r) / 2f).toInt().toShort()
                    output.add(mono) // RAM intense
                    i += 2
                }
                pcmArray = pcm
            }
            bitstream.closeFrame()
        }
        bitstream.close()
        val monoSamples = ShortArray(pcmArray.size / 2)
        val HEADER_LENGTH = 22
        var k = 0
        for (i in monoSamples.indices) {
            if (k > HEADER_LENGTH) {
                monoSamples[i] = ((pcmArray[i * 2] + pcmArray[i * 2 + 1]) / 2).toShort()
            }
            k++
        }
    }

    fun createFileFromStream(ins: InputStream, destination1: File?) {
        try {
            FileOutputStream(destination1).use { os ->
                val buffer = ByteArray(mBufferSize)
                var length: Int
                while (ins.read(buffer).also { length = it } > 0) {
                    os.write(buffer, 0, length)
                }
                os.flush()
            }
        } catch (ex: Exception) {
            Log.d("Save File", ex.message!!)
            ex.printStackTrace()
        }
    }

    fun setupAudio(oriFile : File, fCut : Float) {

        val fs = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).toFloat()
        val channel = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val audioFormat = TarsosDSPAudioFormat(fs, 16, channel, false, false)

        val inputStream = FileInputStream(oriFile)
        val audioStream = UniversalAudioInputStream(inputStream,audioFormat)
        val audioDispatcher = AudioDispatcher(audioStream,mBufferSize,mOverlap)
        val writer = WriterProcessor(audioFormat, outputFile)

        audioDispatcher.addAudioProcessor(LowPassFS(fCut,fs))
        audioDispatcher.addAudioProcessor(writer)
        audioDispatcher.run()

        val audioThread = Thread(audioDispatcher, "Audio Thread")

        return
    }
}