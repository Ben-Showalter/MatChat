package org.matchat.feature.scan

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

/** Where the offline setup page (S33) can be saved. Behind an interface so
 *  [SetupHelpViewModel] is unit-testable without a device. */
interface SetupPageStore {
    /** API 24–28 need WRITE_EXTERNAL_STORAGE for the public Download folder. */
    val needsStoragePermission: Boolean

    /** Saves to Download, replacing an earlier copy; the path to show, or null. */
    fun saveToDownloads(): String?

    /** Saves to the SD card (MatChat's own folder there, no permission needed). */
    fun saveToSdCard(): SdCardSave
}

sealed interface SdCardSave {
    data class Saved(val where: String) : SdCardSave
    data object NoCard : SdCardSave
    data object Failed : SdCardSave
}

/**
 * Puts the offline setup page (assets/matchat-setup.html, built from
 * tools/setup-code/) where the user can take it to a computer (UX-SPEC S33,
 * Flip-DumbPhoneGuide "Setup codes"). The page itself holds no secrets.
 * Ported from BusyCall's SetupPageSaver.
 */
internal class AndroidSetupPageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SetupPageStore {

    override val needsStoragePermission: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun saveToDownloads(): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveWithMediaStore() else saveLegacy()
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalStateException) {
        null
    }

    @Suppress("NewApi") // Only called on API 29+.
    private fun saveWithMediaStore(): String? {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        // Our own earlier copy (MediaStore only shows an app the files it made): replace it.
        val existing: Uri? = resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(PAGE_FILE),
            null,
        )?.use { c -> if (c.moveToFirst()) Uri.withAppendedPath(collection, c.getLong(0).toString()) else null }
        val uri = existing ?: resolver.insert(
            collection,
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, PAGE_FILE)
                put(MediaStore.Downloads.MIME_TYPE, MIME)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            },
        ) ?: return null
        val out = resolver.openOutputStream(uri, "wt") ?: return null
        copyPage(out)
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        // If another app's file already had the name, MediaStore picked another: show the real one.
        val actual = resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: PAGE_FILE
        return "${Environment.DIRECTORY_DOWNLOADS} › $actual"
    }

    @Suppress("DEPRECATION") // The public Download folder on API 24–28.
    private fun saveLegacy(): String? {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, PAGE_FILE)
        copyPage(file.outputStream())
        // So it shows up straight away when the phone is plugged into a computer (USB/MTP).
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(MIME), null)
        return "${Environment.DIRECTORY_DOWNLOADS} › ${file.name}"
    }

    override fun saveToSdCard(): SdCardSave {
        val dir = context.getExternalFilesDirs(null).drop(1).firstOrNull {
            it != null && Environment.isExternalStorageRemovable(it) &&
                Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED
        } ?: return SdCardSave.NoCard
        return try {
            val file = File(dir, PAGE_FILE)
            copyPage(file.outputStream())
            val onCard = file.absolutePath.substringAfter("/Android/", "")
            SdCardSave.Saved(if (onCard.isEmpty()) file.name else "Android/$onCard")
        } catch (e: IOException) {
            SdCardSave.Failed
        }
    }

    private fun copyPage(out: OutputStream) {
        context.assets.open(PAGE_FILE).use { input -> out.use { input.copyTo(it) } }
    }

    companion object {
        const val PAGE_FILE = "matchat-setup.html"
        private const val MIME = "text/html"
        private const val SHARE_DIR = "setup"

        /** AOSP's Bluetooth share target. If a phone names it differently, the share sheet is used. */
        private const val BLUETOOTH_PACKAGE = "com.android.bluetooth"

        /** An ACTION_SEND intent for the page through the app's FileProvider
         *  (cache-path "setup"), aimed at Bluetooth when [bluetoothOnly]. Null
         *  if the page couldn't be prepared. */
        fun shareIntent(context: Context, bluetoothOnly: Boolean): Intent? = try {
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            val file = File(dir, PAGE_FILE)
            context.assets.open(PAGE_FILE).use { input -> file.outputStream().use { input.copyTo(it) } }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            Intent(Intent.ACTION_SEND).apply {
                type = MIME
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (bluetoothOnly) setPackage(BLUETOOTH_PACKAGE)
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SetupPageModule {
    @Binds
    abstract fun bindStore(impl: AndroidSetupPageStore): SetupPageStore
}
