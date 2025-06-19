package com.github.xpwu.nc

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SomeView: Observer {
  override val events: ObserverEvents = ObserverEvents()
}

class ObserverUnitTest {

  @Test
  fun key() {
    val nc1 = NC()
    val nc2 = NC()

    assertEquals(ObserverKey(UserInfoChanged::class, nc1), ObserverKey(UserInfoChanged::class, nc1))
    assertEquals(ObserverKey(UserInfoChanged::class, nc2), ObserverKey(UserInfoChanged::class, nc2))
    assertEquals(ObserverKey(Built::class, nc1), ObserverKey(Built::class, nc1))
    assertEquals(ObserverKey(Built::class, nc2), ObserverKey(Built::class, nc2))

    assertNotEquals(ObserverKey(UserInfoChanged::class, nc1), ObserverKey(UserInfoChanged::class, nc2))
    assertNotEquals(ObserverKey(UserInfoChanged::class, nc1), ObserverKey(Built::class, nc2))
    assertNotEquals(ObserverKey(UserInfoChanged::class, nc1), ObserverKey(Built::class, nc1))

  }

  @Test
  fun remove() = runBlocking {
    val nc1 = NC()
    val nc2 = NC()
    val view = SomeView()

    view.removeEvent(UserInfoChanged::class, nc1)
    view.removeEvent(UserInfoChanged::class, nc1)
    view.removeEvent(Built::class, nc1)
    view.removeEvent(UserInfoChanged::class, nc1)

    view.removeEvent(UserInfoChanged::class, nc2)
    view.removeEvent(UserInfoChanged::class, nc2)
    view.removeEvent(Built::class, nc2)
    view.removeEvent(UserInfoChanged::class, nc2)

    view.removeAll()

    assertTrue(true)
  }

  @Test
  fun add() = runBlocking {
    val nc1 = NC()
    val nc2 = NC()
    val view1 = SomeView()
    val view2 = SomeView()

    var uccount = 0

    // add1
    view1.addEvent(UserInfoChanged::class, nc1) {
      uccount += it.ids.size
    }
    // add2
    view2.addEvent(UserInfoChanged::class, nc1) {
      uccount += it.ids.size
    }

    nc1.post(UserInfoChanged(arrayOf("1", "2")))
    assertEquals(4, uccount)
    nc2.post(UserInfoChanged(arrayOf("1", "2")))
    assertEquals(4, uccount)

    // add3 note that: reduplicate, so add1 invalid
    view1.addEvent(UserInfoChanged::class, nc1) {
      uccount += it.ids.size + 1
    }
    nc1.post(UserInfoChanged(arrayOf("1", "2", "3")))
    // 4 + 3(add2) + 4(add3), add1 not work
    assertEquals(11, uccount)

    // add4
    view2.addEvent(UserInfoChanged::class, nc2) {
      uccount += it.ids.size + 2
    }

    nc1.post(UserInfoChanged(arrayOf("1", "2", "3")))
    // 11 + 3(add2) + 4(add3)
    assertEquals(18, uccount)

    nc2.post(UserInfoChanged(arrayOf("1", "2", "3")))
    // 18 + 5(add4)
    assertEquals(23, uccount)

    view2.removeEvent(UserInfoChanged::class, nc1)

    nc1.post(UserInfoChanged(arrayOf("1", "2", "3")))
    // 23 + 4(add3)
    assertEquals(27, uccount)

    nc2.post(UserInfoChanged(arrayOf("1", "2", "3")))
    // 27 + 5(add4)
    assertEquals(32, uccount)

    view1.removeAll()
    view2.removeAll()

    nc1.post(UserInfoChanged(arrayOf("1", "2", "3")))
    assertEquals(32, uccount)
    nc2.post(UserInfoChanged(arrayOf("1", "2", "3")))
    assertEquals(32, uccount)
  }
}


