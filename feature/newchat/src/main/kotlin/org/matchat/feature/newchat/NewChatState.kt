package org.matchat.feature.newchat

import org.matchat.core.model.UserId

data class ContactRow(
    val address: UserId,
    val primary: String,
    val secondary: String,
    /** A saved contact (not admin-pushed): Options offers "Remove contact". */
    val isRemovable: Boolean = false,
)

/** S20 New message: contacts, recents, and a final "Type an address" row.
 *  No search box — the two lists are short by construction (AGENTS.md §0). */
data class NewChatState(
    val contacts: List<ContactRow> = emptyList(),
    val recents: List<ContactRow> = emptyList(),
    val isLoading: Boolean = true,
    /** The contact row with focus, or null (a recent, "Type an address"):
     *  Options is offered only while this is set (UX-SPEC S20). */
    val focusedContact: ContactRow? = null,
) {
    val hasSavedContacts: Boolean get() = contacts.isNotEmpty() || recents.isNotEmpty()
    val hasOptions: Boolean get() = focusedContact != null
}
