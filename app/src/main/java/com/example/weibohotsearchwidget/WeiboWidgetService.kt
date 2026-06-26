package com.example.weibohotsearchwidget

import android.content.Intent
import android.widget.RemoteViewsService

class WeiboWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WeiboWidgetFactory(this.applicationContext)
    }
}
