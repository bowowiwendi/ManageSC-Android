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
    val masaAktif: String = "",   // tanggal kedaluwarsa, format yyyy-MM-dd (kosong untuk Unlimited)
    val ipVps: String = "",
    val emailMember: String = "",
    val ram: String = "",
    val pesan: String = "",
    val userSsh: String = "",
    val passSsh: String = "",
    val serverAktif: Boolean = true
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
                pesan TEXT,
                user_ssh TEXT,
                pass_ssh TEXT,
                server_aktif INTEGER DEFAULT 1
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

    fun replaceAll(list: List<Vps>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE, null, null)
            for (v in list) {
                val cv = v.toValues()
                cv.remove("id")
                db.insert(TABLE, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
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
        put("user_ssh", userSsh)
        put("pass_ssh", passSsh)
        put("server_aktif", if (serverAktif) 1 else 0)
    }

    private fun Cursor.toVps() = Vps(
        id = getLong(getColumnIndexOrThrow("id")),
        username = getString(getColumnIndexOrThrow("username")) ?: "",
        tipeAkun = getString(getColumnIndexOrThrow("tipe_akun")) ?: "",
        masaAktif = getString(getColumnIndexOrThrow("masa_aktif")) ?: "",
        ipVps = getString(getColumnIndexOrThrow("ip_vps")) ?: "",
        emailMember = getString(getColumnIndexOrThrow("email_member")) ?: "",
        ram = getString(getColumnIndexOrThrow("ram")) ?: "",
        pesan = getString(getColumnIndexOrThrow("pesan")) ?: "",
        userSsh = getString(getColumnIndexOrThrow("user_ssh")) ?: "",
        passSsh = getString(getColumnIndexOrThrow("pass_ssh")) ?: "",
        serverAktif = getInt(getColumnIndexOrThrow("server_aktif")) == 1
    )

    companion object {
        const val DB_NAME = "managesc.db"
        const val DB_VERSION = 2
        const val TABLE = "vps"
    }
}
