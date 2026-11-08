package org.matchat.feature.scan

import android.Manifest
import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.menu.MenuItem
import org.matchat.core.ui.menu.MenuSheet
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.feature.scan.databinding.FragmentSetupHelpBinding
import org.matchat.core.ui.R as UiR

/** S33 "Make a setup code" (UX-SPEC §S33): how to make a setup code on a
 *  computer, and getting the offline setup page there — saved to Download,
 *  to the SD card, or sent by Bluetooth (Options). */
@AndroidEntryPoint
class SetupHelpFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_setup_help
    override val leftLabel: CharSequence get() = getString(UiR.string.softkey_options)
    override val centerLabel: CharSequence get() = getString(UiR.string.softkey_select)

    private val viewModel: SetupHelpViewModel by viewModels()
    private var binding: FragmentSetupHelpBinding? = null

    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onAction(SetupHelpAction.StoragePermissionResult(it))
    }

    override fun onContentViewCreated(content: View) {
        val b = FragmentSetupHelpBinding.bind(content)
        binding = b
        setTitle(getString(R.string.setup_help_title))
        b.setupHelpSave.setOnClickListener { viewModel.onAction(SetupHelpAction.SaveToPhone) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch {
                    viewModel.navEvents.collect {
                        when (it) {
                            SetupHelpNav.RequestStoragePermission ->
                                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }
            }
        }
        FocusEngine.requestInitialFocus(b.setupHelpSave)
    }

    private fun render(state: SetupHelpState) {
        val b = binding ?: return
        b.setupHelpMessage.isVisible = state.message != null
        b.setupHelpMessage.text = when (val m = state.message) {
            is SetupHelpMessage.Saved -> getString(R.string.setup_help_saved, m.where)
            SetupHelpMessage.SaveFailed -> getString(R.string.setup_help_save_failed)
            SetupHelpMessage.NoSdCard -> getString(R.string.setup_help_no_sd)
            SetupHelpMessage.NoShare -> getString(R.string.setup_help_no_share)
            SetupHelpMessage.NeedsStorage -> getString(R.string.setup_help_needs_storage)
            null -> ""
        }
    }

    override fun onOptions(): Boolean {
        val items = listOf(
            MenuItem(OPTION_SD, getString(R.string.setup_help_save_sd)),
            MenuItem(OPTION_BLUETOOTH, getString(R.string.setup_help_send_bluetooth)),
        )
        MenuSheet.show(requireContext(), items) { item ->
            when (item.id) {
                OPTION_SD -> viewModel.onAction(SetupHelpAction.SaveToSdCard)
                OPTION_BLUETOOTH -> sendByBluetooth()
            }
        }
        return true
    }

    /** Bluetooth first; the share sheet if this phone's Bluetooth app is named differently. */
    private fun sendByBluetooth() {
        val ctx = requireContext()
        val sent = listOf(true, false).any { bluetoothOnly ->
            val intent = AndroidSetupPageStore.shareIntent(ctx, bluetoothOnly) ?: return@any false
            val target = if (bluetoothOnly) intent else Intent.createChooser(intent, null)
            runCatching { startActivity(target) }.isSuccess
        }
        if (!sent) viewModel.onAction(SetupHelpAction.ShareFailed)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private companion object {
        const val OPTION_SD = "sd"
        const val OPTION_BLUETOOTH = "bluetooth"
    }
}
