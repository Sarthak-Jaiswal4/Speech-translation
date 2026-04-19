package com.assignment.ondevicetranslation.ui

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.assignment.ondevicetranslation.R
import com.assignment.ondevicetranslation.data.model.SourceLanguage
import com.assignment.ondevicetranslation.databinding.ActivityMainBinding
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var pulseAnimator: AnimatorSet? = null
    private var isSpeechMode = true

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.initializeSelectedLanguage()
        } else {
            binding.statusText.text = "Microphone permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }

        setupModeDropdown()
        setupLanguageSelector()
        setupCopyButtons()

        binding.toggleButton.setOnClickListener {
            if (hasMicPermission()) viewModel.toggleListening() else requestMicPermission()
        }
        binding.buttonSpeakEnglishTts.setOnClickListener {
            viewModel.speakTypedTextAsEnglishTranslation(binding.textInputTts.text?.toString().orEmpty())
        }
        binding.buttonSpeakSourceTts.setOnClickListener {
            viewModel.speakTypedTextInSourceLanguage(binding.textInputTts.text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.sourceText.text = state.sourceText.ifBlank { "-" }
                binding.translatedText.text = state.translatedText.ifBlank { "-" }
                binding.ttsSourceText.text = state.sourceText.ifBlank { "-" }
                binding.ttsTranslatedText.text = state.translatedText.ifBlank { "-" }
                binding.statusText.text = state.status

                binding.progressBar.setProgressCompat(
                    (state.downloadProgress * 100).roundToInt().coerceIn(0, 100), true
                )
                binding.downloadProgress.text = state.downloadStatus

                binding.toggleButton.setImageResource(
                    if (state.isListening) android.R.drawable.ic_media_pause
                    else R.drawable.ic_mic_filled
                )
                binding.toggleButton.contentDescription = if (state.isListening)
                    getString(R.string.stop_listening) else getString(R.string.start_listening)

                if (state.isListening) startPulseAnimation() else stopPulseAnimation()

                // Problem 2: Offline status badge
                renderOfflineStatus(state.offlineStatus)

                // Problem 3: Speaking visual feedback on TTS buttons
                renderSpeakingFeedback(state.isSpeaking)

                renderDownloadOrMain(state)
            }
        }

        if (hasMicPermission()) viewModel.initializeSelectedLanguage() else requestMicPermission()
    }

    // ── Problem 2: Offline Badge ───────────────────────────────
    private fun renderOfflineStatus(status: OfflineStatus) {
        when (status) {
            OfflineStatus.READY -> {
                binding.offlineStatusBadge.text = getString(R.string.offline_ready)
                binding.offlineStatusBadge.setTextColor(getColor(R.color.success))
            }
            OfflineStatus.DOWNLOADING -> {
                binding.offlineStatusBadge.text = getString(R.string.offline_downloading)
                binding.offlineStatusBadge.setTextColor(getColor(R.color.warning))
            }
            OfflineStatus.NOT_DOWNLOADED -> {
                binding.offlineStatusBadge.text = getString(R.string.offline_not_downloaded)
                binding.offlineStatusBadge.setTextColor(getColor(R.color.error))
            }
        }
    }

    // ── Problem 3: Speaking Feedback ───────────────────────────
    private fun renderSpeakingFeedback(isSpeaking: Boolean) {
        val tintColor = if (isSpeaking) getColor(R.color.secondary) else getColor(R.color.primary_surface)
        binding.buttonSpeakEnglishTts.iconTint = ColorStateList.valueOf(
            if (isSpeaking) getColor(R.color.secondary) else getColor(R.color.primary_light)
        )
        binding.buttonSpeakSourceTts.iconTint = ColorStateList.valueOf(
            if (isSpeaking) getColor(R.color.secondary) else getColor(R.color.primary_light)
        )
        if (isSpeaking) {
            binding.buttonSpeakEnglishTts.alpha = 1f
            binding.buttonSpeakSourceTts.alpha = 1f
            binding.buttonSpeakEnglishTts.animate().alpha(0.6f).setDuration(400).start()
            binding.buttonSpeakSourceTts.animate().alpha(0.6f).setDuration(400).start()
        } else {
            binding.buttonSpeakEnglishTts.animate().alpha(1f).setDuration(250).start()
            binding.buttonSpeakSourceTts.animate().alpha(1f).setDuration(250).start()
        }
    }

    // ── Problem 4: Copy to Clipboard ──────────────────────────
    private fun setupCopyButtons() {
        binding.copyTranslatedButton.setOnClickListener {
            copyToClipboard(binding.translatedText.text?.toString().orEmpty())
        }
        binding.copyTtsTranslatedButton.setOnClickListener {
            copyToClipboard(binding.ttsTranslatedText.text?.toString().orEmpty())
        }
    }

    private fun copyToClipboard(text: String) {
        if (text.isBlank() || text == "-") {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("translation", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    // ── Mode Dropdown ─────────────────────────────────────────
    private fun setupModeDropdown() {
        val modes = resources.getStringArray(R.array.mode_options)
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, modes)
        binding.modeDropdown.setAdapter(adapter)
        binding.modeDropdown.setText(modes[0], false)
        binding.modeDropdown.setOnItemClickListener { _, _, position, _ ->
            isSpeechMode = position == 0
            switchPage(isSpeechMode)
        }
    }

    private fun switchPage(showSpeech: Boolean) {
        val fadeOut = if (showSpeech) binding.textToSpeechPage else binding.speechTranslationPage
        val fadeIn = if (showSpeech) binding.speechTranslationPage else binding.textToSpeechPage
        if (fadeIn.isVisible && !fadeOut.isVisible) return
        fadeOut.animate().cancel(); fadeIn.animate().cancel()
        fadeOut.animate().alpha(0f).setDuration(150L).withEndAction {
            fadeOut.visibility = View.GONE; fadeOut.alpha = 1f
            fadeIn.alpha = 0f; fadeIn.visibility = View.VISIBLE
            fadeIn.animate().alpha(1f).setDuration(200L).start()
        }.start()
    }

    // ── Language Selector ─────────────────────────────────────
    private fun setupLanguageSelector() {
        binding.languageToggleGroup.check(binding.hindiButton.id)
        binding.languageToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val language = if (checkedId == binding.hindiButton.id) SourceLanguage.HINDI else SourceLanguage.TELUGU
            viewModel.setLanguage(language)
            viewModel.initializeSelectedLanguage()
        }
    }

    // ── Download / Main toggle ────────────────────────────────
    private fun renderDownloadOrMain(state: MainUiState) {
        val showDownload = state.status.startsWith("Downloading", ignoreCase = true) && state.downloadProgress < 1f
        if (showDownload == binding.downloadContainer.isVisible) return
        val showView = if (showDownload) binding.downloadContainer else binding.mainContentContainer
        val hideView = if (showDownload) binding.mainContentContainer else binding.downloadContainer
        hideView.animate().cancel(); showView.animate().cancel()
        hideView.animate().alpha(0f).setDuration(200L).withEndAction {
            hideView.visibility = View.GONE; hideView.alpha = 1f
            showView.alpha = 0f; showView.visibility = View.VISIBLE
            showView.animate().alpha(1f).setDuration(250L).start()
        }.start()
    }

    // ── Pulse Animation ───────────────────────────────────────
    private fun startPulseAnimation() {
        if (pulseAnimator?.isRunning == true) return
        val ring = binding.pulseRing
        val sX = ObjectAnimator.ofFloat(ring, View.SCALE_X, 0.8f, 1.3f).apply { repeatCount = ObjectAnimator.INFINITE; repeatMode = ObjectAnimator.RESTART }
        val sY = ObjectAnimator.ofFloat(ring, View.SCALE_Y, 0.8f, 1.3f).apply { repeatCount = ObjectAnimator.INFINITE; repeatMode = ObjectAnimator.RESTART }
        val a = ObjectAnimator.ofFloat(ring, View.ALPHA, 0.6f, 0f).apply { repeatCount = ObjectAnimator.INFINITE; repeatMode = ObjectAnimator.RESTART }
        pulseAnimator = AnimatorSet().apply { playTogether(sX, sY, a); duration = 1100L; interpolator = AccelerateDecelerateInterpolator(); start() }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel(); pulseAnimator = null
        binding.pulseRing.alpha = 0f; binding.pulseRing.scaleX = 1f; binding.pulseRing.scaleY = 1f
    }

    // ── Permissions ───────────────────────────────────────────
    private fun hasMicPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun requestMicPermission() = permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

    override fun onDestroy() { pulseAnimator?.cancel(); super.onDestroy() }
}
