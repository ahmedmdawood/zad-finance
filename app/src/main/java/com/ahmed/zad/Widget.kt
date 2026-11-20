package com.ahmed.zad

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Home-screen widget: net worth at a glance, respecting privacy mode. */
class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context, manager, ids)
    }

    companion object {

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BalanceWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) updateAll(context, manager, ids)
        }

        private fun updateAll(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val prefs = ZadPrefs(context)
            val hidden = prefs.privacyHidden
            val text = if (hidden) {
                MASKED_MONEY
            } else {
                runBlocking(Dispatchers.IO) {
                    val dao = ZadDatabase.get(context).dao()
                    val accounts = dao.accountsFlow().first()
                    val transactions = dao.transactionsFlow().first()
                    val debts = dao.debtsFlow().first()
                    var assets = 0L
                    for (a in accounts) {
                        if (a.type != "CREDIT") assets = Math.addExact(assets, accountBalance(a, transactions))
                    }
                    for (d in debts) {
                        if (d.direction == "LENT" && !d.settled) assets = Math.addExact(assets, d.remainingPiasters)
                    }
                    var liabilities = 0L
                    for (a in accounts) {
                        if (a.type == "CREDIT") {
                            val owed = accountBalance(a, transactions)
                            if (owed > 0) liabilities = Math.addExact(liabilities, owed)
                        }
                    }
                    for (d in debts) {
                        if (d.direction == "BORROWED" && !d.settled) {
                            liabilities = Math.addExact(liabilities, d.remainingPiasters)
                        }
                    }
                    formatMoney(assets - liabilities)
                }
            }
            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_balance)
                views.setTextViewText(R.id.widget_amount, text)
                val openIntent = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, openIntent)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
