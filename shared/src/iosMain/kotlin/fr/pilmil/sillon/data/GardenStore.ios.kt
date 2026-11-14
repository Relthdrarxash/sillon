@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package fr.pilmil.sillon.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fr.pilmil.sillon.model.nowEpochMillis
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

class IosGardenStorage : PlatformGardenStorage {

    private val fileManager = NSFileManager.defaultManager

    private val docsDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        (paths.firstOrNull() as? String) ?: ""
    }

    private val gardenPath: String
        get() = "$docsDir/$FILE_NAME"

    override fun exists(): Boolean = fileManager.fileExistsAtPath(gardenPath)

    override fun readText(): String? =
        NSString.stringWithContentsOfFile(gardenPath, encoding = NSUTF8StringEncoding, error = null)

    override fun writeTextAtomic(content: String): Boolean {
        val ns = NSString.create(string = content)
        return ns.writeToFile(gardenPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    override fun quarantineCorrupt() {
        val target = "$docsDir/$FILE_NAME.corrupt-${nowEpochMillis()}"
        fileManager.moveItemAtPath(gardenPath, toPath = target, error = null)
    }

    private companion object {
        const val FILE_NAME = "sillon_garden.json"
    }
}

@Composable
actual fun rememberGardenStore(): GardenStore = remember { GardenStore(IosGardenStorage()) }
