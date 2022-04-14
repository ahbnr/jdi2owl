package de.ahbnr.semanticweb.jdi2owl.debugging

import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.Event
import java.util.concurrent.LinkedBlockingQueue
import kotlin.Exception

class JvmEventCollector(
    val vm: VirtualMachine
): AutoCloseable {
    sealed class CollectedEvent {
        data class Exception(val e: kotlin.Exception): CollectedEvent()
        object Disconnected: CollectedEvent()
        data class Just(val event: Event): CollectedEvent()
    }

    private val thread = Thread(::collectEvents, "JvmEventHandler")
    @Volatile
    var connected = false
        private set
    private val queue = LinkedBlockingQueue<CollectedEvent>()

    fun start() {
        connected = true
        thread.start()
    }

    fun take(): CollectedEvent = queue.take()

    private fun collectEvents() {
        try {
            while (connected) {
                val eventSet = vm.eventQueue().remove()
                for (event in eventSet) {
                    queue.put(CollectedEvent.Just(event))
                }
            }
        }

        catch (_: InterruptedException) { }
        catch (_: VMDisconnectedException) {
            try {
                queue.put(CollectedEvent.Disconnected)
            }

            catch (_: InterruptedException) {}
        }
        catch (e: Exception) {
            queue.put(CollectedEvent.Exception(e))
        }

        finally {
            connected = false
        }
    }

    override fun close() {
        connected = false
        thread.interrupt()

        thread.join()
    }
}