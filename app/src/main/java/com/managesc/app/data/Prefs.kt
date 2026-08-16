package com.managesc.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.MessageDigest

object Prefs {
    private const val NAME = "managesc_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_GH_TOKEN = "gh_token"
    private const val KEY_GH_USER = "gh_user"
    private const val KEY_GH_REPO = "gh_repo"
    private const val KEY_GH_PATH = "gh_path"
    private const val KEY_GH_ENABLED = "gh_enabled"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun hasPin(ctx: Context): Boolean = sp(ctx).contains(KEY_PIN_HASH)

    fun clearPin(ctx: Context) = sp(ctx).edit { remove(KEY_PIN_HASH) }

    fun setPin(ctx: Context, pin: String) {
        sp(ctx).edit { putString(KEY_PIN_HASH, hash(pin)) }
    }

    fun verifyPin(ctx: Context, pin: String): Boolean {
        val h = sp(ctx).getString(KEY_PIN_HASH, null) ?: return false
        return h == hash(pin)
    }

    private fun hash(s: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // GitHub
    fun getGhToken(ctx: Context) = sp(ctx).getString(KEY_GH_TOKEN, "") ?: ""
    fun setGhToken(ctx: Context, v: String) = sp(ctx).edit { putString(KEY_GH_TOKEN, v) }

    fun getGhUser(ctx: Context) = sp(ctx).getString(KEY_GH_USER, "bowowiwendi") ?: ""
    fun setGhUser(ctx: Context, v: String) = sp(ctx).edit { putString(KEY_GH_USER, v) }

    fun getGhRepo(ctx: Context) = sp(ctx).getString(KEY_GH_REPO, "ipvps") ?: ""
    fun setGhRepo(ctx: Context, v: String) = sp(ctx).edit { putString(KEY_GH_REPO, v) }

    fun getGhPath(ctx: Context) = sp(ctx).getString(KEY_GH_PATH, "main/ip") ?: ""
    fun setGhPath(ctx: Context, v: String) = sp(ctx).edit { putString(KEY_GH_PATH, v) }

    fun isGhEnabled(ctx: Context) = sp(ctx).getBoolean(KEY_GH_ENABLED, false)
    fun setGhEnabled(ctx: Context, v: Boolean) = sp(ctx).edit { putBoolean(KEY_GH_ENABLED, v) }
}
