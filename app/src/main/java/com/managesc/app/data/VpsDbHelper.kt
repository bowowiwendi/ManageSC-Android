package com.managesc.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Vps(
    val id: Long = 0,
    val username: String = "",
    val tipeAkun: String = "",
    val masaAktif: String = "",   // tanggal kedaluwarsa, format yyyy-MM-dd
    val ipVps: String = "",
    val emailMember: String = "",
    val ram: String = "",
    val pesan: String = ""
)

class VpsDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT,
                tipe_akun TEXT,
                masa_aktif TEXT,
                ip_vps TEXT,
                email_member TEXT,
                ram TEXT,
                pesan TEXT
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insert(v: Vps): Long {
        val db = writableDatabase
        val cv = v.toValues()
        cv.remove("id")
        return db.insert(TABLE, null, cv)
    }

    fun update(v: Vps) {
        val db = writableDatabase
        db.update(TABLE, v.toValues(), "id = ?", arrayOf(v.id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }

    fun getAll(): List<Vps> {
        val list = mutableListOf<Vps>()
        readableDatabase.query(
            TABLE, null, null, null, null, null, "masa_aktif ASC"
        ).use { c ->
            while (c.moveToNext()) list.add(c.toVps())
        }
        return list
    }

    fun getById(id: Long): Vps? {
        readableDatabase.query(
            TABLE, null, "id = ?", arrayOf(id.toString()), null, null, null
        ).use { c ->
            if (c.moveToFirst()) return c.toVps()
        }
        return null
    }

    private fun Vps.toValues() = ContentValues().apply {
        put("username", username)
        put("tipe_akun", tipeAkun)
        put("masa_aktif", masaAktif)
        put("ip_vps", ipVps)
        put("email_member", emailMember)
        put("ram", ram)
        put("pesan", pesan)
    }

    private fun Cursor.toVps() = Vps(
        id = getLong(getColumnIndexOrThrow("id")),
        username = getString(getColumnIndexOrThrow("username")) ?: "",
        tipeAkun = getString(getColumnIndexOrThrow("tipe_akun")) ?: "",
        masaAktif = getString(getColumnIndexOrThrow("masa_aktif")) ?: "",
        ipVps = getString(getColumnIndexOrThrow("ip_vps")) ?: "",
        emailMember = getString(getColumnIndexOrThrow("email_member")) ?: "",
        ram = getString(getColumnIndexOrThrow("ram")) ?: "",
        pesan = getString(getColumnIndexOrThrow("pesan")) ?: ""
    )

    companion object {
        const val DB_NAME = "managesc.db"
        const val DB_VERSION = 1
        const val TABLE = "vps"
    }
}
