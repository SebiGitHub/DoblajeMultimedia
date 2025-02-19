package com.example.dobalejemultimedia

import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import java.io.IOException

class RecordingActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputAudioPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

        playerView = findViewById(R.id.playerView)
        val btnRecord: Button = findViewById(R.id.btnRecord)

        // Recibimos el ID del vídeo seleccionado
        val videoResId = intent.getIntExtra("VIDEO_RES_ID", -1)
        if (videoResId == -1) finish() // Error si no se pasó el vídeo

        // Configuramos ExoPlayer para reproducir el vídeo
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        val videoUri = Uri.parse("android.resource://$packageName/$videoResId")
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        btnRecord.setOnClickListener {
            if (!isRecording) {
                startRecording()
                btnRecord.text = "Stop Recording"
            } else {
                stopRecording()
                btnRecord.text = "Start Recording"
                // Aquí podrías llamar a una función para unir (mux) el audio grabado y el vídeo.
                // Ejemplo: muxAudioAndVideo(videoFilePath, outputAudioPath, outputMergedPath)
            }
        }
    }

    private fun startRecording() {
        // Define la ruta para guardar el audio grabado
        outputAudioPath = getExternalFilesDir(null)?.absolutePath + "/recorded_audio.mp4"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputAudioPath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        isRecording = true
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    // Función de ejemplo (placeholder) para unir audio y vídeo con MediaMuxer.
    // La implementación real requiere extraer pistas de audio y vídeo y sincronizarlas.
    /*
    private fun muxAudioAndVideo(videoFilePath: String, audioFilePath: String, outputFilePath: String) {
        // Implementación con MediaMuxer...
    }
    */
}
