package com.example.weibohotsearchwidget.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.weibohotsearchwidget.R
import com.example.weibohotsearchwidget.WeiboHotSearchWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface DataRepository {
  val data: Flow<List<String>>
}

class DefaultDataRepository(private val context: Context) : DataRepository {
  override val data: Flow<List<String>> = flow {
    val list = mutableListOf<String>()
    var connection: HttpURLConnection? = null
    var reader: BufferedReader? = null
    try {
      val url = URL("https://weibo.com/ajax/side/hotSearch")
      connection = url.openConnection() as HttpURLConnection
      connection.apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        setRequestProperty("Accept", "application/json, text/plain, */*")
        setRequestProperty("Referer", "https://weibo.com/")
      }

      if (connection.responseCode == HttpURLConnection.HTTP_OK) {
        val stream = connection.inputStream
        reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        val responseBuilder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
          responseBuilder.append(line)
          line = reader.readLine()
        }

        val jsonString = responseBuilder.toString()
        val jsonResponse = JSONObject(jsonString)
        val dataObj = jsonResponse.optJSONObject("data")
        if (dataObj != null) {
          val realtimeArray = dataObj.optJSONArray("realtime")
          if (realtimeArray != null) {
            for (i in 0 until realtimeArray.length()) {
              val itemObj = realtimeArray.getJSONObject(i)
              val word = itemObj.optString("word", "")
              val isAd = itemObj.optString("is_ad", "0") == "1" || itemObj.optInt("is_ad", 0) == 1
              if (word.isNotEmpty() && !isAd) {
                list.add(word)
              }
            }

            // Cooperative Caching: write to SharedPreferences for the widget
            val prefs = context.getSharedPreferences("weibo_widget_prefs", Context.MODE_PRIVATE)
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeStr = "最后更新: " + sdf.format(Date())
            prefs.edit()
                .putString("hot_search_json", jsonString)
                .putString("last_update_time", timeStr)
                .apply()

            // Notify widget to update immediately using flat layout approach
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, WeiboHotSearchWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val views = RemoteViews(context.packageName, R.layout.weibo_widget_layout)
                val items = WeiboHotSearchWidgetProvider.parseItems(jsonString)
                WeiboHotSearchWidgetProvider.applyItemsToViews(context, views, items)
                views.setTextViewText(R.id.widget_update_time, timeStr)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            }
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      reader?.close()
      connection?.disconnect()
    }

    if (list.isEmpty()) {
      emit(listOf("加载失败，请检查网络或点击刷新"))
    } else {
      emit(list)
    }
  }.flowOn(Dispatchers.IO)
}
