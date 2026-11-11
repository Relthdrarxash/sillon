package fr.pilmil.sillon.backup

import java.io.File

/**
 * Qui a le droit de copier les données de Sillon, et quand.
 *
 * - Le transfert direct d'un téléphone à l'autre est toujours permis (Android 12 et plus).
 * - La sauvegarde dans le compte Google n'est permise que si la personne l'a activée, et seulement
 *   quand Android la chiffre de bout en bout avec le verrouillage d'écran : Google ne peut alors pas
 *   lire les données.
 * - Sous Android 12, rien ne permet de vérifier ce chiffrement ni de distinguer le transfert direct :
 *   Sillon ne laisse alors rien partir.
 */
object BackupPolicy {

    /** Valeurs de BackupAgent.FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED et FLAG_DEVICE_TO_DEVICE_TRANSFER. */
    const val FLAG_CLIENT_SIDE_ENCRYPTION = 1
    const val FLAG_DEVICE_TO_DEVICE = 2

    /** Première version d'Android qui indique le type de sauvegarde à l'application (API 31). */
    const val MIN_SDK_WITH_FLAGS = 31

    fun allows(sdk: Int, transportFlags: Int, cloudBackupEnabled: Boolean): Boolean {
        if (sdk < MIN_SDK_WITH_FLAGS) return false
        if ((transportFlags and FLAG_DEVICE_TO_DEVICE) != 0) return true
        return cloudBackupEnabled && (transportFlags and FLAG_CLIENT_SIDE_ENCRYPTION) != 0
    }

    /** Le réglage n'est proposé que là où la règle ci-dessus peut s'appliquer. */
    fun settingAvailable(sdk: Int): Boolean = sdk >= MIN_SDK_WITH_FLAGS
}

/**
 * Choix de la personne : sauvegarder ou non dans son compte Google. Désactivé tant qu'elle ne l'a pas décidé.
 * Le choix est un simple fichier dans le dossier exclu des sauvegardes : il n'est jamais restauré
 * sur un autre téléphone, où la personne décide de nouveau.
 */
class CloudBackupConsent(private val noBackupDir: File) {

    private val marker: File
        get() = File(noBackupDir, MARKER_NAME)

    var enabled: Boolean
        get() = marker.exists()
        set(value) {
            if (value) {
                noBackupDir.mkdirs()
                marker.createNewFile()
            } else {
                marker.delete()
            }
        }

    private companion object {
        const val MARKER_NAME = "cloud_backup_enabled"
    }
}
