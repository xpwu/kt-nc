package com.github.xpwu.nc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UploadProgressChanged(val ids: Array<String>): Event

class UserInfoChanged(val ids: Array<String>): Event

class Built(val ids: Array<Int>): Event

class EventUnitTest {
  @Test
  fun equal() {
    assertEquals(UploadProgressChanged::class, UploadProgressChanged::class)
    assertEquals(UserInfoChanged::class, UserInfoChanged::class)
    assertEquals(Built::class, Built::class)

    assertNotEquals(UploadProgressChanged::class, UserInfoChanged::class)
    assertNotEquals(UserInfoChanged::class, Built::class)
    assertNotEquals(UploadProgressChanged::class, Built::class)
  }
}