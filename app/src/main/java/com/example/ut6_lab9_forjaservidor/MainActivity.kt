package com.example.ut6_lab9_forjaservidor

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.view.View

class MainActivity : AppCompatActivity() {

    // motor de vídeo y motor de alarma
    private var player: ExoPlayer? = null
    private var alarma: MediaPlayer? = null

    private lateinit var spinnerCanales: Spinner
    private lateinit var playerView: PlayerView
    private lateinit var tvEstado: TextView

    // evita recargar el mismo canal dos veces (-1 = ninguno cargado)
    private var posicionCargada = -1

    // mapa nombre del puesto  URL de streaming .mp4
    //he puesto el mismo enlace por que estuve probando varios creyendo que era el problema
    private val canales = linkedMapOf(
        "Minas Tirith" to "https://raw.githubusercontent.com/chachatwinkel/videos_android/139b2781714da2b0bd3792b9c95481c4e3f98a87/videoplayback.mp4",
        "Isengard" to "https://raw.githubusercontent.com/chachatwinkel/videos_android/a93dcedc38ff02810007d202566179b6708b3e13/Isengard.mp4",
        "Rivendell" to "https://raw.githubusercontent.com/chachatwinkel/videos_android/a93dcedc38ff02810007d202566179b6708b3e13/Rivendell.mp4"
    )

    // listener: ExoPlayer avisa aquí de cada cambio de estado (se ejecuta en el hilo principal)
    private val listenerEstado = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> tvEstado.text = "Cargando Búfer..."
                Player.STATE_READY -> tvEstado.text = "Transmitiendo en vivo"
                Player.STATE_ENDED -> {
                    tvEstado.text = "Conexión terminada"
                    sonarAlarma() // disparo acústico en paralelo
                }
            }
        }

        
        // si la señal se corta por error de red, se trata igual que un final
        override fun onPlayerError(error: PlaybackException) {
            tvEstado.text = "Conexión terminada"
            sonarAlarma()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laboratorio3)


        // vinculamos las vistas del XML
        spinnerCanales = findViewById(R.id.spinnerCanales)
        playerView = findViewById(R.id.playerViewCanal)
        tvEstado = findViewById(R.id.tvEstadoConexion)

        //arrayAdapter con los nombres de los puestos para poblar el Spinner
        val nombres = canales.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCanales.adapter = adapter

        // al cambiar la seleccion, cargamos el canal nuevo
        spinnerCanales.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                cargarCanal(position)
            }
            

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
    }


    private fun initializePlayer() {
        // instanciamos ExoPlayer, lo acoplamos a la vista y le ponemos el listener
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            exoPlayer.addListener(listenerEstado)
        }
        // Cargamos el canal que esté seleccionado en el Spinner
        cargarCanal(spinnerCanales.selectedItemPosition)
    }

    private fun cargarCanal(posicion: Int) {
        val exoPlayer = player ?: return          // si aún no hay reproductor, no hacemos nada
        if (posicion == posicionCargada) return   // ya esta cargado ese canal
        posicionCargada = posicion

        val url = canales.values.elementAt(posicion)
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun sonarAlarma() {
        // liberamos una alarma anterior si la hubiera y creamos una nueva
        alarma?.release()
        alarma = MediaPlayer.create(this, R.raw.campanas_gondor)
        alarma?.start()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        posicionCargada = -1
    }

    // ciclo de vida

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        if (player == null) {
            initializePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        alarma?.release()   // liberamos también el MediaPlayer 
        alarma = null
    }
}