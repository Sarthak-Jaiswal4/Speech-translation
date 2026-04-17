package com.assignment.ondevicetranslation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.assignment.ondevicetranslation.R
import com.assignment.ondevicetranslation.data.model.SourceLanguage
import com.assignment.ondevicetranslation.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLanguageSpinner()
        binding.toggleButton.setOnClickListener {
            if (hasMicPermission()) {
                viewModel.toggleListening()
            } else {
                requestMicPermission()
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.sourceText.text = state.sourceText.ifBlank { "-" }
                binding.translatedText.text = state.translatedText.ifBlank { "-" }
                binding.statusText.text = state.status
                binding.progressBar.progress = state.downloadProgress
                binding.downloadProgress.text = state.downloadStatus
                binding.toggleButton.text = if (state.isListening) {
                    getString(R.string.stop_listening)
                } else {
                    getString(R.string.start_listening)
                }
            }
        }

        if (hasMicPermission()) {
            viewModel.initializeSelectedLanguage()
        } else {
            requestMicPermission()
        }
    }

    private fun setupLanguageSpinner() {
        val languages = SourceLanguage.entries.toTypedArray()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages.map { it.displayName }
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.sourceLanguageSpinner.adapter = adapter
        binding.sourceLanguageSpinner.setSelection(0)
        binding.sourceLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setLanguage(languages[position])
                viewModel.initializeSelectedLanguage()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMicPermission() {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
