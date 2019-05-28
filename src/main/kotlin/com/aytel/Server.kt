package com.aytel

import java.io.BufferedReader
import java.net.Socket
import java.io.OutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ServerSocket


class HttpServer {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val ss = ServerSocket(1337)
            while (true) {
                val s = ss.accept()
                System.err.println("Client accepted")
                Thread(SocketProcessor(s)).start()
            }
        }
    }
    private class SocketProcessor @Throws(Throwable::class)
    constructor(private val s: Socket) : Runnable {
        private val inputStream: InputStream = s.getInputStream()
        private val os: OutputStream = s.getOutputStream()

        override fun run() {
            try {
                readInputHeaders()
                writeResponse("<html><body><h1>Hello from Habrahabr</h1></body></html>")
            } catch (t: Throwable) {
                /*do nothing*/
            } finally {
                try {
                    s.close()
                } catch (t: Throwable) {
                    /*do nothing*/
                }

            }
            System.err.println("Client processing finished")
        }

        @Throws(Throwable::class)
        private fun writeResponse(s: String) {
            val response = "HTTP/1.1 200 OK\r\n" +
                    "Server: YarServer/2009-09-09\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + s.length + "\r\n" +
                    "Connection: close\r\n\r\n"
            val result = response + s
            os.write(result.toByteArray())
            os.flush()
        }

        @Throws(Throwable::class)
        private fun readInputHeaders() {
            val br = BufferedReader(InputStreamReader(inputStream))
            while (true) {
                val s = br.readLine()
                if (s == null || s.trim { it <= ' ' }.isEmpty()) {
                    break
                }
            }
        }
    }
}