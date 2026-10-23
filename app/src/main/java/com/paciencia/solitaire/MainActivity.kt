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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.util.Log
import androidx.core.content.ContextCompat
import com.paciencia.solitaire.ads.AdManager
import com.paciencia.solitaire.customization.Avatar
import com.paciencia.solitaire.customization.CardSkin
import com.paciencia.solitaire.data.GameEconomy
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
    private lateinit var adManager: AdManager
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

        adManager = AdManager(this).apply {
            initialize()
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
        binding.solitaireView.cardSkin = CardSkin.getById(preferences.equippedSkinId)
        binding.solitaireView.avatar = Avatar.getById(preferences.equippedAvatarId)
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

        // Botão Loja
        binding.btnShop.setOnClickListener {
            showShopDialog(0)
        }

        // Badge do Avatar (Canto inferior esquerdo)
        binding.playerAvatarBadge.setOnClickListener {
            showShopDialog(1)
        }

        // Saldo de Moedas no HUD Superior
        binding.btnCoinsDisplay.setOnClickListener {
            showShopDialog(0)
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
        binding.tvCoins.text = preferences.coinsBalance.toString()

        val equippedAvatar = Avatar.getById(preferences.equippedAvatarId)
        binding.tvAvatarIcon.text = equippedAvatar.iconEmoji
        binding.tvAvatarName.text = equippedAvatar.name

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

        // Premiação segura em moedas: garante concessão única por nível concluído
        val coinsEarned = if (!preferences.isLevelVictoryRewarded(completedLevel)) {
            preferences.addCoins(GameEconomy.COINS_PER_LEVEL_VICTORY)
            preferences.markLevelVictoryRewarded(completedLevel)
            GameEconomy.COINS_PER_LEVEL_VICTORY
        } else {
            0
        }

        // Se destravou um novo cenário, atualiza o tema visual
        if (unlockedScenery != null) {
            binding.solitaireView.theme = SceneryManager.getTheme(unlockedScenery)
        }

        updateHud()
        showVictoryDialog(completedLevel, unlockedScenery, coinsEarned)
    }

    private fun showVictoryDialog(completedLevel: Int, newSceneryUnlocked: SceneryType?, coinsEarned: Int) {
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
        val tvCoinsReward = dialog.findViewById<TextView>(R.id.tvVictoryCoinsReward)
        val tvTotalCoins = dialog.findViewById<TextView>(R.id.tvVictoryTotalCoins)
        val tvAvatar = dialog.findViewById<TextView>(R.id.tvVictoryAvatar)
        val tvScenery = dialog.findViewById<TextView>(R.id.tvVictoryScenery)

        val btnNext = dialog.findViewById<Button>(R.id.btnNextLevel)
        val btnPlayAgain = dialog.findViewById<Button>(R.id.btnPlayAgain)
        val btnOpenShop = dialog.findViewById<Button>(R.id.btnVictoryOpenShop)

        val currentAvatar = Avatar.getById(preferences.equippedAvatarId)
        val currentScenery = preferences.selectedScenery

        tvMessage.text = getString(R.string.victory_message, completedLevel)
        tvMoves.text = getString(R.string.victory_stats_moves, engine.moveCount)

        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        tvTime.text = getString(R.string.victory_stats_time, String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds))

        tvCoinsReward.text = if (coinsEarned > 0) {
            getString(R.string.victory_coins_reward)
        } else {
            "Fase concluída anteriormente"
        }
        tvTotalCoins.text = getString(R.string.victory_total_coins, preferences.coinsBalance)
        tvAvatar.text = "Avatar: ${currentAvatar.iconEmoji} ${currentAvatar.name}"
        tvScenery.text = "Cenário: ${currentScenery.title}"

        if (newSceneryUnlocked != null) {
            tvUnlockedAlert.visibility = View.VISIBLE
            tvUnlockedAlert.text = getString(R.string.victory_unlocked_scenery, newSceneryUnlocked.title)
        } else {
            tvUnlockedAlert.visibility = View.GONE
        }

        btnNext.setOnClickListener {
            dialog.dismiss()
            // Exibe anúncio intersticial após vitória e antes de iniciar o próximo nível
            adManager.showInterstitial(this) {
                startNewLevel(completedLevel + 1)
            }
        }

        btnPlayAgain.setOnClickListener {
            dialog.dismiss()
            startNewLevel(completedLevel)
        }

        btnOpenShop.setOnClickListener {
            dialog.dismiss()
            showShopDialog(0)
        }

        dialog.show()
    }

    private fun startNewLevel(level: Int) {
        engine.startNewGame(level)
        elapsedSeconds = 0
        stats.recordGameStarted()
        binding.solitaireView.theme = SceneryManager.getTheme(preferences.selectedScenery)
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

    private fun showShopDialog(initialTab: Int = 0) {
        if (isFinishing || isDestroyed) return
        activeDialog?.dismiss()

        val dialog = Dialog(this)
        activeDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_shop)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvBalance = dialog.findViewById<TextView>(R.id.tvShopCoinsBalance)
        val btnRewarded = dialog.findViewById<Button>(R.id.btnWatchRewardedAd)
        val tabSkins = dialog.findViewById<TextView>(R.id.tabSkins)
        val tabAvatars = dialog.findViewById<TextView>(R.id.tabAvatars)
        val layoutItems = dialog.findViewById<LinearLayout>(R.id.layoutShopItems)
        val btnClose = dialog.findViewById<Button>(R.id.btnCloseShop)

        val previewView = dialog.findViewById<com.paciencia.solitaire.ui.SkinPreviewView>(R.id.shopSkinPreviewView)
        val tvPreviewCollection = dialog.findViewById<TextView>(R.id.tvPreviewCollection)
        val tvPreviewTitle = dialog.findViewById<TextView>(R.id.tvPreviewTitle)
        val tvPreviewDesc = dialog.findViewById<TextView>(R.id.tvPreviewDesc)

        var currentTab = initialTab // 0 = Skins, 1 = Avatares

        fun showSkinPreview(skin: CardSkin) {
            previewView?.cardSkin = skin
            tvPreviewCollection?.text = "COLEÇÃO: ${skin.collection.uppercase(Locale.ROOT)}"
            tvPreviewTitle?.text = skin.name
            tvPreviewDesc?.text = skin.description
        }

        fun showAvatarPreview(avatar: Avatar) {
            previewView?.avatar = avatar
            tvPreviewCollection?.text = "COLEÇÃO: ${avatar.collection.uppercase(Locale.ROOT)}"
            tvPreviewTitle?.text = "${avatar.name} (${avatar.iconEmoji})"
            tvPreviewDesc?.text = "${avatar.title} • ${avatar.description}"
        }

        fun refreshBalance() {
            tvBalance.text = getString(R.string.coins_label, preferences.coinsBalance)
            updateHud()
        }

        fun populateList() {
            layoutItems.removeAllViews()
            val colorSelectedBg = ContextCompat.getColor(this, R.color.primary)
            val colorUnselectedBg = Color.TRANSPARENT
            val colorSelectedText = ContextCompat.getColor(this, R.color.white)
            val colorUnselectedText = ContextCompat.getColor(this, R.color.hud_text_secondary)

            if (currentTab == 0) {
                tabSkins.setBackgroundColor(colorSelectedBg)
                tabSkins.setTextColor(colorSelectedText)
                tabAvatars.setBackgroundColor(colorUnselectedBg)
                tabAvatars.setTextColor(colorUnselectedText)

                val equippedSkin = CardSkin.getById(preferences.equippedSkinId)
                showSkinPreview(equippedSkin)

                val equippedSkinId = preferences.equippedSkinId
                for (skin in CardSkin.ALL_SKINS) {
                    val row = layoutInflater.inflate(R.layout.item_shop_entry, layoutItems, false)
                    val tvIcon = row.findViewById<TextView>(R.id.tvShopItemIcon)
                    val tvCollection = row.findViewById<TextView>(R.id.tvShopItemCollection)
                    val tvName = row.findViewById<TextView>(R.id.tvShopItemName)
                    val tvDesc = row.findViewById<TextView>(R.id.tvShopItemDesc)
                    val btnPreview = row.findViewById<Button>(R.id.btnShopItemPreview)
                    val btnAction = row.findViewById<Button>(R.id.btnShopItemAction)

                    tvIcon.text = "🃏"
                    tvCollection.text = "COLEÇÃO: ${skin.collection.uppercase(Locale.ROOT)}"
                    tvName.text = skin.name
                    tvDesc.text = skin.description

                    // Prévia ampliada ao tocar no botão de olho ou na linha
                    btnPreview.setOnClickListener {
                        showSkinPreview(skin)
                    }
                    row.setOnClickListener {
                        showSkinPreview(skin)
                    }

                    val isEquipped = skin.id == equippedSkinId
                    val isUnlocked = preferences.isSkinUnlocked(skin.id)

                    when {
                        isEquipped -> {
                            btnAction.text = getString(R.string.shop_item_equipped)
                            btnAction.isEnabled = false
                            btnAction.setBackgroundResource(R.drawable.button_primary)
                            btnAction.setTextColor(ContextCompat.getColor(this, R.color.hud_text_secondary))
                        }
                        isUnlocked -> {
                            btnAction.text = getString(R.string.shop_item_equip)
                            btnAction.isEnabled = true
                            btnAction.setBackgroundResource(R.drawable.button_primary)
                            btnAction.setTextColor(ContextCompat.getColor(this, R.color.white))
                            btnAction.setOnClickListener {
                                preferences.equippedSkinId = skin.id
                                binding.solitaireView.cardSkin = skin
                                soundFeedback.cardPlaced(binding.solitaireView)
                                showSkinPreview(skin)
                                populateList()
                            }
                        }
                        else -> {
                            btnAction.text = getString(R.string.shop_item_buy, skin.price)
                            btnAction.isEnabled = true
                            btnAction.setBackgroundResource(R.drawable.button_gold)
                            btnAction.setTextColor(ContextCompat.getColor(this, R.color.black))
                            btnAction.setOnClickListener {
                                if (preferences.hasEnoughCoins(skin.price)) {
                                    preferences.spendCoins(skin.price)
                                    preferences.unlockSkin(skin.id)
                                    preferences.equippedSkinId = skin.id
                                    binding.solitaireView.cardSkin = skin
                                    soundFeedback.victoryCelebration()
                                    showSkinPreview(skin)
                                    Toast.makeText(this, "✨ NOVO ITEM DESCOBERTO: ${skin.name}! ✨", Toast.LENGTH_SHORT).show()
                                    refreshBalance()
                                    populateList()
                                } else {
                                    val missing = skin.price - preferences.coinsBalance
                                    soundFeedback.invalidMove(binding.solitaireView)
                                    Toast.makeText(this, getString(R.string.shop_insufficient_coins, missing), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    layoutItems.addView(row)
                }
            } else {
                tabAvatars.setBackgroundColor(colorSelectedBg)
                tabAvatars.setTextColor(colorSelectedText)
                tabSkins.setBackgroundColor(colorUnselectedBg)
                tabSkins.setTextColor(colorUnselectedText)

                val equippedAvatar = Avatar.getById(preferences.equippedAvatarId)
                showAvatarPreview(equippedAvatar)

                val equippedAvatarId = preferences.equippedAvatarId
                for (avatar in Avatar.ALL_AVATARS) {
                    val row = layoutInflater.inflate(R.layout.item_shop_entry, layoutItems, false)
                    val tvIcon = row.findViewById<TextView>(R.id.tvShopItemIcon)
                    val tvCollection = row.findViewById<TextView>(R.id.tvShopItemCollection)
                    val tvName = row.findViewById<TextView>(R.id.tvShopItemName)
                    val tvDesc = row.findViewById<TextView>(R.id.tvShopItemDesc)
                    val btnPreview = row.findViewById<Button>(R.id.btnShopItemPreview)
                    val btnAction = row.findViewById<Button>(R.id.btnShopItemAction)

                    tvIcon.text = avatar.iconEmoji
                    tvCollection.text = "COLEÇÃO: ${avatar.collection.uppercase(Locale.ROOT)}"
                    tvName.text = avatar.name
                    tvDesc.text = "${avatar.title} • ${avatar.description}"

                    btnPreview.setOnClickListener {
                        showAvatarPreview(avatar)
                    }
                    row.setOnClickListener {
                        showAvatarPreview(avatar)
                    }

                    val isEquipped = avatar.id == equippedAvatarId
                    val isUnlocked = preferences.isAvatarUnlocked(avatar.id)

                    when {
                        isEquipped -> {
                            btnAction.text = getString(R.string.shop_item_equipped)
                            btnAction.isEnabled = false
                            btnAction.setBackgroundResource(R.drawable.button_primary)
                            btnAction.setTextColor(ContextCompat.getColor(this, R.color.hud_text_secondary))
                        }
                        isUnlocked -> {
                            btnAction.text = getString(R.string.shop_item_equip)
                            btnAction.isEnabled = true
                            btnAction.setBackgroundResource(R.drawable.button_primary)
                            btnAction.setTextColor(ContextCompat.getColor(this, R.color.white))
                            btnAction.setOnClickListener {
                                preferences.equippedAvatarId = avatar.id
                                binding.solitaireView.avatar = avatar
                                updateHud()
                                soundFeedback.cardPlaced(binding.solitaireView)
                                showAvatarPreview(avatar)
                                populateList()
                            }
                        }
                        else -> {
                            btnAction.text = getString(R.string.shop_item_buy, avatar.price)
                            btnAction.isEnabled = true
                            btnAction.setBackgroundResource(R.drawable.button_gold)
                            btnAction.setTextColor(ContextCompat.getColor(this, R.color.black))
                            btnAction.setOnClickListener {
                                if (preferences.hasEnoughCoins(avatar.price)) {
                                    preferences.spendCoins(avatar.price)
                                    preferences.unlockAvatar(avatar.id)
                                    preferences.equippedAvatarId = avatar.id
                                    binding.solitaireView.avatar = avatar
                                    soundFeedback.victoryCelebration()
                                    showAvatarPreview(avatar)
                                    Toast.makeText(this, "✨ NOVO AVATAR DESCOBERTO: ${avatar.name}! ✨", Toast.LENGTH_SHORT).show()
                                    refreshBalance()
                                    populateList()
                                } else {
                                    val missing = avatar.price - preferences.coinsBalance
                                    soundFeedback.invalidMove(binding.solitaireView)
                                    Toast.makeText(this, getString(R.string.shop_insufficient_coins, missing), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    layoutItems.addView(row)
                }
            }
        }

        tabSkins.setOnClickListener {
            currentTab = 0
            populateList()
        }

        tabAvatars.setOnClickListener {
            currentTab = 1
            populateList()
        }

        btnRewarded.setOnClickListener {
            // Anúncio em vídeo recompensado oficial Google AdMob
            val started = adManager.showRewardedAd(
                activity = this,
                onUserEarnedReward = { coinsAmount ->
                    preferences.addCoins(GameEconomy.COINS_PER_REWARDED_AD)
                    refreshBalance()
                    soundFeedback.victoryCelebration()
                    Toast.makeText(this, R.string.ad_reward_success, Toast.LENGTH_SHORT).show()
                    populateList()
                },
                onAdClosed = { rewardEarned ->
                    Log.d("MainActivity", "Vídeo recompensado encerrado. Recompensa: $rewardEarned")
                }
            )
            if (!started) {
                Toast.makeText(this, R.string.ad_not_available, Toast.LENGTH_SHORT).show()
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        refreshBalance()
        populateList()
        dialog.show()
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
