package org.matchat.feature.call

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.model.RoomId
import org.matchat.core.rtc.CallPhase
import org.matchat.core.rtc.CallSession
import org.matchat.core.ui.key.LogicalKey
import org.matchat.core.ui.nav.Navigator
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.feature.call.databinding.FragmentCallBinding

/**
 * The one voice-call screen (incoming ring + in-call), driven by the shared
 * CallController session. Non-touch: CALL answers a ringing call, END hangs up /
 * declines, CENTER toggles mute, Options routes to speaker (docs/VOICE.md §6).
 */
@AndroidEntryPoint
class CallFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_call
    override val leftLabel: CharSequence
        get() = if (viewModel.session.value.phase == CallPhase.CONNECTED) getString(R.string.call_speaker) else ""
    override val centerLabel: CharSequence
        get() = when (viewModel.session.value.phase) {
            CallPhase.RINGING -> getString(R.string.call_answer)
            CallPhase.CONNECTED -> getString(R.string.call_mute)
            else -> ""
        }

    private val viewModel: CallViewModel by viewModels()
    private val navigator: Navigator get() = requireActivity() as Navigator
    private var binding: FragmentCallBinding? = null

    override fun onContentViewCreated(content: View) {
        binding = FragmentCallBinding.bind(content)
        val args = requireArguments()
        val incoming = args.getBoolean(ARG_INCOMING, false)
        val roomId = args.getString(ARG_ROOM_ID).orEmpty()
        val peerName = args.getString(ARG_PEER_NAME)
        if (!incoming && roomId.isNotEmpty()) viewModel.placeOnce(RoomId(roomId), peerName)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.session.collect(::render)
            }
        }
    }

    private fun render(state: CallSession) {
        val b = binding ?: return
        b.callPeer.text = state.peerName ?: getString(R.string.call_unknown_peer)
        b.callStatus.text = statusText(state)
        b.callEncryption.visibility = if (state.isActive) View.VISIBLE else View.GONE
        b.callHint.text = hintText(state)
        refreshSoftkeys()
        if (state.phase == CallPhase.ENDED) navigator.back()
    }

    private fun statusText(state: CallSession): String = when (state.phase) {
        CallPhase.DIALING -> getString(R.string.call_status_dialing)
        CallPhase.RINGING -> getString(R.string.call_status_ringing)
        CallPhase.CONNECTING -> getString(R.string.call_status_connecting)
        CallPhase.CONNECTED ->
            if (state.audioAvailable) statusConnected(state) else getString(R.string.call_status_no_audio)
        CallPhase.ENDED -> getString(R.string.call_status_ended)
        CallPhase.IDLE -> ""
    }

    private fun statusConnected(state: CallSession): String =
        if (state.micMuted) getString(R.string.call_status_muted) else getString(R.string.call_status_connected)

    private fun hintText(state: CallSession): String = when (state.phase) {
        CallPhase.RINGING -> getString(R.string.call_hint_ringing)
        CallPhase.CONNECTED -> getString(R.string.call_hint_incall)
        else -> getString(R.string.call_hint_end)
    }

    override fun onOtherKey(key: LogicalKey): Boolean = when (key) {
        LogicalKey.CALL -> {
            if (viewModel.session.value.phase == CallPhase.RINGING) viewModel.answer()
            true
        }
        LogicalKey.END -> {
            viewModel.hangup()
            true
        }
        else -> false
    }

    override fun onCenter(): Boolean {
        if (viewModel.session.value.phase == CallPhase.CONNECTED) {
            viewModel.toggleMute()
            return true
        }
        return false
    }

    override fun onOptions(): Boolean {
        if (viewModel.session.value.phase == CallPhase.CONNECTED) {
            viewModel.toggleSpeaker()
            return true
        }
        return false
    }

    override fun onBack(): Boolean {
        // Back never silently leaves a live call; it hangs up.
        if (viewModel.session.value.isActive) {
            viewModel.hangup()
            return true
        }
        return super.onBack()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ROOM_ID = "roomId"
        const val ARG_PEER_NAME = "peerName"
        const val ARG_INCOMING = "incoming"
    }
}
