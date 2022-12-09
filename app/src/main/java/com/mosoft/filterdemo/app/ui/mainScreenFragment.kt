package com.mosoft.filterdemo.app.ui

import android.R.interpolator.linear
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.mosoft.filterdemo.R
import com.mosoft.filterdemo.app.baseFragment.baseFragment
import com.mosoft.filterdemo.databinding.FragmentMainScreenBinding
import java.lang.Integer.min
import java.lang.Math.max


class mainScreenFragment: baseFragment() {

    lateinit var binding : FragmentMainScreenBinding
    lateinit var playerOriginal : MediaPlayer
    lateinit var playerFiltered : MediaPlayer
    lateinit var playerOriginalViz : Visualizer
    lateinit var playerFilteredViz : Visualizer
    var isPlaying = false //For Handling onPause and onResume continuity

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_main_screen, container, false)

        binding.btnShowMenu.setOnClickListener {
            if(binding.lyRightMenu.visibility == View.GONE) {
                binding.lyRightMenu.visibility = View.VISIBLE
            } else {
                binding.lyRightMenu.visibility = View.GONE
            }
        }

        var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val mUri : Uri = result.data?.data!!

                //var tmp = setupAudio(mUri)

                playerOriginal = MediaPlayer.create(context, mUri)
                /*
                playerOriginalViz = Visualizer(playerOriginal.audioSessionId)
                playerOriginalViz.apply {
                    measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS
                    scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                    captureSize = Visualizer.getCaptureSizeRange()[0]
                    enabled = true
                }*/

                //tmp = setupAudio(mUri)

                playerFiltered = MediaPlayer.create(context, mUri)
                //playerFilteredViz = tmp.second



                //Test Effect
                val aTmp = PresetReverb(0, playerFiltered.audioSessionId)
                aTmp.preset = PresetReverb.PRESET_SMALLROOM
                aTmp.enabled = true
                //Test Effect

                playerOriginal.setVolume(0f,0f)

                //MakeToast(mUri.path!!)    //DEBUG
            }
        }

        binding.btnLoadFile.setOnClickListener {
            val mIntent = Intent(Intent.ACTION_GET_CONTENT)
            mIntent.type = "audio/*"
            resultLauncher.launch(Intent.createChooser(mIntent,"Select Audio File"))
        }

        binding.btnToggle.setOnTouchListener { _, motionEvent ->
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

    fun setupAudio(audioUri : Uri) : Pair<MediaPlayer,Visualizer> {
        var mMediaPlayer = MediaPlayer.create(context, audioUri)
        var mVisualizer = Visualizer(mMediaPlayer.audioSessionId)

        mVisualizer.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS
        mVisualizer.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
        mVisualizer.captureSize = Visualizer.getCaptureSizeRange()[0]
        mVisualizer.enabled = true

        return Pair.create(mMediaPlayer,mVisualizer)
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
}