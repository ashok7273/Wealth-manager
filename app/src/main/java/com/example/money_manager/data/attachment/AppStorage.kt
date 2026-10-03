package com.example.money_manager.data.attachment

import android.content.Context
import java.io.File

/**
 * Everything the app writes lives under one app-owned folder on shared storage
 * (Android/data/<package>/files), which needs no storage permission and is removed
 * with the app. Falls back to internal storage when no external volume is mounted.
 */
object AppStorage {

    private const val IMAGES = "images"

    fun root(context: Context): File =
        (context.getExternalFilesDir(null) ?: context.filesDir).apply { mkdirs() }

    fun images(context: Context): File = File(root(context), IMAGES).apply { mkdirs() }
}
