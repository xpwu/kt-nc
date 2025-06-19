package com.github.xpwu.nc

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

data class ObserverKey(val event: KClass<*>, val nc: NC)

class ObserverEvents {
  internal val events = HashMap<ObserverKey, ObserverItem>()
  internal val mutex: Mutex = Mutex()
}

suspend fun ObserverEvents.getAndRemove(key: ObserverKey): ObserverItem? {
  mutex.withLock {
    val r = events[key]
    r?.let { events.remove(key) }
    return r
  }
}

suspend fun ObserverEvents.put(key: ObserverKey, item: ObserverItem) {
  mutex.withLock {
    events[key]?.remove()
    events[key] = item
  }
}

suspend fun ObserverEvents.getAndRemoveAll(): List<ObserverItem> {
  mutex.withLock {
    val r = ArrayList<ObserverItem>()
    for (item in events) {
      r.add(item.value)
    }
    events.clear()

    return r
  }
}

interface Observer {
  val events: ObserverEvents
}

suspend fun<T: Event> Observer.addEvent(e: KClass<T>, toNC: NC, block: suspend (T) -> Unit) {
  val item = toNC.addEvent(e, block)
  events.put(ObserverKey(e, toNC), item)
}

suspend fun <T: Event> Observer.removeEvent(e: KClass<T>, fromNC: NC) {
  val key = ObserverKey(e, fromNC)
  events.getAndRemove(key)?.remove()
}

suspend fun Observer.removeAll() {
  val items = events.getAndRemoveAll()
  for (item in items) {
    item.remove()
  }
}
