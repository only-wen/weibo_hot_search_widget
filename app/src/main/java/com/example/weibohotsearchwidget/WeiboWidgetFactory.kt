package com.example.weibohotsearchwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeiboWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    data class HotSearchItem(
        val rank: Int,
        val title: String,
        val heat: String,
        val tag: String
    )

    private val items = ArrayList<HotSearchItem>()

    override fun onCreate() {
        // Initial setup
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    // Called on a background thread when notifyAppWidgetViewDataChanged is called
    override fun onDataSetChanged() {
        val prefs = context.getSharedPreferences("weibo_widget_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("hot_search_json", null)
        
        val newItems = ArrayList<HotSearchItem>()
        if (!jsonString.isNullOrEmpty()) {
            try {
                val jsonResponse = JSONObject(jsonString)
                val dataObj = jsonResponse.optJSONObject("data")
                if (dataObj != null) {
                    val realtimeArray = dataObj.optJSONArray("realtime")
                    if (realtimeArray != null) {
                        var displayRank = 1
                        
                        for (i in 0 until realtimeArray.length()) {
                            if (newItems.size >= 15) break

                            val itemObj = realtimeArray.getJSONObject(i)
                            val word = itemObj.optString("word", "")
                            
                            val isAd = itemObj.optString("is_ad", "0") == "1" || itemObj.optInt("is_ad", 0) == 1
                            if (word.isEmpty() || isAd) continue

                            val num = itemObj.optInt("num", 0)
                            val iconDesc = itemObj.optString("icon_desc", "")

                            val heatStr = when {
                                num >= 1000000 -> String.format(Locale.getDefault(), "%.1fM", num / 1000000.0)
                                num >= 10000 -> String.format(Locale.getDefault(), "%.0f万", num / 10000.0)
                                num > 0 -> num.toString()
                                else -> ""
                            }

                            val isGov = itemObj.optInt("is_gov", 0) == 1
                            val isPin = itemObj.optInt("is_pin", 0) == 1
                            
                            val rankLabel: Int
                            val itemTag: String
                            
                            if (isGov || isPin) {
                                rankLabel = 0
                                itemTag = if (isGov) "推荐" else "置顶"
                            } else {
                                rankLabel = displayRank++
                                itemTag = iconDesc
                            }

                            newItems.add(HotSearchItem(rankLabel, word, heatStr, itemTag))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        synchronized(items) {
            items.clear()
            items.addAll(newItems)
        }
    }

    override fun getViewAt(position: Int): RemoteViews? {
        val item = synchronized(items) {
            if (position < 0 || position >= items.size) return null
            items[position]
        }

        val views = RemoteViews(context.packageName, R.layout.weibo_widget_item)

        // 1. Rank presentation
        if (item.rank == 0) {
            views.setTextViewText(R.id.item_rank, "荐")
            views.setInt(R.id.item_rank, "setBackgroundResource", R.drawable.weibo_rank_badge_3)
        } else {
            views.setTextViewText(R.id.item_rank, item.rank.toString())
            when (item.rank) {
                1 -> views.setInt(R.id.item_rank, "setBackgroundResource", R.drawable.weibo_rank_badge_1)
                2 -> views.setInt(R.id.item_rank, "setBackgroundResource", R.drawable.weibo_rank_badge_2)
                3 -> views.setInt(R.id.item_rank, "setBackgroundResource", R.drawable.weibo_rank_badge_3)
                else -> views.setInt(R.id.item_rank, "setBackgroundResource", R.drawable.weibo_rank_badge)
            }
        }

        // 2. Title
        views.setTextViewText(R.id.item_title, item.title)

        // 3. Status Tag (爆, 沸, 热, 新)
        if (item.tag.isNotEmpty()) {
            views.setTextViewText(R.id.item_tag, item.tag)
            views.setViewVisibility(R.id.item_tag, View.VISIBLE)
            
            val tagBg = when (item.tag) {
                "爆", "沸", "置顶" -> R.drawable.weibo_tag_bg_red
                "热" -> R.drawable.weibo_tag_bg_orange
                "新", "推荐" -> R.drawable.weibo_tag_bg_blue
                else -> R.drawable.weibo_tag_bg_orange
            }
            views.setInt(R.id.item_tag, "setBackgroundResource", tagBg)
        } else {
            views.setViewVisibility(R.id.item_tag, View.GONE)
        }

        // 4. Heat Value
        if (item.heat.isNotEmpty()) {
            views.setTextViewText(R.id.item_heat, item.heat)
            views.setViewVisibility(R.id.item_heat, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.item_heat, View.GONE)
        }

        // 5. Fill-in intent: Opens the browser on click of this item
        val encodedWord = Uri.encode(item.title)
        val fillInIntent = Intent().apply {
            data = Uri.parse("https://s.weibo.com/weibo?q=$encodedWord")
        }
        views.setOnClickFillInIntent(R.id.item_root, fillInIntent)

        return views
    }
}
