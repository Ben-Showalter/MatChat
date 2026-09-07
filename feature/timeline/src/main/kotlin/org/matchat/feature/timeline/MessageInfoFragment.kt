package org.matchat.feature.timeline

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.matchat.core.model.UserId
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.nav.Navigator
import org.matchat.core.ui.softkey.SoftkeyFragment
import java.text.DateFormat
import java.util.Date

/** S11 Message info: read-only metadata for one event, with a link to the sender's
 *  profile. All data comes from nav args, so no ViewModel is needed. */
class MessageInfoFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_room_info
    override val leftLabel: CharSequence get() = ""
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val navigator: Navigator get() = requireActivity() as Navigator
    private var list: RecyclerView? = null

    override fun onContentViewCreated(content: View) {
        val args = requireArguments()
        val eventId = args.getString("eventId").orEmpty()
        val senderId = args.getString("senderId").orEmpty()
        val timestamp = args.getLong("timestamp")

        setTitle(getString(R.string.msginfo_title))
        val adapter = RoomInfoAdapter(
            onFieldActivated = {},
            onMemberActivated = {},
            onActionActivated = { navigator.toProfile(UserId(senderId)) },
        )
        val rv = content.findViewById<RecyclerView>(R.id.roominfo_list)
        list = rv
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        adapter.submitList(
            listOf(
                RoomInfoRow.Info(getString(R.string.msginfo_sent_by), senderId),
                RoomInfoRow.Info(getString(R.string.msginfo_sent), formatFull(timestamp)),
                RoomInfoRow.Info(getString(R.string.msginfo_event_id), eventId),
                RoomInfoRow.Action(ACTION_PROFILE, getString(R.string.msginfo_view_profile)),
            ),
        )
        FocusEngine.requestInitialFocus(rv)
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
