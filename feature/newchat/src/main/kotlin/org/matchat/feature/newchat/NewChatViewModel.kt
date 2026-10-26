package org.matchat.feature.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matchat.core.contacts.ContactsRepository
import org.matchat.core.matrix.MatrixSession
import org.matchat.core.model.Contact
import org.matchat.core.model.UserId
import javax.inject.Inject

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val session: MatrixSession,
    private val contacts: ContactsRepository,
) : ViewModel() {

    private val navChannel = Channel<NewChatNav>(Channel.BUFFERED)
    val navEvents: Flow<NewChatNav> = navChannel.receiveAsFlow()

    private val focused = MutableStateFlow<UserId?>(null)

    // The contact picked for "Add to a phone contact…" while the phone's
    // contact picker is open (its answer carries only the picked contact).
    private var pendingPhoneMerge: UserId? = null

    val state: StateFlow<NewChatState> =
        combine(contacts.contacts, contacts.recents, focused) { people, recents, focusedAddress ->
            val rows = people.map { it.toRow() }
            NewChatState(
                contacts = rows,
                recents = recents.map { it.toRow() },
                isLoading = false,
                focusedContact = rows.firstOrNull { it.address == focusedAddress },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NewChatState())

    fun onAction(action: NewChatAction) {
        when (action) {
            is NewChatAction.Select -> startChat(action.address)
            NewChatAction.TypeAnAddress -> emit(NewChatNav.TypeAddress)
            is NewChatAction.Focused -> focused.value = action.address
            is NewChatAction.AddToPhoneContact -> addToPhoneContact(action.address)
            is NewChatAction.RemoveContact -> removeContact(action.address)
            is NewChatAction.PhoneContactPicked -> onPhoneContactPicked(action.contactUri)
            NewChatAction.PhoneContactsUnavailable -> {
                pendingPhoneMerge = null
                emit(NewChatNav.Message(NewChatMessage.PHONE_CONTACTS_UNAVAILABLE))
            }
        }
    }

    private fun startChat(address: UserId) {
        viewModelScope.launch {
            session.startDirectChat(address).getOrNull()?.let { emit(NewChatNav.OpenRoom(it)) }
        }
    }

    /** The phone's own picker when MatChat may write contacts, else the
     *  phone's "add to existing contact" screen (UX-SPEC S20). */
    private fun addToPhoneContact(address: UserId) {
        if (contacts.canUsePhoneContacts()) {
            pendingPhoneMerge = address
            emit(NewChatNav.PickPhoneContact)
        } else {
            emit(NewChatNav.EditPhoneContact(address.value))
        }
    }

    private fun onPhoneContactPicked(contactUri: String?) {
        val address = pendingPhoneMerge ?: return
        pendingPhoneMerge = null
        if (contactUri == null) return
        viewModelScope.launch {
            val merged = contacts.mergeIntoPhoneContact(contactUri, address)
            emit(NewChatNav.Message(if (merged) NewChatMessage.PHONE_MERGED else NewChatMessage.PHONE_MERGE_FAILED))
        }
    }

    private fun removeContact(address: UserId) {
        viewModelScope.launch {
            contacts.remove(address)
            if (focused.value == address) focused.value = null
            emit(NewChatNav.Message(NewChatMessage.CONTACT_REMOVED))
        }
    }

    private fun emit(nav: NewChatNav) {
        viewModelScope.launch { navChannel.send(nav) }
    }

    private fun Contact.toRow() = ContactRow(
        address = address,
        primary = name ?: address.value,
        secondary = if (name != null) address.value else "",
        isRemovable = source == Contact.Source.LOCAL,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
