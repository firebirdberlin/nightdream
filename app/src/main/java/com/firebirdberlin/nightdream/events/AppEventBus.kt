package com.firebirdberlin.nightdream.events

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

class AppEventBus {
    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val mainHandler = Handler(Looper.getMainLooper())

        private val _events = MutableSharedFlow<Any>(extraBufferCapacity = 64)
        val events: SharedFlow<Any> = _events.asSharedFlow()

        private val stickyEvents = ConcurrentHashMap<Class<*>, Any>()
        private val javaListeners = CopyOnWriteArrayList<Consumer<Any>>()

        @JvmStatic
        fun publish(event: Any) {
            scope.launch {
                _events.emit(event)
            }
            mainHandler.post {
                for (consumer in javaListeners) {
                    try {
                        consumer.accept(event)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        @JvmStatic
        fun postSticky(event: Any) {
            stickyEvents[event.javaClass] = event
            publish(event)
        }

        @JvmStatic
        fun <T> removeStickyEvent(eventType: Class<T>): T? {
            return stickyEvents.remove(eventType) as? T
        }

        @JvmStatic
        fun <T> getStickyEvent(eventType: Class<T>): T? {
            return stickyEvents[eventType] as? T
        }

        @JvmStatic
        fun addListener(consumer: Consumer<Any>) {
            if (!javaListeners.contains(consumer)) {
                javaListeners.add(consumer)
            }
        }

        @JvmStatic
        fun removeListener(consumer: Consumer<Any>) {
            javaListeners.remove(consumer)
        }
    }
}
