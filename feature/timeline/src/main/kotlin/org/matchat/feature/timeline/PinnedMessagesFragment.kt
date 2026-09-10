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
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.nav.Navigator

/** Room Info > Pinned messages (Pinned messages round). A read-only list of
 *  this room's pinned messages (reusing RoomInfoAdapter's Field row — see
 *  PinnedMessagesViewModel's doc comment); CENTER opens the room, since no
 *  screen in this app can jump to a specific message yet. */
@AndroidEntryPoint
class PinnedMessagesFragment : org.matchat.core.ui.softkey.SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_room_info
    override val leftLabel: CharSequence get() = ""
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: PinnedMessagesViewModel by viewModels()
    private val navigator: Navigator get() = requireActivity() as Navigator
    private var list: RecyclerView? = null

    private val adapter = RoomInfoAdapter(
        onFieldActivated = { navigator.toRoom(viewModel.roomId()) },
        onMemberActivated = {},
        onActionActivated = {},
    )

    override fun onContentViewCreated(content: View) {
        val rv = content.findViewById<RecyclerView>(R.id.roominfo_list)
        list = rv
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        setTitle(getString(R.string.pinned_messages_title))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        FocusEngine.requestInitialFocus(rv)
    }

    private fun render(state: RoomInfoState) {
        adapter.submitList(
            state.rows.ifEmpty { listOf(RoomInfoRow.Section(getString(R.string.pinned_messages_empty))) },
        )
    }

    override fun onDestroyView() {
        list?.adapter = null
        list = null
        super.onDestroyView()
    }
}
