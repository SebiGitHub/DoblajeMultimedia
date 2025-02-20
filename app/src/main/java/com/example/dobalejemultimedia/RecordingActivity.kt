package com.example.dobalejemultimedia

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class RecordingActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputAudioPath: String = ""
    private var recordingStartTimeUs: Long = 0
    private var recordingEndTimeUs: Long = 0
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)

        playerView = findViewById(R.id.playerView)
        val btnRecord: Button = findViewById(R.id.btnRecord)

        // Recibimos el ID del vídeo seleccionado (guardado en raw)
        val videoResId = intent.getIntExtra("VIDEO_RES_ID", -1)
        if (videoResId == -1) finish() // Finalizamos si no se pasó el vídeo

        // Copiamos el vídeo de raw a un fichero accesible
        val videoFilePath = copyRawVideoToFile(videoResId)

        // Configuramos ExoPlayer para reproducir el vídeo
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        val videoUri = Uri.parse("android.resource://$packageName/$videoResId")
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        // Dejar el vídeo en pausa y sin volumen
        player.pause()
        player.volume = 0f

        // Verificamos permisos (RECORD_AUDIO y WRITE_EXTERNAL_STORAGE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }

        btnRecord.setOnClickListener {
            if (!isRecording) {
                // Al iniciar, guardamos el tiempo actual del vídeo (en microsegundos)
                recordingStartTimeUs = player.currentPosition * 1000L
                // Iniciamos el vídeo y la grabación simultáneamente
                player.play()
                startRecording()
                btnRecord.text = "Stop Recording"
                isRecording = true
            } else {
                // Al detener, guardamos el tiempo final y pausamos el vídeo
                recordingEndTimeUs = player.currentPosition * 1000L
                stopRecording()
                player.pause()
                btnRecord.text = "Start Recording"
                isRecording = false
                // Obtenemos la ruta de salida en la carpeta de Descargas con nombre "doblaje_x.mp4"
                val outputMergedPath = getNextOutputFilePath()
                // Fusionamos el fragmento del vídeo con la grabación de voz
                muxAudioAndVideo(videoFilePath, outputAudioPath, outputMergedPath, recordingStartTimeUs, recordingEndTimeUs)
                Toast.makeText(this, "Recording successful!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisos concedidos, OK.
            } else {
                // Permisos no concedidos; finalizamos la actividad.
                finish()
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
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    // Copia el vídeo de raw a un fichero accesible en el almacenamiento interno de la app.
    private fun copyRawVideoToFile(videoResId: Int): String {
        val inputStream = resources.openRawResource(videoResId)
        val outputFile = File(getExternalFilesDir(null), "video_input.mp4")
        FileOutputStream(outputFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return outputFile.absolutePath
    }

    // Función que devuelve la ruta para el siguiente archivo de salida: "doblaje_x.mp4"
    private fun getNextOutputFilePath(): String {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var counter = 1
        downloadsFolder.listFiles()?.forEach { file ->
            if (file.name.startsWith("doblaje_") && file.name.endsWith(".mp4")) {
                val numPart = file.name.removePrefix("doblaje_").removeSuffix(".mp4")
                val num = numPart.toIntOrNull() ?: 0
                if (num >= counter) {
                    counter = num + 1
                }
            }
        }
        return File(downloadsFolder, "doblaje_$counter.mp4").absolutePath
    }

    @SuppressLint("WrongConstant")
    private fun muxAudioAndVideo(
        videoFilePath: String,
        audioFilePath: String,
        outputFilePath: String,
        startTimeUs: Long,
        endTimeUs: Long
    ) {
        try {
            // Configuramos el extractor para el vídeo.
            val videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(videoFilePath)
            var videoTrackIndex = -1
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    videoTrackIndex = i
                    break
                }
            }
            if (videoTrackIndex == -1) {
                throw RuntimeException("No se encontró pista de vídeo en $videoFilePath")
            }
            videoExtractor.selectTrack(videoTrackIndex)
            val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)

            // Configuramos el extractor para el audio.
            val audioExtractor = MediaExtractor()
            audioExtractor.setDataSource(audioFilePath)
            var audioTrackIndex = -1
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }
            if (audioTrackIndex == -1) {
                throw RuntimeException("No se encontró pista de audio en $audioFilePath")
            }
            audioExtractor.selectTrack(audioTrackIndex)
            val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)

            // Creamos el MediaMuxer para el fichero de salida.
            val muxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrackIndex = muxer.addTrack(videoFormat)
            val muxerAudioTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            // Buffer y objeto para la información de cada muestra.
            val bufferSize = 1024 * 1024 // 1 MB
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // Escribimos las muestras del vídeo dentro del fragmento definido.
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                val sampleTime = videoExtractor.sampleTime
                if (sampleTime < startTimeUs) {
                    videoExtractor.advance()
                    continue
                }
                if (sampleTime > endTimeUs) break
                bufferInfo.presentationTimeUs = sampleTime - startTimeUs
                bufferInfo.flags = videoExtractor.sampleFlags
                muxer.writeSampleData(muxerVideoTrackIndex, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Escribimos las muestras del audio dentro del fragmento definido.
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                val sampleTime = audioExtractor.sampleTime
                if (sampleTime < startTimeUs) {
                    audioExtractor.advance()
                    continue
                }
                if (sampleTime > endTimeUs) break
                bufferInfo.presentationTimeUs = sampleTime - startTimeUs
                bufferInfo.flags = audioExtractor.sampleFlags
                muxer.writeSampleData(muxerAudioTrackIndex, buffer, bufferInfo)
                audioExtractor.advance()
            }

            muxer.stop()
            muxer.release()
            videoExtractor.release()
            audioExtractor.release()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
