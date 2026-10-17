package com.example

import com.example.data.repository.SecurityUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testSha256Hashing() {
    val hash1 = SecurityUtil.hashPassword("secret123")
    val hash2 = SecurityUtil.hashPassword("secret123")
    val hashDiff = SecurityUtil.hashPassword("different456")

    assertEquals(hash1, hash2)
    assertNotEquals(hash1, hashDiff)
    assertEquals(64, hash1.length) // 256 bits = 64 hex chars
  }

  @Test
  fun testUniqueIdFormat() {
    val id = SecurityUtil.generateUniqueId()
    assertTrue("Generated ID should start with HT-", id.startsWith("HT-"))
    assertTrue("Generated ID should match HT-\\d{6}", id.matches(Regex("^HT-\\d{6}$")))
  }

  @Test
  fun testCompletionPercentageCalculation() {
    val total = 10
    val done = 7
    val pct = (done * 100) / total
    assertEquals(70, pct)
  }
}
