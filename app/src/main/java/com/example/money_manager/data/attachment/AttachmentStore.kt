package com.example.money_manager.data.attachment

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Photos are copied into the app's own images folder so they survive the picker's
 * transient URI grants and never leave the device. Only the file name is stored
 * with a transaction.
 */
object AttachmentStore {

    private fun dir(context: Context): File = AppStorage.images(context)

    fun file(context: Context, name: String): File = File(dir(context), name)

    fun exists(context: Context, name: String?): Boolean =
        name != null && file(context, name).exists()

    /** Reserves a file the camera can write straight into. */
    fun newPhotoFile(context: Context): File = file(context, "${UUID.randomUUID()}.jpg")

    fun contentUriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    /** Copies a picked image in and returns its stored name, or null if it could not be read. */
    fun importPhoto(context: Context, source: Uri): String? {
        val target = newPhotoFile(context)
        return runCatching {
            context.contentResolver.openInputStream(source).use { input ->
                requireNotNull(input)
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.name
        }.getOrElse {
            target.delete()
            null
        }
    }

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        runCatching { file(context, name).delete() }
    }
}
