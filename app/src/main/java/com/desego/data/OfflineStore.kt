package com.desego.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.util.UUID

/** Public catalog, per-account snapshots and an idempotent, durable mutation outbox. */
class OfflineStore(context: Context, name: String = "desego-offline.db") : SQLiteOpenHelper(context.applicationContext, name, null, 1) {
    private val assets = context.applicationContext.assets

    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE snapshots (scope TEXT NOT NULL, resource TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(scope,resource))")
        db.execSQL("CREATE TABLE outbox (id INTEGER PRIMARY KEY AUTOINCREMENT, scope TEXT NOT NULL, resource TEXT NOT NULL, path TEXT NOT NULL, method TEXT NOT NULL, payload TEXT NOT NULL, version TEXT NOT NULL, error TEXT, UNIQUE(scope,resource))")
        val seed = assets.open("catalog-seed.json").bufferedReader().use { it.readText() }
        db.insertOrThrow("snapshots", null, ContentValues().apply {
            put("scope", "public"); put("resource", "catalog"); put("payload", seed); put("updated_at", 0L)
        })
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun read(scope: String, resource: String): JSONObject? = readableDatabase.query("snapshots", arrayOf("payload"), "scope=? AND resource=?", arrayOf(scope, resource), null, null, null).use {
        if (it.moveToFirst()) runCatching { JSONObject(it.getString(0)) }.getOrNull() else null
    }
    fun write(scope: String, resource: String, value: JSONObject) {
        writableDatabase.insertWithOnConflict("snapshots", null, ContentValues().apply {
            put("scope", scope); put("resource", resource); put("payload", value.toString()); put("updated_at", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun enqueue(scope: String, resource: String, path: String, method: String, payload: JSONObject) {
        writableDatabase.insertWithOnConflict("outbox", null, ContentValues().apply {
            put("scope", scope); put("resource", resource); put("path", path); put("method", method)
            put("payload", payload.toString()); put("version", UUID.randomUUID().toString())
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
    data class Mutation(val id: Long, val resource: String, val path: String, val method: String, val payload: JSONObject, val version: String, val error: String?)
    fun pending(scope: String): List<Mutation> = readableDatabase.query("outbox", null, "scope=?", arrayOf(scope), null, null, "id ASC").use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                fun value(name: String) = cursor.getString(cursor.getColumnIndexOrThrow(name))
                add(Mutation(cursor.getLong(cursor.getColumnIndexOrThrow("id")), value("resource"), value("path"), value("method"), JSONObject(value("payload")), value("version"), value("error")))
            }
        }
    }
    fun acknowledge(mutation: Mutation) { writableDatabase.delete("outbox", "id=? AND version=?", arrayOf(mutation.id.toString(), mutation.version)) }
    fun reject(mutation: Mutation, message: String) { writableDatabase.update("outbox", ContentValues().apply { put("error", message) }, "id=? AND version=?", arrayOf(mutation.id.toString(), mutation.version)) }
    fun clearScope(scope: String) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("snapshots", "scope=?", arrayOf(scope))
            writableDatabase.delete("outbox", "scope=?", arrayOf(scope))
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
}
