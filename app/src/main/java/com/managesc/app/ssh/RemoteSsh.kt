package com.managesc.app.ssh

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RemoteSsh {

    /** Test koneksi SSH ke VPS. Return pesan hasil. */
    suspend fun testConnection(host: String, user: String, pass: String, port: Int = 22): String {
        return withContext(Dispatchers.IO) {
            try {
                val jsch = JSch()
                val session: Session = jsch.getSession(user, host, port)
                session.setPassword(pass)
                session.setConfig("StrictHostKeyChecking", "no")
                session.timeout = 15000
                session.connect()
                val ok = session.isConnected
                session.disconnect()
                if (ok) "Koneksi SSH berhasil ke $host" else "Gagal koneksi ke $host"
            } catch (e: Exception) {
                "Gagal SSH: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    /** Jalankan perintah di VPS via SSH. Return output atau error. */
    suspend fun runCommand(host: String, user: String, pass: String, command: String, port: Int = 22): String {
        return withContext(Dispatchers.IO) {
            var session: Session? = null
            try {
                val jsch = JSch()
                session = jsch.getSession(user, host, port)
                session.setPassword(pass)
                session.setConfig("StrictHostKeyChecking", "no")
                session.timeout = 60000
                session.connect()
                val channel = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                channel.setCommand(command)
                channel.inputStream = null
                val output = StringBuilder()
                val input = channel.inputStream
                channel.connect()
                val buf = ByteArray(1024)
                var len: Int
                while (input.read(buf).also { len = it } > 0) {
                    output.append(String(buf, 0, len))
                }
                channel.disconnect()
                session.disconnect()
                output.toString().take(2000)
            } catch (e: Exception) {
                session?.disconnect()
                "Error: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    /**
     * Buka shell interaktif dengan PTY (seperti Termius).
     * Mengembalikan ShellSession yang bisa di-write (keyboard) dan di-read (output stream).
     */
    data class ShellSession(
        val session: Session,
        val channel: com.jcraft.jsch.ChannelShell,
        val input: java.io.OutputStream,   // tulis ke shell (keyboard)
        val output: java.io.InputStream,    // baca dari shell (terminal)
        val err: java.io.InputStream
    )

    /** Update ukuran PTY (cols x rows) saat layar/keyboard berubah. */
    fun resizeShell(shell: ShellSession, cols: Int, rows: Int) {
        try {
            shell.channel.setPtySize(cols, rows, 0, 0)
        } catch (_: Exception) {}
    }

    /** Jalankan skrip setup VPS sekali jalan (exec channel) — untuk tombol Setup Otomatis. */
    suspend fun runSetup(host: String, user: String, pass: String, script: String, port: Int = 22): String {
        return runCommand(host, user, pass, script, port)
    }

    suspend fun openShell(host: String, user: String, pass: String, port: Int = 22): ShellSession {
        return withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session = jsch.getSession(user, host, port)
            session.setPassword(pass)
            session.setConfig("StrictHostKeyChecking", "no")
            session.timeout = 15000
            session.setServerAliveInterval(15000)   // kirim heartbeat tiap 15s cegah putus idle
            session.setServerAliveCountMax(3)
            session.connect()
            val channel = session.openChannel("shell") as com.jcraft.jsch.ChannelShell
            channel.setPty(true)
            channel.setPtyType("xterm", 80, 24, 0, 0)
            channel.inputStream = null
            val out = channel.outputStream   // kita tulis keyboard ke sini
            val inStream = channel.inputStream
            channel.connect(5000)
            ShellSession(session, channel, out, inStream, inStream)
        }
    }
}
