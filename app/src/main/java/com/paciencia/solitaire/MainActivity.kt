package com.paciencia.solitaire

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.util.Log
import androidx.core.content.ContextCompat
import com.paciencia.solitaire.data.GamePreferences
import com.paciencia.solitaire.data.GameStateSerializer
import com.paciencia.solitaire.data.GameStats
import com.paciencia.solitaire.databinding.ActivityMainBinding
import com.paciencia.solitaire.engine.KlondikeEngine
import com.paciencia.solitaire.scenery.SceneryManager
import com.paciencia.solitaire.scenery.SceneryType
import com.paciencia.solitaire.ui.SoundFeedback
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: KlondikeEngine
    private lateinit var preferences: GamePreferences
    private lateinit var stats: GameStats
    private lateinit var soundFeedback: SoundFeedback
    private var activeDialog: Dialog? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var isTimerRunning = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                elapsedSeconds++
                updateTimerDisplay()
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = GamePreferences(this)
        stats = GameStats(this)
        soundFeedback = SoundFeedback(this).apply {
            isEnabled = preferences.isHapticsEnabled
        }

        engine = KlondikeEngine()
        setupSolitaireView()
        setupBottomControls()

        // Restaura partida salva anterior ou inicia nova partida do nível atual
        val savedTime = GameStateSerializer.loadGameState(this, engine)
        if (savedTime != null) {
            elapsedSeconds = savedTime
        } else {
            engine.startNewGame(preferences.currentLevel)
            elapsedSeconds = 0
            stats.recordGameStarted()
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("PacienciaSolitaire", "Excecao nao tratada na thread ${thread.name}", throwable)
        }

        updateHud()
        startTimer()
    }

    private fun setupSolitaireView() {
        binding.solitaireView.engine = engine
        binding.solitaireView.soundFeedback = soundFeedback
        binding.solitaireView.theme = SceneryManager.getTheme(preferences.selectedScenery)
        binding.solitaireView.isParticlesEnabled = preferences.isParticlesEnabled
        binding.solitaireView.isPowerSaveMode = preferences.isPowerSaveMode

        binding.solitaireView.onMoveMade = {
            updateHud()
            checkAutoFinish()
        }

        binding.solitaireView.onVictory = {
            handleVictory()
        }
    }

    private fun setupBottomControls() {
        // Botão Desfazer
        binding.btnUndo.setOnClickListener {
            val didUndo = engine.undo()
            if (didUndo) {
                soundFeedback.cardTouch(binding.solitaireView)
                updateHud()
                binding.solitaireView.invalidate()
            }
        }

        // Botão Reiniciar
        binding.btnRestart.setOnClickListener {
            showRestartConfirmation()
        }

        // Botão Cenários
        binding.btnScenery.setOnClickListener {
            showScenerySelectDialog()
        }

        // Botão Configurações
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun startTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true
            timerHandler.postDelayed(timerRunnable, 1000)
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateHud() {
        binding.tvLevel.text = getString(R.string.level_label, engine.currentLevel)
        binding.tvMoves.text = getString(R.string.moves_label, engine.moveCount)
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        binding.tvTime.text = getString(R.string.time_label, formattedTime)
    }

    private fun checkAutoFinish() {
        if (preferences.isAutoFinishEnabled && engine.canAutoComplete() && !engine.isWon) {
            timerHandler.postDelayed({
                if (isFinishing || isDestroyed) return@postDelayed
                if (engine.stepAutoComplete()) {
                    soundFeedback.cardPlaced(binding.solitaireView)
                    updateHud()
                    binding.solitaireView.invalidate()
                    if (engine.isWon) {
                        handleVictory()
                    } else {
                        checkAutoFinish()
                    }
                }
            }, 120)
        }
    }

    private fun handleVictory() {
        stopTimer()
        stats.recordGameWon(elapsedSeconds, engine.moveCount)
        GameStateSerializer.clearSavedGameState(this)

        val completedLevel = engine.currentLevel
        val unlockedScenery = preferences.onLevelCompleted(completedLevel)

        // Se destravou um novo cenário, atualiza o tema visual
        if (unlockedScenery != null) {
            binding.solitaireView.theme = SceneryManager.getTheme(unlockedScenery)
        }

        showVictoryDialog(completedLevel, unlockedScenery)
    }

    private fun showVictoryDialog(completedLevel: Int, newSceneryUnlocked: SceneryType?) {
        if (isFinishing || isDestroyed) return
        activeDialog?.dismiss()

        val dialog = Dialog(this)
        activeDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_victory)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val tvMessage = dialog.findViewById<TextView>(R.id.tvVictoryMessage)
        val tvUnlockedAlert = dialog.findViewById<TextView>(R.id.tvUnlockedScenery)
        val tvMoves = dialog.findViewById<TextView>(R.id.tvVictoryMoves)
        val tvTime = dialog.findViewById<TextView>(R.id.tvVictoryTime)
        val btnNext = dialog.findViewById<Button>(R.id.btnNextLevel)
        val btnPlayAgain = dialog.findViewById<Button>(R.id.btnPlayAgain)

        tvMessage.text = getString(R.string.victory_message, completedLevel)
        tvMoves.text = getString(R.string.victory_stats_moves, engine.moveCount)

        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        tvTime.text = getString(R.string.victory_stats_time, String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds))

        if (newSceneryUnlocked != null) {
            tvUnlockedAlert.visibility = View.VISIBLE
            tvUnlockedAlert.text = getString(R.string.victory_unlocked_scenery, newSceneryUnlocked.title)
        } else {
            tvUnlockedAlert.visibility = View.GONE
        }

        btnNext.setOnClickListener {
            dialog.dismiss()
            startNewLevel(completedLevel + 1)
        }

        btnPlayAgain.setOnClickListener {
            dialog.dismiss()
            startNewLevel(completedLevel)
        }

        dialog.show()
    }

    private fun startNewLevel(level: Int) {
        engine.startNewGame(level)
        elapsedSeconds = 0
        stats.recordGameStarted()
        updateHud()
        binding.solitaireView.invalidate()
        startTimer()
    }

    private fun showRestartConfirmation() {
        if (isFinishing || isDestroyed) return
        activeDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.restart_confirm_title)
            .setMessage(R.string.restart_confirm_message)
            .setPositiveButton(R.string.restart_confirm_yes) { _, _ ->
                engine.startNewGame(engine.currentLevel)
                elapsedSeconds = 0
                updateHud()
                binding.solitaireView.invalidate()
            }
            .setNegativeButton(R.string.restart_confirm_no, null)
            .create()
        activeDialog = dialog
        dialog.show()
    }

    private fun showSettingsDialog() {
        if (isFinishing || isDestroyed) return
        activeDialog?.dismiss()

        val dialog = Dialog(this)
        activeDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_settings)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val switchParticles = dialog.findViewById<SwitchCompat>(R.id.switchParticles)
        val switchHaptics = dialog.findViewById<SwitchCompat>(R.id.switchHaptics)
        val switchPowerSave = dialog.findViewById<SwitchCompat>(R.id.switchPowerSave)
        val switchAutoFinish = dialog.findViewById<SwitchCompat>(R.id.switchAutoFinish)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseSettings)

        switchParticles.isChecked = preferences.isParticlesEnabled
        switchHaptics.isChecked = preferences.isHapticsEnabled
        switchPowerSave.isChecked = preferences.isPowerSaveMode
        switchAutoFinish.isChecked = preferences.isAutoFinishEnabled

        switchParticles.setOnCheckedChangeListener { _, isChecked ->
            preferences.isParticlesEnabled = isChecked
            binding.solitaireView.isParticlesEnabled = isChecked
            binding.solitaireView.invalidate()
        }

        switchHaptics.setOnCheckedChangeListener { _, isChecked ->
            preferences.isHapticsEnabled = isChecked
            soundFeedback.isEnabled = isChecked
        }

        switchPowerSave.setOnCheckedChangeListener { _, isChecked ->
            preferences.isPowerSaveMode = isChecked
            binding.solitaireView.isPowerSaveMode = isChecked
            binding.solitaireView.invalidate()
        }

        switchAutoFinish.setOnCheckedChangeListener { _, isChecked ->
            preferences.isAutoFinishEnabled = isChecked
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showScenerySelectDialog() {
        if (isFinishing || isDestroyed) return
        activeDialog?.dismiss()

        val dialog = Dialog(this)
        activeDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_scenery_select)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val highestLevel = preferences.highestLevelUnlocked
        val currentScenery = preferences.selectedScenery

        // Itens
        setupSceneryItem(
            dialog.findViewById(R.id.itemSceneryForest),
            dialog.findViewById(R.id.tvForestStatus),
            dialog.findViewById(R.id.ivForestCheck),
            SceneryType.ENCHANTED_FOREST,
            highestLevel,
            currentScenery,
            dialog
        )

        setupSceneryItem(
            dialog.findViewById(R.id.itemSceneryFrozen),
            dialog.findViewById(R.id.tvFrozenStatus),
            dialog.findViewById(R.id.ivFrozenCheck),
            SceneryType.FROZEN_MOUNTAINS,
            highestLevel,
            currentScenery,
            dialog
        )

        setupSceneryItem(
            dialog.findViewById(R.id.itemSceneryDesert),
            dialog.findViewById(R.id.tvDesertStatus),
            dialog.findViewById(R.id.ivDesertCheck),
            SceneryType.GOLDEN_DESERT,
            highestLevel,
            currentScenery,
            dialog
        )

        setupSceneryItem(
            dialog.findViewById(R.id.itemSceneryMedieval),
            dialog.findViewById(R.id.tvMedievalStatus),
            dialog.findViewById(R.id.ivMedievalCheck),
            SceneryType.MEDIEVAL_KINGDOM,
            highestLevel,
            currentScenery,
            dialog
        )

        setupSceneryItem(
            dialog.findViewById(R.id.itemSceneryCosmic),
            dialog.findViewById(R.id.tvCosmicStatus),
            dialog.findViewById(R.id.ivCosmicCheck),
            SceneryType.COSMIC_NIGHT,
            highestLevel,
            currentScenery,
            dialog
        )

        dialog.findViewById<Button>(R.id.btnCloseScenery).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupSceneryItem(
        itemView: View,
        statusView: TextView,
        iconView: ImageView,
        scenery: SceneryType,
        highestLevel: Int,
        selectedScenery: SceneryType,
        dialog: Dialog
    ) {
        val isUnlocked = scenery.isUnlocked(highestLevel)
        val isSelected = scenery == selectedScenery

        val colorAccent = ContextCompat.getColor(this, R.color.accent)
        val colorSecondary = ContextCompat.getColor(this, R.color.hud_text_secondary)

        if (isSelected) {
            statusView.text = getString(R.string.scenery_selected)
            statusView.setTextColor(colorAccent)
            iconView.setImageResource(R.drawable.ic_check)
            iconView.setColorFilter(colorAccent)
        } else if (isUnlocked) {
            statusView.text = getString(R.string.scenery_select)
            statusView.setTextColor(colorSecondary)
            iconView.setImageResource(R.drawable.ic_check)
            iconView.setColorFilter(colorSecondary)
        } else {
            statusView.text = getString(R.string.scenery_unlocked_at, scenery.minLevel)
            statusView.setTextColor(colorSecondary)
            iconView.setImageResource(R.drawable.ic_lock)
            iconView.setColorFilter(colorSecondary)
        }

        itemView.setOnClickListener {
            if (isUnlocked) {
                preferences.selectedScenery = scenery
                binding.solitaireView.theme = SceneryManager.getTheme(scenery)
                dialog.dismiss()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
        timerHandler.removeCallbacksAndMessages(null)
        GameStateSerializer.saveGameState(this, engine, elapsedSeconds)
    }

    override fun onResume() {
        super.onResume()
        startTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        timerHandler.removeCallbacksAndMessages(null)
        activeDialog?.let {
            if (it.isShowing) it.dismiss()
        }
        activeDialog = null
    }
}
