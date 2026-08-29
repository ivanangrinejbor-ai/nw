package org.catrobat.catroid.content

import android.util.Log
import kotlinx.coroutines.*
import org.catrobat.catroid.utils.ErrorLog
import java.io.*
import java.net.*

class LocalServer private constructor() {
    companion object {
        // Разделитель упакованных в одно сообщение значений (невидимый, нельзя ввести с клавиатуры)
        const val VALUE_SEPARATOR: Char = '\u0001'

        // Окно подавления одинаковых подряд идущих сообщений (эхо loopback + ретрансляции дают копии)
        private const val DUPLICATE_WINDOW_MS = 100L

        private var serverSocket: ServerSocket? = null
        private val clients = mutableListOf<Socket>()
        private val outputStreams = mutableListOf<OutputStream>()

        @Volatile var clientLimit: Int = 10
        @Volatile var serverTimeoutSeconds: Int = 30

        @Volatile private var connectedPort: String? = null
        @Volatile private var connectedIP: String? = null
        @Volatile private var isRunning = false
        @Volatile private var clientSocket: Socket? = null

        private val recentMessages = ArrayDeque<String>()
        private var lastMessageAt = 0L

        fun fireTcpMessageEvent(message: String) {
            val listener = org.catrobat.catroid.stage.StageActivity.getActiveStageListener() ?: return
            val activity = org.catrobat.catroid.stage.StageActivity.activeStageActivity.get() ?: return
            val project = org.catrobat.catroid.ProjectManager.getInstance().currentProject ?: return
            activity.runOnUiThread {
                try {
                    for (sprite in listener.spritesFromStage) {
                        for (script in sprite.scriptList) {
                            if (script is WhenTcpMessageScript) {
                                val brick = script.scriptBrick as? org.catrobat.catroid.content.bricks.WhenTcpMessageBrick
                                if (brick?.userVariable != null) {
                                    brick.userVariable.value = message
                                }
                                sprite.look.addAction(sprite.createSequenceAction(script))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LocalServer", "fireTcpMessageEvent failed: ${e.message}")
                }
            }
        }

        fun fireTcpDisconnectedEvent(reason: String) {
            val listener = org.catrobat.catroid.stage.StageActivity.getActiveStageListener() ?: return
            val activity = org.catrobat.catroid.stage.StageActivity.activeStageActivity.get() ?: return
            activity.runOnUiThread {
                try {
                    for (sprite in listener.spritesFromStage) {
                        for (script in sprite.scriptList) {
                            if (script is WhenTcpDisconnectedScript) {
                                sprite.look.addAction(sprite.createSequenceAction(script))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LocalServer", "fireTcpDisconnectedEvent failed: ${e.message}")
                }
            }
        }

        private var serverJob: Job? = null
        private val sendJobs = mutableListOf<Job>()
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var sessionCounter = 0

        fun startOrJoin(ip: String?, port: String) {
            if (ip.isNullOrEmpty()) {
                stop()
                synchronized(recentMessages) {
                    recentMessages.clear()
                }
                val session = ++sessionCounter
                isRunning = true
                serverJob = coroutineScope.launch {
                    try {
                        startServer(port, session)
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e("LocalServer", "Ошибка: ${e.message}", e)
                        }
                    } finally {
                        if (session == sessionCounter) {
                            stop()
                        }
                    }
                }
            } else {
                isRunning = true
                val session = sessionCounter
                coroutineScope.launch {
                    try {
                        connectToServer(ip, port, session)
                    } catch (e: Exception) {
                        Log.e("LocalServer", "Подключение не удалось: ${e.message}")
                    }
                }
            }
        }

        private suspend fun startServer(port: String, session: Int) = withContext(Dispatchers.IO) {
            if (session != sessionCounter) {
                return@withContext
            }
            val server = ServerSocket(port.toInt())
            serverSocket = server
            server.soTimeout = serverTimeoutSeconds * 1000
            val ip = getLocalIPAddress()
            if (session != sessionCounter || !isRunning) {
                try {
                    server.close()
                } catch (e: IOException) {
                    Log.w("LocalServer", "Ошибка при закрытии сокета: ${e.message}")
                }
                serverSocket = null
                return@withContext
            }
            connectedPort = port
            connectedIP = ip
            Log.d("LocalServer",
                "TCP сервер запущен на ${connectedIP ?: "?"}:$port (лимит $clientLimit, таймаут ${serverTimeoutSeconds}с)")

            while (isRunning && session == sessionCounter) {
                try {
                    val socket = server.accept()
                    if (clients.size >= clientLimit) {
                        Log.w("LocalServer", "Достигнут лимит клиентов ($clientLimit), соединение отклонено")
                        socket.close()
                        continue
                    }
                    addClient(socket)
                    launch { setupConnection(socket) }
                } catch (e: java.net.SocketTimeoutException) {
                    Log.d("LocalServer", "Таймаут ожидания клиента, сервер останавливается")
                    break
                }
            }
        }

        private suspend fun connectToServer(ip: String, port: String, session: Int) = withContext(Dispatchers.IO) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(ip, port.toInt()), 10000)
            } catch (e: Exception) {
                try {
                    socket.close()
                } catch (closeError: IOException) {
                    Log.w("LocalServer", "Ошибка при закрытии сокета после неудачного подключения: ${closeError.message}")
                }
                throw e
            }
            if (session != sessionCounter || !isRunning) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    Log.w("LocalServer", "Ошибка при закрытии сокета: ${e.message}")
                }
                return@withContext
            }
            connectedIP = ip
            connectedPort = port
            addClient(socket)
            clientSocket = socket
            setupConnection(socket)
        }

        private fun addClient(socket: Socket) {
            synchronized(clients) {
                clients.add(socket)
                outputStreams.add(socket.getOutputStream())
            }
        }

        private fun removeClient(socket: Socket) {
            synchronized(clients) {
                val index = clients.indexOf(socket)
                if (index >= 0) {
                    clients.removeAt(index)
                    if (index < outputStreams.size) {
                        outputStreams.removeAt(index)
                    }
                }
            }
            try {
                socket.close()
            } catch (e: IOException) {
                Log.w("LocalServer", "Ошибка при закрытии сокета: ${e.message}")
            }
        }

        private suspend fun setupConnection(socket: Socket) = withContext(Dispatchers.IO) {
            socket.soTimeout = serverTimeoutSeconds * 1000
            try {
                BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8)).use { reader ->
                    while (isRunning && !socket.isClosed) {
                        val message = reader.readLine()
                        if (message == null) {
                            Log.d("LocalServer", "Соединение разорвано удаленной стороной.")
                            break
                        }
                        if (message == "nil") {
                            clearRecentMessages()
                            continue
                        }
                        val isBroadcastEcho = message.startsWith("ALL_ECHO:")
                        val isBroadcast = isBroadcastEcho || message.startsWith("ALL:")
                        val cleanMessage = when {
                            isBroadcastEcho -> message.removePrefix("ALL_ECHO:")
                            isBroadcast -> message.removePrefix("ALL:")
                            else -> message
                        }
                        val isDuplicate: Boolean
                        synchronized(recentMessages) {
                            val now = System.currentTimeMillis()
                            isDuplicate = recentMessages.lastOrNull() == cleanMessage &&
                                    now - lastMessageAt < DUPLICATE_WINDOW_MS
                            if (!isDuplicate) {
                                recentMessages.addLast(cleanMessage)
                                while (recentMessages.size > 50) {
                                    recentMessages.removeFirst()
                                }
                                lastMessageAt = now
                            }
                        }
                        if (!isDuplicate) {
                            Log.d("LocalServer", "Получено: $cleanMessage")
                            fireTcpMessageEvent(cleanMessage)
                            if (isBroadcast && serverSocket != null) {
                                val data = (cleanMessage + "\n").toByteArray(Charsets.UTF_8)
                                val outs: List<OutputStream>
                                val senderIndex: Int
                                synchronized(clients) {
                                    outs = outputStreams.toList()
                                    senderIndex = clients.indexOf(socket)
                                }
                                for (i in outs.indices) {
                                    if (!isBroadcastEcho && i == senderIndex) continue
                                    try {
                                        outs[i].write(data)
                                        outs[i].flush()
                                    } catch (e: Exception) {
                                        Log.e("LocalServer", "Ошибка ретрансляции: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRunning && e is java.net.SocketTimeoutException) {
                    fireTcpDisconnectedEvent("timeout")
                } else if (isRunning) {
                    Log.e("LocalServer", "Ошибка чтения: ${e.message}")
                }
            } finally {
                removeClient(socket)
            }
        }

        fun send(value: String) {
            sendAll(listOf(value))
        }

        fun sendAll(values: List<String>) {
            if (values.any { it.startsWith("ALL_ECHO:") } && serverSocket != null) {
                synchronized(recentMessages) {
                    for (v in values) {
                        if (v.startsWith("ALL_ECHO:")) {
                            val clean = v.removePrefix("ALL_ECHO:")
                            recentMessages.addLast(clean)
                            while (recentMessages.size > 50) recentMessages.removeFirst()
                            lastMessageAt = System.currentTimeMillis()
                        }
                    }
                }
            }
            val job = coroutineScope.launch {
                val deadline = System.currentTimeMillis() + 2000
                val pending = values.toMutableList()
                while (System.currentTimeMillis() < deadline && isRunning) {
                    val outs: List<OutputStream>
                    synchronized(clients) {
                        outs = outputStreams.toList()
                    }
                    if (outs.isEmpty()) {
                        delay(10)
                        continue
                    }
                    for (value in pending) {
                        val data = (value + "\n").toByteArray(Charsets.UTF_8)
                        for (out in outs) {
                            try {
                                out.write(data)
                                out.flush()
                            } catch (e: Exception) {
                                Log.e("LocalServer", "Ошибка отправки: ${e.message}")
                            }
                        }
                    }
                    pending.clear()
                    break
                }
                if (pending.isNotEmpty()) {
                    Log.e("LocalServer", "Соединение не установлено.")
                }
            }
            synchronized(sendJobs) {
                sendJobs.add(job)
                job.invokeOnCompletion {
                    synchronized(sendJobs) {
                        sendJobs.remove(job)
                    }
                }
            }
        }

        fun isPortInUse(port: Int): Boolean {
            return try {
                ServerSocket().use { socket ->
                    socket.reuseAddress = true
                    socket.bind(InetSocketAddress(port))
                    false
                }
            } catch (e: Exception) {
                true
            }
        }

        @Synchronized
        fun stop() {
            sessionCounter++
            if (!isRunning) {
                return
            }
            isRunning = false
            try {
                synchronized(sendJobs) {
                    for (job in sendJobs) {
                        job.cancel()
                    }
                    sendJobs.clear()
                }
                synchronized(clients) {
                    for (out in outputStreams) {
                        try {
                            out.close()
                        } catch (e: IOException) {
                            Log.w("LocalServer", "Ошибка при закрытии потока: ${e.message}")
                        }
                    }
                    for (socket in clients) {
                        try {
                            socket.close()
                        } catch (e: IOException) {
                            Log.w("LocalServer", "Ошибка при закрытии сокета: ${e.message}")
                        }
                    }
                    clients.clear()
                    outputStreams.clear()
                }
                serverSocket?.close()
            } catch (e: IOException) {
                Log.w("LocalServer", "Ошибка при закрытии: ${e.message}")
            } finally {
                serverSocket = null
                serverJob?.cancel()
                serverJob = null
                connectedIP = null
                connectedPort = null
                clientSocket = null
            }
        }

        fun getValue(): String {
            synchronized(recentMessages) {
                return recentMessages.lastOrNull() ?: ""
            }
        }

        fun clearRecentMessages() {
            synchronized(recentMessages) {
                recentMessages.clear()
            }
        }

        fun disconnectFromServer() {
            val socket = clientSocket
            if (socket == null) {
                return
            }
            clientSocket = null
            try {
                socket.getOutputStream().write("nil\n".toByteArray(Charsets.UTF_8))
                socket.getOutputStream().flush()
            } catch (e: IOException) {
                Log.w("LocalServer", "Ошибка при отправке nil: ${e.message}")
            }
            removeClient(socket)
        }

        fun getMessages(): List<String> {
            synchronized(recentMessages) {
                return recentMessages.toList()
            }
        }

        fun getIP(): String = connectedIP ?: "NaN"
        fun getPort(): String = connectedPort ?: "NaN"

        private fun getLocalIPAddress(): String? {
            return try {
                NetworkInterface.getNetworkInterfaces().toList()
                    .flatMap { it.inetAddresses.toList() }
                    .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                    ?.hostAddress
            } catch (e: Exception) {
                null
            }
        }
    }
}