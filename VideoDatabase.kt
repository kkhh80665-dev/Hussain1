package com.example.novavideo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Local SQLite library for user-provided, authorized video URLs. */
class VideoDatabase(context: Context) : SQLiteOpenHelper(context, "nova_video.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE videos (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, url TEXT NOT NULL UNIQUE, source TEXT NOT NULL DEFAULT 'رابط مباشر', added_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { }

    fun allVideos(): List<VideoSource> {
        val result = mutableListOf<VideoSource>()
        readableDatabase.query("videos", arrayOf("title", "url", "source"), null, null, null, null, "added_at DESC").use { c ->
            while (c.moveToNext()) result.add(VideoSource(c.getString(0), c.getString(1), c.getString(2)))
        }
        return result
    }

    fun addVideo(video: VideoSource): Boolean {
        val values = ContentValues().apply {
            put("title", video.title); put("url", video.url); put("source", video.source); put("added_at", System.currentTimeMillis())
        }
        return writableDatabase.insertWithOnConflict("videos", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    fun deleteVideo(url: String) {
        writableDatabase.delete("videos", "url = ?", arrayOf(url))
    }
}
