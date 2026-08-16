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
}
