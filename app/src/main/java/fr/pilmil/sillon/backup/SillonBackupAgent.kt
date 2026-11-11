package fr.pilmil.sillon.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.FullBackupDataOutput
import android.os.Build

/**
 * Filtre les sauvegardes automatiques d'Android selon [BackupPolicy] : rien ne part vers le compte Google
 * sans le choix de la personne, et jamais sans chiffrement de bout en bout.
 */
class SillonBackupAgent : BackupAgentHelper() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        val flags = if (Build.VERSION.SDK_INT >= BackupPolicy.MIN_SDK_WITH_FLAGS) data.transportFlags else 0
        val consent = CloudBackupConsent(noBackupFilesDir)
        if (BackupPolicy.allows(Build.VERSION.SDK_INT, flags, consent.enabled)) {
            super.onFullBackup(data)
        }
    }
}
