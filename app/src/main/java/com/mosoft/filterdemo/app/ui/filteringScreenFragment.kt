package com.mosoft.filterdemo.app.ui

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.writer.WriterProcessor
import com.mosoft.filterdemo.R
import com.mosoft.filterdemo.app.base.baseFragment
import com.mosoft.filterdemo.app.events.Events
import com.mosoft.filterdemo.app.filterList.filterListRVAdapter
import com.mosoft.filterdemo.databinding.FragmentFilteringScreenBinding
import com.mosoft.filterdemo.db.filterList
import com.squareup.otto.Subscribe
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.SampleBuffer
import java.io.*
import java.lang.Math.max
import java.lang.Math.min


class filteringScreenFragment: baseFragment() {

    var mBufferSize = 64000
    var mOverlap = 32000

    lateinit var binding : FragmentFilteringScreenBinding

    lateinit var playerOriginal : MediaPlayer
    lateinit var playerFiltered : MediaPlayer
    lateinit var PHeffectA : PresetReverb
    lateinit var PHeffectB : PresetReverb
    lateinit var playerOriginalViz : Visualizer
    lateinit var playerFilteredViz : Visualizer
    lateinit var tmpFileOriginal : File
    lateinit var tmpFileFiltered : File
    lateinit var mediaExtractor : MediaExtractor
    lateinit var outputFile : RandomAccessFile
    lateinit var mediaFormat : MediaFormat
    lateinit var pcmArray : ShortArray

    var currentFilterId = 0
    var isPlaying = false //For Handling onPause and onResume continuity
    var isLooping = false
    var isEditingParameter = false
    var boolOne = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_filtering_screen, container, false)

        binding.lyParameter1.root.visibility = View.GONE
        binding.lyParameter2.root.visibility = View.GONE
        binding.lyParameter3.root.visibility = View.GONE
        binding.lyParameter4.root.visibility = View.GONE

        binding.btnShowMenu.setOnClickListener {
            if(it.isClickable) {
                binding.btnShowMenu.isClickable = false
                binding.btnRightMenuClose.isClickable = true
                binding.lyRightMenuMaster.visibility = View.VISIBLE
                binding.lyRightMenuOver.isClickable = false
                menuDisableOtherButtons()

                binding.lyRightMenuMain.apply {
                    clearAnimation()
                    animation = AnimationUtils.loadAnimation(context, R.anim.animation_slide_in_from_right)
                    animation.duration = 300
                    animate()
                }

                binding.lyRightMenuOver.apply {
                    clearAnimation()
                    animation = AnimationUtils.loadAnimation(context, R.anim.animation_fade_in)
                    animation.duration = 300
                    animate()
                }

                binding.lyRightMenuMain.animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        binding.lyRightMenuOver.isClickable = true
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }

                })
                //requireActivity().onBackPressedDispatcher.hasEnabledCallbacks(true)
            }
        }

        binding.btnRightMenuClose.setOnClickListener {
            if(it.isClickable) {
                binding.btnRightMenuClose.isClickable = false
                binding.btnShowMenu.isClickable = true
                binding.lyRightMenuOver.isClickable = false
                menuDisableOtherButtons()

                binding.lyRightMenuMain.apply {
                    binding.lyRightMenuMain.clearAnimation()
                    animation = AnimationUtils.loadAnimation(context, R.anim.animation_slide_out_to_right)
                    animation.duration = 300
                    animate()
                }

                binding.lyRightMenuOver.apply {
                    binding.lyRightMenuOver.clearAnimation()
                    animation = AnimationUtils.loadAnimation(context, R.anim.animation_fade_out)
                    animation.duration = 300
                    animate()
                }

                binding.lyRightMenuMain.animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        binding.lyRightMenuMaster.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }

                })
                //requireActivity().onBackPressedDispatcher.hasEnabledCallbacks(false)
            }
        }

        binding.lyRightMenuOver.setOnClickListener {
            binding.btnRightMenuClose.performClick()
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(binding.lyRightMenuMaster.visibility == View.VISIBLE)
                    binding.btnRightMenuClose.performClick()
                else
                    findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,callback)

        //Player

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val mUri : Uri = result.data?.data!!

                prepFiles()
                createFileFromStream(requireContext().contentResolver.openInputStream(mUri)!!,tmpFileOriginal)

                mediaExtractor = MediaExtractor()
                mediaExtractor.setDataSource(tmpFileOriginal.path)
                mediaFormat = mediaExtractor.getTrackFormat(0)

                setupAudio(tmpFileOriginal, 500f)

                val originalUri = Uri.parse(tmpFileOriginal.path)
                val filteredUri = Uri.parse(tmpFileFiltered.path)

                playerOriginal = MediaPlayer.create(context, originalUri)
                playerFiltered = MediaPlayer.create(context, filteredUri)
                //playerFiltered = MediaPlayer.create(context, originalUri)
