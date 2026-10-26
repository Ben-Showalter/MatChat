package org.matchat.feature.newchat

import org.matchat.core.model.RoomId
import org.matchat.core.model.UserId

sealed interface NewChatAction {
    data class Select(val address: UserId) : NewChatAction
    data object TypeAnAddress : NewChatAction

    /** Focus moved to a contact row ([address]) or off the contacts (null). */
    data class Focused(val address: UserId?) : NewChatAction

    /** Options > "Add to a phone contact…" on the focused contact. */
    data class AddToPhoneContact(val address: UserId) : NewChatAction

    /** Options > "Remove contact" on the focused contact. */
    data class RemoveContact(val address: UserId) : NewChatAction

    /** The phone's contact picker answered: [contactUri] null = backed out. */
    data class PhoneContactPicked(val contactUri: String?) : NewChatAction

    /** No Contacts app could open the picker or editor. */
    data object PhoneContactsUnavailable : NewChatAction
}

sealed interface NewChatNav {
    data class OpenRoom(val roomId: RoomId) : NewChatNav
    data object TypeAddress : NewChatNav

    /** Open the phone's own contact picker (contacts permission granted). */
    data object PickPhoneContact : NewChatNav

    /** Open the phone's "add to existing contact" screen for [address]. */
    data class EditPhoneContact(val address: String) : NewChatNav

    /** A short confirmation or failure line. */
    data class Message(val key: NewChatMessage) : NewChatNav
}

enum class NewChatMessage { CONTACT_REMOVED, PHONE_MERGED, PHONE_MERGE_FAILED, PHONE_CONTACTS_UNAVAILABLE }
