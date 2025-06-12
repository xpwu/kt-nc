package com.github.xpwu.nc

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

interface ObserverItem {
  suspend fun remove()
}

class Item<T: Event>(private val queue: WeakReference<Queue<T>>, private val id: Long,
                     var block: suspend (T) -> Unit): ObserverItem {

  override suspend fun remove() {
    queue.get()?.remove(this.id)
    // 释放引用
    this.block = {}
  }

}

class Queue<T: Event> {
  private val items: MutableMap<Long, WeakReference<Item<T>>> = HashMap()
  private var id: Long = 1
  private val mutex: Mutex = Mutex()

  // post 执行过程中，某个 block 可能调用 add / remove 方法，
  // 所以，post 需要拿到 [blocks] 后，再执行 block，防止与 add / remove 互锁
  suspend fun post(e: T) {
    val needDel: MutableList<Long> = ArrayList()

    var entries: MutableSet<MutableMap.MutableEntry<Long, WeakReference<Item<T>>>>
    mutex.withLock {
      entries = items.entries
    }

    for (entry in entries) {
      val item = entry.value.get()
      if (item == null) {
        needDel.add(entry.key)
        continue
      }
      item.block(e)
    }

    mutex.withLock {
      for (id in needDel) {
        items.remove(id)
      }
    }
  }

  suspend fun add(block: suspend (T) -> Unit): ObserverItem {
    mutex.withLock {
      id++
      val item = Item(WeakReference(this), id, block)
      items[id] = WeakReference(item)

      return item
    }
  }

  suspend fun remove(id: Long) {
    mutex.withLock {
      items.remove(id)
    }
  }

}

class NC {
  private val events = HashMap<KClass<*>, Queue<*>>()
  private val mutex = Mutex()

  internal suspend fun<T: Event> getQueue(e: KClass<T>): Queue<T> {
    this.mutex.withLock {
      var oldQ = this.events[e]
      if (oldQ == null) {
        oldQ = Queue<T>()
        this.events[e] = oldQ
      }
      @Suppress("UNCHECKED_CAST")
      return oldQ as Queue<T>
    }
  }
}

suspend fun<T: Event> NC.addEvent(e: KClass<T>, block: suspend (T) -> Unit): ObserverItem {
  return getQueue(e).add(block)
}

suspend fun <T: Event> NC.post(e: T) {
  @Suppress("UNCHECKED_CAST")
  val q = getQueue(e::class) as Queue<T>
  q.post(e)
}

