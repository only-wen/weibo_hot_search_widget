package com.example.weibohotsearchwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class WeiboHotSearchWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.example.weibohotsearchwidget.ACTION_REFRESH"
        const val MAX_ITEMS = 8

        val ROW_IDS = intArrayOf(
            R.id.item_row_0, R.id.item_row_1, R.id.item_row_2, R.id.item_row_3,
            R.id.item_row_4, R.id.item_row_5, R.id.item_row_6, R.id.item_row_7
        )
        val RANK_IDS = intArrayOf(
            R.id.item_rank_0, R.id.item_rank_1, R.id.item_rank_2, R.id.item_rank_3,
            R.id.item_rank_4, R.id.item_rank_5, R.id.item_rank_6, R.id.item_rank_7
        )
        val TITLE_IDS = intArrayOf(
            R.id.item_title_0, R.id.item_title_1, R.id.item_title_2, R.id.item_title_3,
            R.id.item_title_4, R.id.item_title_5, R.id.item_title_6, R.id.item_title_7
        )
        val DIVIDER_IDS = intArrayOf(
            R.id.divider_0, R.id.divider_1, R.id.divider_2, R.id.divider_3,
            R.id.divider_4, R.id.divider_5, R.id.divider_6
            // no divider after last item
        )

        data class HotItem(val rank: Int, val title: String)

        fun parseItems(jsonString: String): List<HotItem> {
            val result = mutableListOf<HotItem>()
            try {
                val dataObj = JSONObject(jsonString).optJSONObject("data") ?: return result
                val arr = dataObj.optJSONArray("realtime") ?: return result
                var displayRank = 1
                for (i in 0 until arr.length()) {
                    if (result.size >= MAX_ITEMS) break
                    val obj = arr.getJSONObject(i)
                    val word = obj.optString("word", "")
                    val isAd = obj.optString("is_ad", "0") == "1" || obj.optInt("is_ad", 0) == 1
                    if (word.isEmpty() || isAd) continue
                    result.add(HotItem(displayRank++, word))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        fun applyItemsToViews(context: Context, views: RemoteViews, items: List<HotItem>) {
            for (i in 0 until MAX_ITEMS) {
                if (i < items.size) {
                    val item = items[i]
                    views.setViewVisibility(ROW_IDS[i], View.VISIBLE)
                    views.setTextViewText(RANK_IDS[i], item.rank.toString())
                    views.setTextViewText(TITLE_IDS[i], item.title)

                    // Rank badge background + text colour per rank
                    val badgeRes = when (item.rank) {
                        1 -> R.drawable.weibo_rank_badge_1
                        2 -> R.drawable.weibo_rank_badge_2
                        3 -> R.drawable.weibo_rank_badge_3
                        else -> R.drawable.weibo_rank_badge
                    }
                    views.setInt(RANK_IDS[i], "setBackgroundResource", badgeRes)

                    // Top-3 white text, rest dark gray
                    val rankTextColor = if (item.rank <= 3) Color.WHITE else Color.parseColor("#666666")
                    views.setTextColor(RANK_IDS[i], rankTextColor)

                    // Show divider between items (not after last visible item)
                    if (i < DIVIDER_IDS.size) {
                        val showDivider = i < items.size - 1
                        views.setViewVisibility(DIVIDER_IDS[i], if (showDivider) View.VISIBLE else View.GONE)
                    }

                    // Per-item click: open browser to Weibo search
                    val encoded = Uri.encode(item.title)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://s.weibo.com/weibo?q=$encoded"))
                    val pi = PendingIntent.getActivity(
                        context, i, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(ROW_IDS[i], pi)
                } else {
                    views.setViewVisibility(ROW_IDS[i], View.GONE)
                    if (i < DIVIDER_IDS.size) views.setViewVisibility(DIVIDER_IDS[i], View.GONE)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        val pendingResult = goAsync()
        performRefresh(context, pendingResult)
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, WeiboHotSearchWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val views = RemoteViews(context.packageName, R.layout.weibo_widget_layout)
                views.setTextViewText(R.id.widget_update_time, "刷新中…")
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views)
                val pendingResult = goAsync()
                performRefresh(context, pendingResult)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = buildBaseViews(context)

        val prefs = context.getSharedPreferences("weibo_widget_prefs", Context.MODE_PRIVATE)
        val cachedJson = prefs.getString("hot_search_json", null)
        val lastTime = prefs.getString("last_update_time", null)

        if (!cachedJson.isNullOrEmpty()) {
            applyItemsToViews(context, views, parseItems(cachedJson))
            views.setTextViewText(R.id.widget_update_time, lastTime ?: "--:--")
        } else {
            for (i in 0 until MAX_ITEMS) {
                views.setViewVisibility(ROW_IDS[i], View.GONE)
                if (i < DIVIDER_IDS.size) views.setViewVisibility(DIVIDER_IDS[i], View.GONE)
            }
            views.setTextViewText(R.id.widget_update_time, "--:--")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun buildBaseViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.weibo_widget_layout)
        val refreshIntent = Intent(context, WeiboHotSearchWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPi = PendingIntent.getBroadcast(
            context, 999, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPi)
        return views
    }

    private fun performRefresh(context: Context, pendingResult: PendingResult? = null) {
        thread {
            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null
            var success = false
            var jsonResponse: String? = null
            try {
                val url = URL("https://weibo.com/ajax/side/hotSearch")
                connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    setRequestProperty("Accept", "application/json, text/plain, */*")
                    setRequestProperty("Referer", "https://weibo.com/")
                }
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val sb = StringBuilder()
                    var line = reader.readLine()
                    while (line != null) { sb.append(line); line = reader.readLine() }
                    jsonResponse = sb.toString()
                    success = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                reader?.close()
                connection?.disconnect()
            }

            Handler(Looper.getMainLooper()).post {
                try {
                    val prefs = context.getSharedPreferences("weibo_widget_prefs", Context.MODE_PRIVATE)
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisWidget = ComponentName(context, WeiboHotSearchWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                    if (appWidgetIds.isNotEmpty()) {
                        val views = buildBaseViews(context)
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

                        if (success && jsonResponse != null) {
                            val timeStr = sdf.format(Date())
                            prefs.edit()
                                .putString("hot_search_json", jsonResponse)
                                .putString("last_update_time", timeStr)
                                .apply()
                            applyItemsToViews(context, views, parseItems(jsonResponse))
                            views.setTextViewText(R.id.widget_update_time, timeStr)
                        } else {
                            val cachedJson = prefs.getString("hot_search_json", null)
                            val cachedTime = prefs.getString("last_update_time", "--:--")
                            if (!cachedJson.isNullOrEmpty()) {
                                applyItemsToViews(context, views, parseItems(cachedJson))
                                views.setTextViewText(R.id.widget_update_time, "$cachedTime ✕")
                            } else {
                                for (i in 0 until MAX_ITEMS) {
                                    views.setViewVisibility(ROW_IDS[i], View.GONE)
                                    if (i < DIVIDER_IDS.size) views.setViewVisibility(DIVIDER_IDS[i], View.GONE)
                                }
                                views.setTextViewText(R.id.widget_update_time, "失败")
                            }
                        }
                        appWidgetManager.updateAppWidget(appWidgetIds, views)
                    }
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}
