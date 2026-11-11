package fr.pilmil.sillon

import fr.pilmil.sillon.backup.BackupPolicy
import fr.pilmil.sillon.backup.CloudBackupConsent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupPolicyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val encrypted = BackupPolicy.FLAG_CLIENT_SIDE_ENCRYPTION
    private val transfer = BackupPolicy.FLAG_DEVICE_TO_DEVICE

    @Test
    fun `le nuage est interdit par defaut`() {
        assertFalse(BackupPolicy.allows(sdk = 34, transportFlags = encrypted, cloudBackupEnabled = false))
    }

    @Test
    fun `le nuage est permis une fois active et chiffre de bout en bout`() {
        assertTrue(BackupPolicy.allows(sdk = 34, transportFlags = encrypted, cloudBackupEnabled = true))
    }

    @Test
    fun `le nuage est refuse sans chiffrement meme active`() {
        assertFalse(BackupPolicy.allows(sdk = 34, transportFlags = 0, cloudBackupEnabled = true))
    }

    @Test
    fun `le transfert direct est toujours permis`() {
        assertTrue(BackupPolicy.allows(sdk = 31, transportFlags = transfer, cloudBackupEnabled = false))
        assertTrue(BackupPolicy.allows(sdk = 34, transportFlags = transfer or encrypted, cloudBackupEnabled = false))
    }

    @Test
    fun `avant Android 12 rien ne part`() {
        assertFalse(BackupPolicy.allows(sdk = 30, transportFlags = encrypted or transfer, cloudBackupEnabled = true))
        assertFalse(BackupPolicy.settingAvailable(30))
        assertTrue(BackupPolicy.settingAvailable(31))
    }

    @Test
    fun `le choix est desactive au depart puis se retient`() {
        val consent = CloudBackupConsent(tmp.newFolder("no_backup"))
        assertFalse(consent.enabled)
        consent.enabled = true
        assertTrue(consent.enabled)
        assertTrue(CloudBackupConsent(tmp.root.resolve("no_backup")).enabled)
        consent.enabled = false
        assertEquals(false, CloudBackupConsent(tmp.root.resolve("no_backup")).enabled)
    }
}
