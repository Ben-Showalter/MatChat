package org.matchat.feature.newchat

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.matchat.core.model.Contact
import org.matchat.core.model.UserId
import org.matchat.core.testing.FakeContactsRepository
import org.matchat.core.testing.FakeMatrixSession

/** S20: saved contacts, Options on a focused contact, phone-contact hand-off. */
class NewChatViewModelTest {

    private val wayne = UserId("@wayne:example.org")
    private val boss = UserId("@boss:example.org")

    @BeforeEach fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun subject(contacts: FakeContactsRepository) = NewChatViewModel(FakeMatrixSession(), contacts)

    @Test
    fun `Options appear only while a contact row has focus`() = runTest {
        val vm = subject(FakeContactsRepository(initial = listOf(Contact(wayne, "Wayne", Contact.Source.LOCAL))))
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        assertFalse(vm.state.value.hasOptions)

        vm.onAction(NewChatAction.Focused(wayne))
        advanceUntilIdle()
        assertEquals(wayne, vm.state.value.focusedContact?.address)

        vm.onAction(NewChatAction.Focused(null))
        advanceUntilIdle()
        assertNull(vm.state.value.focusedContact)
    }

    @Test
    fun `saved contacts can be removed, admin ones can't`() = runTest {
        val vm = subject(
            FakeContactsRepository(
                initial = listOf(
                    Contact(boss, "Boss", Contact.Source.ADMIN),
                    Contact(wayne, "Wayne", Contact.Source.LOCAL),
                ),
            ),
        )
        val rows = vm.state.first { !it.isLoading }.contacts
        assertFalse(rows.single { it.address == boss }.isRemovable)
        assertTrue(rows.single { it.address == wayne }.isRemovable)
    }

    @Test
    fun `removing a contact takes it off the list`() = runTest {
        val contacts = FakeContactsRepository(initial = listOf(Contact(wayne, "Wayne", Contact.Source.LOCAL)))
        val vm = subject(contacts)
        vm.navEvents.test {
            vm.onAction(NewChatAction.RemoveContact(wayne))
            assertEquals(NewChatNav.Message(NewChatMessage.CONTACT_REMOVED), awaitItem())
        }
        assertTrue(contacts.contacts.first().isEmpty())
    }

    @Test
    fun `add to a phone contact picks or edits depending on the permission`() = runTest {
        val allowed = subject(FakeContactsRepository(phoneAllowed = true, mergeResult = "Dad"))
        allowed.navEvents.test {
            allowed.onAction(NewChatAction.AddToPhoneContact(wayne))
            assertEquals(NewChatNav.PickPhoneContact, awaitItem())
            allowed.onAction(NewChatAction.PhoneContactPicked("content://com.android.contacts/contacts/1"))
            assertEquals(NewChatNav.Message(NewChatMessage.PHONE_MERGED), awaitItem())
        }

        val notAllowed = subject(FakeContactsRepository(phoneAllowed = false))
        notAllowed.navEvents.test {
            notAllowed.onAction(NewChatAction.AddToPhoneContact(wayne))
            assertEquals(NewChatNav.EditPhoneContact(wayne.value), awaitItem())
        }
    }

    @Test
    fun `no Contacts app says so`() = runTest {
        val vm = subject(FakeContactsRepository(phoneAllowed = false))
        vm.navEvents.test {
            vm.onAction(NewChatAction.PhoneContactsUnavailable)
            assertEquals(NewChatNav.Message(NewChatMessage.PHONE_CONTACTS_UNAVAILABLE), awaitItem())
        }
    }
}
