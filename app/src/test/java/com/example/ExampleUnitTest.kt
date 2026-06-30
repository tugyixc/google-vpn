package com.example

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testWarpConfigGeneration() {
    try {
      println("=== STARTING RAW FETCH ===")
      val client = okhttp3.OkHttpClient()
      val request = okhttp3.Request.Builder()
        .url("https://tugyi.val.run/")
        .get()
        .build()
      client.newCall(request).execute().use { response ->
        val rawBody = response.body?.string() ?: ""
        println("RAW BODY:")
        println(rawBody)
      }
      println("=== STARTING GENERATION TEST ===")
      val config = com.example.network.WarpConfigGenerator.generate(object : com.example.network.WarpConfigGenerator.ProgressListener {
        override fun onProgress(message: String) {
          println("[PROGRESS] $message")
        }
      })
      println("=== GENERATION RESULT ===")
      println("PrivateKey: ${config.privateKey}")
      println("Endpoint: ${config.endpoint}")
      println("IPv4 Address: ${config.ipv4Address}")
      println("IPv6 Address: ${config.ipv6Address}")
      println("Config Text length: ${config.configText.length}")
      println("=========================")
    } catch (e: Exception) {
      println("Error during generation: ${e.message}")
      e.printStackTrace()
    }
  }
}
