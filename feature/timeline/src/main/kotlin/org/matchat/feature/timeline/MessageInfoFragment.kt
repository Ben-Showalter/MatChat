package org.matchat.feature.timeline

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.model.ReactionSummary
import org.matchat.core.model.UserId
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.nav.Navigator
import org.matchat.core.ui.softkey.SoftkeyFragment
import java.text.DateFormat
import java.util.Date

/** S11 Message info: read-only metadata for one event, with a link to the
 *  sender's profile. Sender/sent/event-id rows come straight from nav args
 *  (no live lookup needed for those); the reactions rows are live, via
 *  MessageInfoViewModel — a follow-up to the Reactions round ("who
 *  reacted", by name). */
@AndroidEntryPoint
class MessageInfoFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_room_info
    override val leftLabel: CharSequence get() = ""
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: MessageInfoViewModel by viewModels()
    private val navigator: Navigator get() = requireActivity() as Navigator
    private var list: RecyclerView? = null

    private val adapter = RoomInfoAdapter(
        onFieldActivated = {},
        onMemberActivated = {},
        onActionActivated = { navigator.toProfile(UserId(senderId)) },
    )

    private var senderId: String = ""

    override fun onContentViewCreated(content: View) {
        val args = requireArguments()
        val eventId = args.getString("eventId").orEmpty()
        senderId = args.getString("senderId").orEmpty()
        val timestamp = args.getLong("timestamp")

        setTitle(getString(R.string.msginfo_title))
        val rv = content.findViewById<RecyclerView>(R.id.roominfo_list)
        list = rv
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        adapter.submitList(rows(eventId, timestamp, emptyList()))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reactions.collect { reactions ->
                    adapter.submitList(rows(eventId, timestamp, reactions))
                }
            }
        }
        FocusEngine.requestInitialFocus(rv)
    }

    private fun rows(eventId: String, timestamp: Long, reactions: List<ReactionSummary>): List<RoomInfoRow> =
        buildList {
            add(RoomInfoRow.Info(getString(R.string.msginfo_sent_by), senderId))
            add(RoomInfoRow.Info(getString(R.string.msginfo_sent), formatFull(timestamp)))
            add(RoomInfoRow.Info(getString(R.string.msginfo_event_id), eventId))
            if (reactions.isNotEmpty()) {
                add(RoomInfoRow.Section(getString(R.string.msginfo_reactions)))
                reactions.forEach { r -> add(RoomInfoRow.Info(r.key, r.senderNames.joinToString(", "))) }
            }
            add(RoomInfoRow.Action(ACTION_PROFILE, getString(R.string.msginfo_view_profile)))
        }

    private fun formatFull(epochMs: Long): String =
        if (epochMs <= 0) "—" else DateFormat.getDateTimeInstance().format(Date(epochMs))

    override fun onDestroyView() {
        list?.adapter = null
        list = null
        super.onDestroyView()
    }

    private companion object {
        const val ACTION_PROFILE = "profile"
    }
}
