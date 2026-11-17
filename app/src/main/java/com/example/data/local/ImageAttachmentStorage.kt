package com.example.data.local

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copies picked visual media into app-private storage (`filesDir/attached_images`)
 * so task and note photo attachments survive app restarts, process death, and device reboots
 * even after Android revokes temporary Photo Picker URI grants.
 */
object ImageAttachmentStorage {
    private const val TAG = "ImageAttachmentStorage"
    const val DIRECTORY_NAME = "attached_images"

    fun getImagesDir(context: Context): File {
        return File(context.filesDir, DIRECTORY_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    fun persistPickedImage(context: Context, sourceUri: Uri): String? {
        val appContext = context.applicationContext
        val imagesDir = getImagesDir(appContext)

        // Skip copying only if it is already stored inside our attached_images directory
        if (sourceUri.scheme == ContentResolver.SCHEME_FILE) {
            val path = sourceUri.path
            if (!path.isNullOrBlank()) {
                val existing = File(path)
                val inAppImagesDir = runCatching {
                    existing.canonicalPath.startsWith(imagesDir.canonicalPath + File.separator)
                }.getOrDefault(false)
                if (existing.exists() && inAppImagesDir) {
                    return sourceUri.toString()
                }
            }
        }

        val ext = resolveExtension(appContext, sourceUri)
        val destFile = File(imagesDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext")

        try {
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val copied = input.copyTo(output)
                    output.flush()
                    if (copied > 0L && destFile.exists() && destFile.length() > 0L) {
                        return Uri.fromFile(destFile).toString()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy picked image into app storage: $sourceUri", e)
        }

        // Clean up empty/partial file if copy failed
        if (destFile.exists()) {
            destFile.delete()
        }

        // Fallback: try taking persistable URI permission if supported by provider
        return try {
            appContext.contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            sourceUri.toString()
        } catch (e: Exception) {
            Log.w(TAG, "Persistable URI permission unavailable for $sourceUri", e)
            null
        }
    }

    fun deleteOwnedImageFile(context: Context, imageUri: String?): Boolean {
        if (imageUri.isNullOrBlank()) return false
        return try {
            val parsed = Uri.parse(imageUri)
            val path = when (parsed.scheme) {
                ContentResolver.SCHEME_FILE -> parsed.path
                null -> imageUri
                else -> return false
            } ?: return false
            val target = File(path)
            val imagesDir = getImagesDir(context.applicationContext)
            val inOwnedDir = target.canonicalPath.startsWith(imagesDir.canonicalPath + File.separator)
            inOwnedDir && target.exists() && target.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting owned image file: $imageUri", e)
            false
        }
    }

    fun deleteAllAttachedImages(context: Context) {
        val dir = File(context.applicationContext.filesDir, DIRECTORY_NAME)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    private fun resolveExtension(context: Context, uri: Uri): String {
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val extFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val sanitized = extFromMime?.lowercase()?.filter { it.isLetterOrDigit() }
        return if (!sanitized.isNullOrBlank()) sanitized else "jpg"
    }
}
