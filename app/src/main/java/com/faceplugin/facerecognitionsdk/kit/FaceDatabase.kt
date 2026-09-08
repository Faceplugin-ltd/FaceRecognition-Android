package com.faceplugin.facerecognitionsdk.kit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FaceDatabase private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val lock = ReentrantLock()
    private val file = File(appContext.filesDir, "face_database.json")
    private val thumbDir = File(appContext.filesDir, "face_thumbnails")

    @Volatile
    private var peopleInternal: MutableList<EnrolledPerson> = mutableListOf()

    val people: List<EnrolledPerson>
        get() = lock.withLock { peopleInternal.toList() }

    val count: Int
        get() = lock.withLock { peopleInternal.size }

    val isEmpty: Boolean
        get() = count == 0

    fun load() {
        lock.withLock {
            if (!file.exists()) {
                peopleInternal = mutableListOf()
                return
            }
            try {
                val root = JSONArray(file.readText())
                val list = mutableListOf<EnrolledPerson>()
                for (i in 0 until root.length()) {
                    val obj = root.optJSONObject(i) ?: continue
                    list += EnrolledPerson(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        featureBase64 = obj.optString("featureBase64"),
                        thumbnailFile = obj.optString("thumbnailFile").takeIf { it.isNotBlank() },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    )
                }
                peopleInternal = list
            } catch (_: Exception) {
                peopleInternal = mutableListOf()
            }
        }
    }

    fun add(name: String, feature: ByteArray, thumbnail: Bitmap?): EnrolledPerson? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || feature.isEmpty()) return null
        val id = UUID.randomUUID().toString()
        val thumbFile = thumbnail?.let { saveThumbnail(it, id) }
        val person = EnrolledPerson(
            id = id,
            name = trimmed,
            featureBase64 = Base64.encodeToString(feature, Base64.NO_WRAP),
            thumbnailFile = thumbFile,
            createdAt = System.currentTimeMillis(),
        )
        lock.withLock {
            peopleInternal.add(person)
            persistLocked()
        }
        return person
    }

    fun remove(ids: Set<String>) {
        if (ids.isEmpty()) return
        lock.withLock {
            for (id in ids) deleteThumbnailLocked(id)
            peopleInternal.removeAll { it.id in ids }
            persistLocked()
        }
    }

    fun updateName(id: String, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        return lock.withLock {
            val index = peopleInternal.indexOfFirst { it.id == id }
            if (index < 0) return@withLock false
            peopleInternal[index].name = trimmed
            persistLocked()
            true
        }
    }

    fun clear() {
        lock.withLock {
            peopleInternal.clear()
            persistLocked()
            if (thumbDir.exists()) thumbDir.deleteRecursively()
        }
    }

    fun bestMatch(feature: ByteArray, threshold: Float): BestMatch? {
        val snapshot = people
        var best: BestMatch? = null
        for (person in snapshot) {
            val stored = try {
                Base64.decode(person.featureBase64, Base64.DEFAULT)
            } catch (_: Exception) {
                null
            } ?: continue
            val score = FaceRecognitionQueue.similarity(feature, stored)
            if (score < threshold) continue
            if (best == null || score > best.score) {
                best = BestMatch(person, score)
            }
        }
        return best
    }

    fun featureTemplates(): List<ByteArray> = lock.withLock {
        peopleInternal.mapNotNull {
            try {
                Base64.decode(it.featureBase64, Base64.DEFAULT)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun personAtVideoWorkerIndex(index: Int): EnrolledPerson? = lock.withLock {
        peopleInternal.getOrNull(index)
    }

    fun thumbnail(person: EnrolledPerson): Bitmap? {
        val fileName = person.thumbnailFile ?: return null
        val path = File(thumbDir, fileName)
        if (!path.isFile) return null
        return BitmapFactory.decodeFile(path.absolutePath)
    }

    private fun persistLocked() {
        thumbDir.mkdirs()
        val arr = JSONArray()
        for (p in peopleInternal) {
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("featureBase64", p.featureBase64)
                    .put("thumbnailFile", p.thumbnailFile)
                    .put("createdAt", p.createdAt),
            )
        }
        file.writeText(arr.toString())
    }

    private fun deleteThumbnailLocked(id: String) {
        val person = peopleInternal.firstOrNull { it.id == id } ?: return
        val fileName = person.thumbnailFile ?: return
        File(thumbDir, fileName).delete()
    }

    private fun saveThumbnail(image: Bitmap, id: String): String? {
        return try {
            thumbDir.mkdirs()
            val fileName = "$id.jpg"
            val outFile = File(thumbDir, fileName)
            FileOutputStream(outFile).use { out ->
                image.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            fileName
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: FaceDatabase? = null

        fun get(context: Context): FaceDatabase {
            return instance ?: synchronized(this) {
                instance ?: FaceDatabase(context.applicationContext).also { instance = it }
            }
        }
    }
}
