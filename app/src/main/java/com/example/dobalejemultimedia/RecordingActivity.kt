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

// Clase que maneja la grabación de audio sobre el vídeo a doblar ademas de la union del archivo resultante de ambos.
class RecordingActivity : AppCompatActivity() {

    // Variables para el reproductor de video y su vista
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    // Variables para la grabación de audio
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false // Indica si la grabación está en curso
    private var outputAudioPath: String = "" // Ruta donde se guardará el audio grabado
    private var recordingStartTimeUs: Long = 0 // Tiempo de inicio de la grabación en microsegundos
    private var recordingEndTimeUs: Long = 0 // Tiempo de finalización de la grabación en microsegundos
    private val PERMISSION_REQUEST_CODE = 1001 // Código para la solicitud de permisos


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording) // Asignamos el layout de la actividad

        playerView = findViewById(R.id.playerView)
        val btnRecord: Button = findViewById(R.id.btnRecord)

        // Obtenemos el ID del vídeo que se pasó como extra en el intent
        val videoResId = intent.getIntExtra("VIDEO_RES_ID", -1)
        if (videoResId == -1) finish() // Si no se pasó un ID válido, cerramos la actividad

        // Copiamos el vídeo desde la carpeta raw a un archivo accesible en almacenamiento interno
        val videoFilePath = copyRawVideoToFile(videoResId)

        // Configuramos ExoPlayer para reproducir el video
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        val videoUri = Uri.parse("android.resource://$packageName/$videoResId")
        val mediaItem = MediaItem.fromUri(videoUri)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.pause() // Dejamos el vídeo pausado por defecto
        player.volume = 0f // Silenciamos el audio del vídeo para evitar interferencias con la grabación

        // Verificamos y solicitamos permisos necesarios (grabar audio y escribir en almacenamiento)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }

        // Configuración del botón de grabación
        btnRecord.setOnClickListener {
            if (!isRecording) {
                recordingStartTimeUs = player.currentPosition * 1000L // Guardamos el tiempo actual del video en microsegundos

                player.play() // Iniciamos el video
                startRecording() // Iniciamos la grabación de audio
                btnRecord.text = "Stop Recording"
                isRecording = true
            } else {
                recordingEndTimeUs = player.currentPosition * 1000L // Guardamos el tiempo final de grabación

                stopRecording()
                player.pause() // Pausamos el video
                btnRecord.text = "Start Recording"
                isRecording = false

                val outputMergedPath = getNextOutputFilePath() // Generamos la ruta del archivo de salida
                muxAudioAndVideo(videoFilePath, outputAudioPath, outputMergedPath, recordingStartTimeUs, recordingEndTimeUs)
                Toast.makeText(this, "Recording successful!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Metodo que se llama cuando el usuario responde a una solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int, // Código de la solicitud de permisos
        permissions: Array<String>, // Lista de permisos solicitados
        grantResults: IntArray // Resultados de los permisos concedidos o denegados
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Verificamos si la respuesta corresponde a nuestra solicitud de permisos específica
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Comprobamos si se han concedido los permisos
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisos concedidos, podemos proceder con la funcionalidad que los requiere
            } else {
                // Si los permisos no son concedidos, cerramos la actividad
                finish()
            }
        }
    }

    // Metodo para iniciar la grabación de audio
    private fun startRecording() {
        // Definimos la ruta donde se almacenará el archivo de audio grabado
        outputAudioPath = getExternalFilesDir(null)?.absolutePath + "/recorded_audio.mp4"

        // Inicializamos el MediaRecorder y configuramos los parámetros para la grabación de audio
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC) // Fuente de audio: micrófono
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // Formato del archivo de salida
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // Codificación del audio
            setOutputFile(outputAudioPath) // Establecemos la ruta de salida del archivo grabado

            try {
                prepare() // Preparamos el MediaRecorder para la grabación
                start() // Iniciamos la grabación
            } catch (e: IOException) {
                e.printStackTrace() // Manejo de errores en caso de fallo en la preparación/inicio
            }
        }
    }

    // Metodo para detener la grabación de audio
    private fun stopRecording() {
        mediaRecorder?.apply {
            stop() // Detiene la grabación
            release() // Libera los recursos del MediaRecorder
        }
        mediaRecorder = null // Establecemos el MediaRecorder en null para evitar uso accidental
    }

    // Metodo que se ejecuta cuando la actividad es destruida
    override fun onDestroy() {
        super.onDestroy()
        player.release() // Libera los recursos del reproductor de medios
    }

    // Copia un archivo de video desde la carpeta raw a un archivo accesible en el almacenamiento interno de la app
    private fun copyRawVideoToFile(videoResId: Int): String {
        val inputStream = resources.openRawResource(videoResId) // Obtiene el recurso de video en raw
        val outputFile = File(getExternalFilesDir(null), "video_input.mp4") // Define la ruta de destino

        // Copiamos el contenido del archivo raw al almacenamiento interno
        FileOutputStream(outputFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        return outputFile.absolutePath // Retornamos la ruta del archivo copiado
    }

    // Genera la ruta para el siguiente archivo de salida en la carpeta de Descargas: "doblaje_x.mp4"
    private fun getNextOutputFilePath(): String {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) // Carpeta de Descargas
        var counter = 1 // Contador para asignar nombres únicos a los archivos

        // Verificamos si ya existen archivos con el prefijo "doblaje_"
        downloadsFolder.listFiles()?.forEach { file ->
            if (file.name.startsWith("doblaje_") && file.name.endsWith(".mp4")) {
                val numPart = file.name.removePrefix("doblaje_").removeSuffix(".mp4")
                val num = numPart.toIntOrNull() ?: 0
                if (num >= counter) {
                    counter = num + 1 // Incrementamos el contador si ya existe un archivo con ese número
                }
            }
        }

        // Retornamos la ruta del próximo archivo disponible
        return File(downloadsFolder, "doblaje_$counter.mp4").absolutePath
    }


    @SuppressLint("WrongConstant") // Suprime advertencias de uso incorrecto de constantes
    private fun muxAudioAndVideo(
        videoFilePath: String,  // Ruta del archivo de video
        audioFilePath: String,  // Ruta del archivo de audio
        outputFilePath: String, // Ruta del archivo de salida combinado
        startTimeUs: Long,      // Tiempo de inicio en microsegundos
        endTimeUs: Long         // Tiempo de finalización en microsegundos
    ) {
        try {
            // ---- EXTRACTOR DE VIDEO ----
            val videoExtractor = MediaExtractor() // Objeto para extraer la pista de video
            videoExtractor.setDataSource(videoFilePath) // Carga el archivo de video en el extractor
            var videoTrackIndex = -1 // Índice de la pista de video (aún no encontrado)

            // Recorremos todas las pistas del archivo de video para encontrar la de tipo "video/"
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i) // Obtiene el formato de la pista
                val mime = format.getString(MediaFormat.KEY_MIME) // Obtiene el tipo MIME de la pista
                if (mime?.startsWith("video/") == true) { // Si el MIME es de video
                    videoTrackIndex = i
                    break // Se encontró la pista de video, salimos del bucle
                }
            }

            // Si no se encontró ninguna pista de video, lanzamos un error
            if (videoTrackIndex == -1) {
                throw RuntimeException("No se encontró pista de vídeo en $videoFilePath")
            }

            videoExtractor.selectTrack(videoTrackIndex) // Seleccionamos la pista de video encontrada
            val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex) // Guardamos el formato del video

            // ---- EXTRACTOR DE AUDIO ----
            val audioExtractor = MediaExtractor() // Objeto para extraer la pista de audio
            audioExtractor.setDataSource(audioFilePath) // Carga el archivo de audio en el extractor
            var audioTrackIndex = -1 // Índice de la pista de audio (aún no encontrado)

            // Recorremos todas las pistas del archivo de audio para encontrar la de tipo "audio/"
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i) // Obtiene el formato de la pista
                val mime = format.getString(MediaFormat.KEY_MIME) // Obtiene el tipo MIME de la pista
                if (mime?.startsWith("audio/") == true) { // Si el MIME es de audio
                    audioTrackIndex = i
                    break // Se encontró la pista de audio, salimos del bucle
                }
            }

            // Si no se encontró ninguna pista de audio, lanzamos un error
            if (audioTrackIndex == -1) {
                throw RuntimeException("No se encontró pista de audio en $audioFilePath")
            }

            audioExtractor.selectTrack(audioTrackIndex) // Seleccionamos la pista de audio encontrada
            val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex) // Guardamos el formato del audio

            // ---- CONFIGURACIÓN DEL MULTIPLEXOR ----
            val muxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrackIndex = muxer.addTrack(videoFormat) // Agregamos la pista de video al multiplexor
            val muxerAudioTrackIndex = muxer.addTrack(audioFormat) // Agregamos la pista de audio al multiplexor
            muxer.start() // Iniciamos el multiplexor

            // ---- CONFIGURACIÓN DEL BUFFER ----
            val bufferSize = 1024 * 1024 // Tamaño del buffer: 1 MB
            val buffer = ByteBuffer.allocate(bufferSize) // Creamos un buffer para almacenar datos
            val bufferInfo = MediaCodec.BufferInfo() // Objeto para almacenar información sobre las muestras

            // ---- PROCESO DE ESCRITURA DE VIDEO ----
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0) // Leemos los datos del video en el buffer

                // Si no hay más datos, terminamos el bucle
                if (bufferInfo.size < 0) break

                val sampleTime = videoExtractor.sampleTime // Obtenemos el tiempo de presentación de la muestra

                // Si el tiempo de la muestra es menor al tiempo de inicio, avanzamos al siguiente
                if (sampleTime < startTimeUs) {
                    videoExtractor.advance()
                    continue
                }

                // Si la muestra está fuera del rango, terminamos
                if (sampleTime > endTimeUs) break

                bufferInfo.presentationTimeUs = sampleTime - startTimeUs // Ajustamos el tiempo de la muestra
                bufferInfo.flags = videoExtractor.sampleFlags // Guardamos las flags de la muestra
                muxer.writeSampleData(muxerVideoTrackIndex, buffer, bufferInfo) // Escribimos la muestra en el multiplexor
                videoExtractor.advance() // Avanzamos a la siguiente muestra de video
            }

            // ---- PROCESO DE ESCRITURA DE AUDIO ----
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = audioExtractor.readSampleData(buffer, 0) // Leemos los datos del audio en el buffer

                // Si no hay más datos, terminamos el bucle
                if (bufferInfo.size < 0) break

                val sampleTime = audioExtractor.sampleTime // Obtenemos el tiempo de presentación de la muestra

                // Si el tiempo de la muestra es menor al tiempo de inicio, avanzamos al siguiente
                if (sampleTime < startTimeUs) {
                    audioExtractor.advance()
                    continue
                }

                // Si la muestra está fuera del rango, terminamos
                if (sampleTime > endTimeUs) break

                bufferInfo.presentationTimeUs = sampleTime - startTimeUs // Ajustamos el tiempo de la muestra
                bufferInfo.flags = audioExtractor.sampleFlags // Guardamos las flags de la muestra
                muxer.writeSampleData(muxerAudioTrackIndex, buffer, bufferInfo) // Escribimos la muestra en el multiplexor
                audioExtractor.advance() // Avanzamos a la siguiente muestra de audio
            }

            // ---- FINALIZACIÓN DEL PROCESO ----
            muxer.stop() // Detenemos el multiplexor
            muxer.release() // Liberamos los recursos del multiplexor
            videoExtractor.release() // Liberamos el extractor de video
            audioExtractor.release() // Liberamos el extractor de audio

        } catch (e: Exception) {
            e.printStackTrace() // Capturamos y mostramos cualquier error
        }
    }

}
