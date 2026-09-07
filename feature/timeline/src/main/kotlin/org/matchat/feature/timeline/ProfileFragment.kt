package org.matchat.feature.timeline

import android.view.View
import android.widget.Toast
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
import org.matchat.core.ui.softkey.SoftkeyFragment

/** S11 sender profile: name + address + Send message. */
@AndroidEntryPoint
class ProfileFragment : SoftkeyFragment() {

    override val contentLayoutId: Int = R.layout.fragment_room_info
    override val leftLabel: CharSequence get() = ""
    override val centerLabel: CharSequence get() = getString(org.matchat.core.ui.R.string.softkey_select)

    private val viewModel: ProfileViewModel by viewModels()
    private val navigator: Navigator get() = requireActivity() as Navigator
    private var list: RecyclerView? = null

    private val adapter = RoomInfoAdapter(
        onFieldActivated = {},
        onMemberActivated = {},
        onActionActivated = { if (it.key == ProfileViewModel.ACTION_MESSAGE) viewModel.message() },
    )

    override fun onContentViewCreated(content: View) {
        val rv = content.findViewById<RecyclerView>(R.id.roominfo_list)
        list = rv
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.navEvents.collect(::onNav) }
            }
        }
        FocusEngine.requestInitialFocus(rv)
    }

    private fun render(state: ProfileState) {
        setTitle(state.title.ifBlank { getString(R.string.profile_title) })
        adapter.submitList(state.rows)
    }

    private fun onNav(nav: ProfileNav) {
        when (nav) {
            is ProfileNav.OpenRoom -> navigator.toRoom(nav.roomId)
            ProfileNav.Failed ->
                Toast.makeText(requireContext(), R.string.profile_message_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        list?.adapter = null
        list = null
        super.onDestroyView()
    }
}
