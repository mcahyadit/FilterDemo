package com.mosoft.filterdemo.app.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.mosoft.filterdemo.R
import com.mosoft.filterdemo.app.base.baseFragment
import com.mosoft.filterdemo.databinding.FragmentPreFilteredScreenBinding
import java.lang.Integer.min
import java.lang.Math.abs

class preFilteredScreenFragment : baseFragment() {

    lateinit var binding: FragmentPreFilteredScreenBinding

    lateinit var playerOriginal: MediaPlayer
    lateinit var playerFiltered: MediaPlayer
    lateinit var mUriOriginal: Uri
    lateinit var mUriFiltered: Uri

    var isPlaying = false //For Handling onPause and onResume continuity
    var isLooping = false
    var firstLoad = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_pre_filtered_screen, container, false)

        val resultLauncherFirstLoad =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    mUriOriginal = result.data?.data!!
                }
            }
        val resultLauncherSecondLoad =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    mUriFiltered = result.data?.data!!

                    //Check duration
                    val mediaExtractor1 = MediaMetadataRetriever()
                    mediaExtractor1.setDataSource(context, mUriOriginal)
                    val mediaExtractor2 = MediaMetadataRetriever()
                    mediaExtractor2.setDataSource(context, mUriFiltered)

                    val dur1 =
                        mediaExtractor1.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                            .toInt()
                    val dur2 =
                        mediaExtractor2.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                            .toInt()

                    if (abs(dur1 - dur2) >= 300) {
                        MakeToast("Invalid Input - Duration of two files does not match")
                    } else {
                        playerOriginal = MediaPlayer.create(context, mUriOriginal)
                        playerFiltered = MediaPlayer.create(context, mUriFiltered)
                        setupSeekBar()
                        if (firstLoad) {
                            massButtonEnabler()
                            firstLoad = false
                        }

                        //OnCompletionButtonHandler
                        playerOriginal.setOnCompletionListener {
                            if (!it.isLooping)
                                playPauseMediaPlayer()
                        }

                        playerOriginal.setVolume(0f, 0f)
                        playerFiltered.setVolume(1f, 1f)
                    }
                }
            }

        binding.btnLoadFile.setOnClickListener {
            val mIntent = Intent(Intent.ACTION_GET_CONTENT)
            mIntent.type = "audio/*"
            resultLauncherFirstLoad.launch(Intent.createChooser(mIntent, "Select Audio File"))
            resultLauncherSecondLoad.launch(Intent.createChooser(mIntent, "Select Audio File"))
        }

        binding.btnToggle.setOnTouchListener { it, motionEvent ->
            //playerFiltered.seekTo(playerOriginal.currentPosition)
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                binding.btnToggle.setColorFilter(resources.getColor(R.color.PHred))
                playerOriginal.setVolume(1f, 1f)
                playerFiltered.setVolume(0f, 0f)

                //MakeToast("Listening to Unfiltered Audio")  //DEBUG
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                binding.btnToggle.clearColorFilter()
                playerOriginal.setVolume(0f, 0f)
                playerFiltered.setVolume(1f, 1f)

                //MakeToast("Listening to Filtered Audio")    //DEBUG
            }

            return@setOnTouchListener true
        }

        binding.btnPlayPause.setOnClickListener {
            playPauseMediaPlayer()
        }

        binding.btnBackward.setOnClickListener {
            seekToMediaPlayer(kotlin.math.max(playerOriginal.currentPosition + 5000, 0))
        }

        binding.btnForward.setOnClickListener {
            seekToMediaPlayer(
                kotlin.math.min(
                    playerOriginal.currentPosition + 5000,
                    playerOriginal.duration
                )
            )
        }

        binding.btnPrevious.setOnClickListener {
            seekToMediaPlayer(0)
        }

        binding.btnLoop.setOnClickListener {
            if (isLooping) {
                isLooping = false
                playerOriginal.isLooping = false
                playerFiltered.isLooping = false
            } else {
                isLooping = true
                playerOriginal.isLooping = true
                playerFiltered.isLooping = true
            }
        }

        binding.lySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    seekToMediaPlayer(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        massButtonEnabler()

        return binding.root
    }

    private fun playPauseMediaPlayer() {
        if (isPlaying) {
            playerOriginal.pause()
            playerFiltered.pause()
            isPlaying = false
            binding.btnPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_play_arrow))
        } else {
            if (playerOriginal.currentPosition == playerOriginal.duration) {
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
        binding.lySeekbar.max = min(playerOriginal.duration, playerFiltered.duration)
        binding.tvDuration.text = timeStringMaker(playerOriginal.duration)

        val seekBarHandler = Handler()
        seekBarHandler.postDelayed(object : Runnable {
            override fun run() {
                binding.lySeekbar.progress = playerOriginal.currentPosition
                binding.tvCurrent.text = timeStringMaker(binding.lySeekbar.progress)
                seekBarHandler.postDelayed(this, 300)
            }
        }, 0)
    }

    private fun massButtonEnabler() {
        if (binding.btnPlayPause.isClickable) {
            binding.btnPlayPause.isClickable = false
            binding.btnPlayPause.alpha = 0.3f

            binding.btnForward.isClickable = false
            binding.btnForward.alpha = 0.3f

            binding.btnBackward.isClickable = false
            binding.btnBackward.alpha = 0.3f

            if (isLooping) {
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

    private fun timeStringMaker(timeStamp: Int): String {
        val minute = timeStamp / 1000 / 60
        val second = timeStamp / 1000 % 60

        val minuteString = minute.toString()
        var secondString = second.toString()
        if (second < 10)
            secondString = "0$secondString"

        return ("$minuteString:$secondString")
    }

    override fun onPause() {
        super.onPause()

        if (isPlaying) {
            playerOriginal.pause()
            playerFiltered.pause()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isPlaying) {
            playerOriginal.start()
            playerFiltered.start()
        }
    }
}