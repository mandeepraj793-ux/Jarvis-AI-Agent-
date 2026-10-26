package com.example

import com.example.data.ai.JarvisAiClient
import com.example.domain.model.ActionProposal
import com.example.domain.model.JarvisActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JarvisUnitTest {

    private lateinit var aiClient: JarvisAiClient

    @Before
    fun setUp() {
        aiClient = JarvisAiClient()
    }

    @Test
    fun parseLocalRuleIntent_openYouTube_english() {
        val result = aiClient.parseLocalRuleIntent("Open YouTube please", isOffline = false)
        assertNotNull(result.actionProposal)
        assertEquals(JarvisActionType.OPEN_APP, result.actionProposal?.type)
        assertEquals("youtube", result.actionProposal?.target)
        assertFalse(result.actionProposal?.requiresConfirmation == true)
    }

    @Test
    fun parseLocalRuleIntent_openWhatsApp_hindiHinglish() {
        val result = aiClient.parseLocalRuleIntent("WhatsApp kholo", isOffline = false)
        assertNotNull(result.actionProposal)
        assertEquals(JarvisActionType.OPEN_APP, result.actionProposal?.type)
        assertEquals("whatsapp", result.actionProposal?.target)
        assertTrue(result.spokenText.contains("khol raha hoon"))
    }

    @Test
    fun parseLocalRuleIntent_alarm_requiresConfirmation() {
        val result = aiClient.parseLocalRuleIntent("Kal subah 7 baje ka alarm lagao", isOffline = false)
        assertNotNull(result.actionProposal)
        assertEquals(JarvisActionType.SET_ALARM, result.actionProposal?.type)
        assertEquals("7", result.actionProposal?.arguments?.get("hour"))
        assertTrue(result.actionProposal?.requiresConfirmation == true)
    }

    @Test
    fun parseLocalRuleIntent_settings_wifi() {
        val result = aiClient.parseLocalRuleIntent("Open wifi settings", isOffline = false)
        assertNotNull(result.actionProposal)
        assertEquals(JarvisActionType.OPEN_SETTINGS, result.actionProposal?.type)
        assertEquals("wifi", result.actionProposal?.target)
    }

    @Test
    fun parseLocalRuleIntent_createNote() {
        val result = aiClient.parseLocalRuleIntent("Note that I need to buy milk tomorrow", isOffline = false)
        assertNotNull(result.actionProposal)
        assertEquals(JarvisActionType.CREATE_NOTE, result.actionProposal?.type)
        assertNotNull(result.actionProposal?.arguments?.get("note_content"))
    }

    @Test
    fun actionProposal_securityAllowlistValidation() {
        val safeAction = ActionProposal(
            type = JarvisActionType.OPEN_APP,
            target = "youtube",
            requiresConfirmation = false
        )
        assertEquals(JarvisActionType.OPEN_APP, safeAction.type)

        val unknownAction = JarvisActionType.fromString("MALICIOUS_SHELL_EXECUTE")
        assertEquals(JarvisActionType.UNKNOWN, unknownAction)
    }

    @Test
    fun parseLocalRuleIntent_offlineNotice() {
        val result = aiClient.parseLocalRuleIntent("What is the meaning of quantum physics", isOffline = true)
        assertTrue(result.isOfflineResponse)
        assertTrue(result.spokenText.contains("Offline mode active"))
    }
}
