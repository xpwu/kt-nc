package com.github.xpwu.nc

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NCUnitTest {

  // 防止 item 被 gc 回收，故在类变量中保存
  private val items = ArrayList<ObserverItem>()

  @Test
  fun add(): Unit = runBlocking {
    val nc = NC()
    var times = 0
    var eventB = 0

    var item: ObserverItem? = null
    item = nc.addEvent(UserInfoChanged::class) {
      if (times == 3) {
        item?.remove()
      }

      eventB++
      assertArrayEquals(it.ids, arrayOf("a", "b"))
    }

    nc.addEvent(Built::class) {
      assertFalse(true)
    }

    while (times < 5) {
      val job = launch {
        times++
        if (times <= 3) {
          nc.post(UserInfoChanged(arrayOf("a", "b")))

          assertEquals(eventB, times)
          return@launch
        }

        nc.post(UserInfoChanged(arrayOf("c", "d")))
        assertNotEquals(eventB, times)
      }

      job.join()
    }
  }

  @Test
  fun all() = runBlocking {

    items.clear()

    var postTimes = 0
    var userInfoChangedBlock3 = 0
    val userInfoChangedVar = arrayOf("a", "b")
    var userInfoChangedBlockEver = 0
    var buildBlockEver = 0
    var buildBlock = 0
    val buildBlockVar = arrayOf(12, 45)
    var uploadProgressChangedBlock = 0
    val uploadProgressChangedVar = arrayOf("dkjfd")

    val nc = NC()

    var item1: ObserverItem? = null
    item1 = nc.addEvent(UserInfoChanged::class) {
      userInfoChangedBlock3++
      assertArrayEquals(it.ids, userInfoChangedVar)

      if (userInfoChangedBlock3 == 3) {
        item1?.remove()
      }
    }

    val item2 = nc.addEvent(UserInfoChanged::class) {
      userInfoChangedBlockEver++
      assertArrayEquals(it.ids, userInfoChangedVar)
    }
    items.add(item2)

    var autoRelease: ObserverItem? = nc.addEvent(UploadProgressChanged::class) {
      uploadProgressChangedBlock++
      assertArrayEquals(it.ids, uploadProgressChangedVar)
    }

    val o = nc.addEvent(Built::class) {
      buildBlockEver++
      assertArrayEquals(it.ids, buildBlockVar)
    }
    items.add(o)

    val oRemove = nc.addEvent(Built::class) {
      buildBlock++
      assertArrayEquals(it.ids, buildBlockVar)
    }

    while (postTimes < 13) {
      val job = launch {
        postTimes++

        nc.post(Built(buildBlockVar))
        nc.post(UserInfoChanged(userInfoChangedVar))
        nc.post(UploadProgressChanged(uploadProgressChangedVar))

        assertEquals(buildBlockEver, postTimes)
        assertEquals(userInfoChangedBlockEver, postTimes)

        val userInfoChangedBlock3C = 3
        if (postTimes <= userInfoChangedBlock3C) {
          assertEquals(userInfoChangedBlock3, postTimes)
        } else {
          assertEquals(userInfoChangedBlock3, userInfoChangedBlock3C)
        }

        val autoReleaseC = 7
        if (postTimes == autoReleaseC) {
          autoRelease = null
          System.gc()
          // 等待 gc 完成，无法明确的知道 gc 的完成，先用延时处理
          delay(1000)
        }
        if (postTimes <= autoReleaseC) {
          assertEquals(postTimes, uploadProgressChangedBlock)
        } else {
          assertEquals(autoReleaseC, uploadProgressChangedBlock)
        }

        val removeC = 9
        if (postTimes == removeC) {
          oRemove.remove()
        }
        if (postTimes <= removeC) {
          assertEquals(buildBlock, postTimes)
        } else {
          assertEquals(buildBlock, removeC)
        }
      }

      job.join()
    }
  }

}