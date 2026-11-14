package fr.pilmil.sillon.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AndroidGardenStorage(private val dir: File) : PlatformGardenStorage {

    private val gardenFile: File
        get() = File(dir, FILE_NAME)

    override fun exists(): Boolean = gardenFile.exists()

    override fun readText(): String? = try {
        gardenFile.readText(StandardCharsets.UTF_8)
    } catch (_: IOException) {
        null
    }

    override fun writeTextAtomic(content: String): Boolean {
        val tmpFile = File(dir, "$FILE_NAME.tmp")
        return try {
            dir.mkdirs()
            FileOutputStream(tmpFile).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
                fos.fd.sync()
            }
            Files.move(
                tmpFile.toPath(),
                gardenFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
            true
        } catch (_: IOException) {
            try {
                Files.move(tmpFile.toPath(), gardenFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                true
            } catch (_: IOException) {
                tmpFile.delete()
                false
            }
        }
    }

    override fun quarantineCorrupt() {
        val target = File(dir, "$FILE_NAME.corrupt-${System.currentTimeMillis()}")
        try {
            Files.move(gardenFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
        }
        dir.listFiles { f -> f.name.startsWith("$FILE_NAME.corrupt-") }
            ?.sortedByDescending { it.name }
            ?.drop(MAX_CORRUPT_COPIES)
            ?.forEach { it.delete() }
    }

    private companion object {
        const val FILE_NAME = "sillon_garden.json"
        const val MAX_CORRUPT_COPIES = 3
    }
}

@Composable
actual fun rememberGardenStore(): GardenStore {
    val context = LocalContext.current
    return remember { GardenStore(AndroidGardenStorage(context.filesDir)) }
}