/*
                playerOriginalViz = Visualizer(0)
                playerFilteredViz = Visualizer(0)
                playerOriginalViz.apply {
                    enabled = true
                    captureSize = Visualizer.getMaxCaptureRate()
                }
                playerFilteredViz.apply {
                    enabled = true
                    captureSize = Visualizer.getMaxCaptureRate()
                }

                playerOriginalViz.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        var tmp = binding.tvAudioOriginalWv.text.toString() + "/n" + waveform?.get(0).toString()
                        binding.tvAudioOriginalWv.text = tmp
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        var tmp = binding.tvAudioOriginalWv.text.toString() + "/n" + fft?.get(0).toString()
                        binding.tvAudioOriginalWv.text = tmp
                    }
                }, Visualizer.getMaxCaptureRate(), true, true)
*/
                PHeffectA = PresetReverb(1,playerFiltered.audioSessionId)
                PHeffectA.preset = PresetReverb.PRESET_LARGEHALL
                PHeffectA.enabled = true
                playerFiltered.setAuxEffectSendLevel(1f)

                //Enable Media Controls
                setupSeekBar()
                if(boolOne) {
                    massButtonEnabler()
                    boolOne = false
                }

                //OnCompletionButtonHandler
                playerOriginal.setOnCompletionListener {
                    if(!it.isLooping)
                        playPauseMediaPlayer()
                }

                playerOriginal.setVolume(0f,0f)
                playerFiltered.setVolume(1f,1f)
            }
        }

        binding.btnLoadFile.setOnClickListener {
            val mIntent = Intent(Intent.ACTION_GET_CONTENT)
            mIntent.type = "audio/*"
            resultLauncher.launch(Intent.createChooser(mIntent,"Select Audio File"))
        }

        binding.btnToggle.setOnTouchListener { it, motionEvent ->
            //playerFiltered.seekTo(playerOriginal.currentPosition)
            if(motionEvent.action == MotionEvent.ACTION_DOWN) {
                binding.btnToggle.setColorFilter(resources.getColor(R.color.PHred))
                playerOriginal.setVolume(1f,1f)
                playerFiltered.setVolume(0f,0f)

                //MakeToast("Listening to Unfiltered Audio")  //DEBUG
            } else if(motionEvent.action == MotionEvent.ACTION_UP) {
                binding.btnToggle.clearColorFilter()
                playerOriginal.setVolume(0f,0f)
                playerFiltered.setVolume(1f,1f)

                //MakeToast("Listening to Filtered Audio")    //DEBUG
            }

            return@setOnTouchListener true
        }

        //Media Control

        binding.btnApplyFilter.setOnClickListener {
            clearFilteredFile()

            setupAudio(tmpFileOriginal, binding.lyParameter1.sldSlider.progress.toFloat())

            val filteredUri = Uri.parse(tmpFileFiltered.path)

            if (isPlaying) {
                playPauseMediaPlayer()
                playerFiltered.reset()
                playerFiltered = MediaPlayer.create(context, filteredUri)
                playerFiltered.seekTo(playerOriginal.currentPosition)
                playPauseMediaPlayer()
            } else {
                playerFiltered.reset()
                playerFiltered = MediaPlayer.create(context, filteredUri)
                playerFiltered.seekTo(playerOriginal.currentPosition)
            }
        }

        binding.btnPlayPause.setOnClickListener {
            playPauseMediaPlayer()
        }

        binding.btnBackward.setOnClickListener {
            seekToMediaPlayer(kotlin.math.max(playerOriginal.currentPosition + 5000,0))
        }

        binding.btnForward.setOnClickListener {
            seekToMediaPlayer(kotlin.math.min(playerOriginal.currentPosition + 5000,playerOriginal.duration))
        }

        binding.btnPrevious.setOnClickListener {
            seekToMediaPlayer(0)
        }

        binding.btnLoop.setOnClickListener {
            if(isLooping) {
                isLooping = false
                playerOriginal.isLooping = false
                playerFiltered.isLooping = false
            }
            else {
                isLooping = true
                playerOriginal.isLooping = true
                playerFiltered.isLooping = true
            }
        }

        binding.lySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser)
                    seekToMediaPlayer(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        massButtonEnabler()
        //Needs to be put after finishing all setOnClick
        //Prevents Media Control before loading a File

        //FilterList
        val filterListRecyclerView = binding.rvFilterList
        filterListRecyclerView.adapter = filterListRVAdapter(filterList())
        filterListRecyclerView.layoutManager = LinearLayoutManager(context)
        filterListRecyclerView.setHasFixedSize(true)

        //Sliders
        binding.lyParameter1.etSliderParameter.setOnFocusChangeListener { _, hasFocus ->
            isEditingParameter = hasFocus
            binding.root.isFocusable = hasFocus
            val view = binding.lyParameter1
            val tmp =
                try {
                    view.etSliderParameter.text.toString().toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            view.etSliderParameter
            view.sldSlider.progress =
                min(
                    max(
                        0,
                        tmp
                    ),
                    view.sldSlider.max
                )
            if(!isEditingParameter) {
                hideKeyboard()
            }
        }

        binding.lyParameter2.etSliderParameter.setOnFocusChangeListener { _, hasFocus ->
            isEditingParameter = hasFocus
            binding.root.isFocusable = hasFocus
            val view = binding.lyParameter2
            val tmp =
                try {
                    view.etSliderParameter.text.toString().toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            view.etSliderParameter
            view.sldSlider.progress =
                min(
                    max(
                        0,
                        tmp
                    ),
                    view.sldSlider.max
                )
            if(!isEditingParameter) {
                hideKeyboard()
            }
        }
        binding.lyParameter3.etSliderParameter.setOnFocusChangeListener { _, hasFocus ->
            isEditingParameter = hasFocus
            binding.root.isFocusable = hasFocus
            val view = binding.lyParameter3
            val tmp =
                try {
                    view.etSliderParameter.text.toString().toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            view.etSliderParameter
            view.sldSlider.progress =
                min(
                    max(
                        0,
                        tmp
                    ),
                    view.sldSlider.max
                )
            if(!isEditingParameter) {
                hideKeyboard()
            }
        }
        binding.lyParameter4.etSliderParameter.setOnFocusChangeListener { _, hasFocus ->
            isEditingParameter = hasFocus
            binding.root.isFocusable = hasFocus
            val view = binding.lyParameter4
            val tmp =
                try {
                    view.etSliderParameter.text.toString().toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            view.etSliderParameter
            view.sldSlider.progress =
                min(
                    max(
                        0,
                        tmp
                    ),
                    view.sldSlider.max
                )
            if(!isEditingParameter) {
                hideKeyboard()
            }
        }

        binding.lyParameter1.sldSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.lyParameter1.etSliderParameter.clearFocus()
                    binding.lyParameter1.etSliderParameter.setText(progress.toString())
                }
            }
        })

        binding.lyParameter2.sldSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.lyParameter2.etSliderParameter.clearFocus()
                    binding.lyParameter2.etSliderParameter.setText(progress.toString())
                }
            }
        })

        binding.lyParameter3.sldSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.lyParameter3.etSliderParameter.clearFocus()
                    binding.lyParameter3.etSliderParameter.setText(progress.toString())
                }
            }
        })

        binding.lyParameter4.sldSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.lyParameter4.etSliderParameter.clearFocus()
                    binding.lyParameter4.etSliderParameter.setText(progress.toString())
                }
            }
        })

        return binding.root
    }

    private fun playPauseMediaPlayer() {
        if(isPlaying) {
            playerOriginal.pause()
            playerFiltered.pause()
            isPlaying = false
            binding.btnPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_play_arrow))
        } else {
            if(playerOriginal.currentPosition == playerOriginal.duration) {
                //Handle Status on Completion
                seekToMediaPlayer(0)
            }
            playerOriginal.start()
            playerFiltered.start()
            isPlaying = true
            binding.btnPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_pause))
        }
    }

    private fun seekToMediaPlayer(newPosition: Int) {
        playerOriginal.seekTo(newPosition)
        playerFiltered.seekTo(playerOriginal.currentPosition)
        //binding.tvCurrent.text = timeStringMaker(playerOriginal.currentPosition)
    }

    private fun setupSeekBar() {
        binding.lySeekbar.max = Integer.min(playerOriginal.duration, playerFiltered.duration)
        binding.tvDuration.text = timeStringMaker(playerOriginal.duration)

        val seekBarHandler = Handler()
        seekBarHandler.postDelayed( object : Runnable {
            override fun run() {
                binding.lySeekbar.progress = playerOriginal.currentPosition
                binding.tvCurrent.text = timeStringMaker(binding.lySeekbar.progress)
                seekBarHandler.postDelayed(this,300)
            }
        }, 0)
    }

    private fun massButtonEnabler() {
        if(binding.btnPlayPause.isClickable) {
            binding.btnPlayPause.isClickable = false
            binding.btnPlayPause.alpha = 0.3f

            binding.btnForward.isClickable = false
            binding.btnForward.alpha = 0.3f

            binding.btnBackward.isClickable = false
            binding.btnBackward.alpha = 0.3f

            if(isLooping) {
                binding.btnLoop.performClick()
            }
            binding.btnLoop.isClickable = false
            binding.btnLoop.alpha = 0.3f

            binding.btnPrevious.isClickable = false
            binding.btnPrevious.alpha = 0.3f

            binding.btnToggle.isEnabled = false
            binding.btnToggle.alpha = 0.3f

            binding.lySeekbar.isEnabled = false

        } else {
            binding.btnPlayPause.isClickable = true
            binding.btnPlayPause.alpha = 1.0f

            binding.btnForward.isClickable = true
            binding.btnForward.alpha = 1.0f

            binding.btnBackward.isClickable = true
            binding.btnBackward.alpha = 1.0f

            binding.btnLoop.isClickable = true
            binding.btnLoop.alpha = 1.0f

            binding.btnPrevious.isClickable = true
            binding.btnPrevious.alpha = 1.0f

            binding.btnToggle.isEnabled = true
            binding.btnToggle.alpha = 1.0f

            binding.lySeekbar.isEnabled = true
        }
    }

    private fun menuDisableOtherButtons() {
        binding.btnLoadFile.isClickable = !binding.btnLoadFile.isClickable
        binding.btnToggle.isClickable = !binding.btnToggle.isClickable
    }

    private fun timeStringMaker(timeStamp: Int): String {
        val minute = timeStamp/1000/60
        val second = timeStamp/1000%60

        val minuteString = minute.toString()
        var secondString = second.toString()
        if (second<10)
            secondString = "0$secondString"

        return ("$minuteString:$secondString")
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

    fun prepFiles() {
        val mainPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        val extension = ".wav"
        //CreateDestinationFiles
        tmpFileOriginal = File(mainPath + "/"
                + "Original" + extension)
        tmpFileFiltered = File(mainPath + "/"
                + "Filter-Results" + extension)

        if(tmpFileOriginal.exists())
            tmpFileOriginal.delete()
        if(tmpFileFiltered.exists())
            tmpFileFiltered.delete()

        tmpFileOriginal.createNewFile()
        outputFile = RandomAccessFile(tmpFileFiltered.path, "rw")
    }

    fun clearFilteredFile() {
        if(tmpFileFiltered.exists())
            tmpFileFiltered.delete()
        outputFile = RandomAccessFile(tmpFileFiltered.path, "rw")
    }

    @Subscribe
    fun adjustSliders(event: Events.filterListItemOnClick) {
        currentFilterId = event.data.id

        binding.btnRightMenuClose.performClick()
        val tmp = event.data.numberOfParameters

        binding.lyParameterMaster.apply {
            clearAnimation()
            animation = AnimationUtils.loadAnimation(context, R.anim.animation_slide_out_to_left)
            animation.duration = 300
            animate()
        }

        binding.lyParameter1.root.visibility = View.GONE
        binding.lyParameter2.root.visibility = View.GONE
        binding.lyParameter3.root.visibility = View.GONE
        binding.lyParameter4.root.visibility = View.GONE

        if (tmp >= 1) {
            binding.lyParameter1.root.visibility = View.VISIBLE
            binding.lyParameter1.tvSliderParameterName.text = event.data.parameterString[0]
            binding.lyParameter1.tvSliderParameterUnit.text = event.data.parameterString[1]
            binding.lyParameter1.sldSlider.max = event.data.paramaterInt[0]
        }

        if (tmp >= 2) {
            binding.lyParameter2.root.visibility = View.VISIBLE
            binding.lyParameter2.tvSliderParameterName.text = event.data.parameterString[2]
            binding.lyParameter2.tvSliderParameterUnit.text = event.data.parameterString[3]
            binding.lyParameter1.sldSlider.max = event.data.paramaterInt[1]
        }

        if (tmp >= 3) {
            binding.lyParameter3.root.visibility = View.VISIBLE
            binding.lyParameter3.tvSliderParameterName.text = event.data.parameterString[4]
            binding.lyParameter3.tvSliderParameterUnit.text = event.data.parameterString[5]
            binding.lyParameter1.sldSlider.max = event.data.paramaterInt[2]
        }

        if (tmp >= 4) {
            binding.lyParameter4.root.visibility = View.VISIBLE
            binding.lyParameter4.tvSliderParameterName.text = event.data.parameterString[6]
            binding.lyParameter4.tvSliderParameterUnit.text = event.data.parameterString[7]
            binding.lyParameter1.sldSlider.max = event.data.paramaterInt[3]
        }

        binding.lyParameterMaster.apply {
            clearAnimation()
            animation = AnimationUtils.loadAnimation(context, R.anim.animation_slide_in_from_right)
            animation.duration = 300
            animate()
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
                val pcm: ShortArray = buffer.buffer
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

        audioDispatcher.addAudioProcessor(LowPassFS(fCut,fs))
        audioDispatcher.addAudioProcessor(WriterProcessor(audioFormat, outputFile))
        audioDispatcher.run()

        val audioThread = Thread(audioDispatcher, "Audio Thread")

        return
    }
}