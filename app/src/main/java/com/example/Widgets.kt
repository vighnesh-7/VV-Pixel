package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.RemoteViews
import java.io.Serializable

class LockWidget2x1 : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            Log.d("LockWidget2x1", "Updating 2x1 Lock Widget ID: $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.lock_widget_layout)

            // Intent to trigger immediate device lock via our Accessibility Service broadcast
            val lockIntent = Intent(VVPixelAccessibilityService.ACTION_LOCK)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                lockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(R.id.widget_lock_container, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class DoubleTapLockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.double_tap_widget_layout)

            // Router Intent that sends back to this provider, allowing us to capture click times
            val tapIntent = Intent(context, DoubleTapLockWidget::class.java).apply {
                action = ACTION_DOUBLE_TAP_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                tapIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(R.id.widget_double_tap_container, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_DOUBLE_TAP_CLICK) {
            val currentTime = System.currentTimeMillis()
            val lastTime = lastClickTime

            lastClickTime = currentTime

            val timeDiff = currentTime - lastTime
            Log.d("DoubleTapWidget", "Tap registered. Time since last: ${timeDiff}ms")

            // If tapped consecutively within 350ms, trigger security lock broadcast
            if (timeDiff in 50..350) {
                Log.d("DoubleTapWidget", "Double tap verified! Locking screen.")
                val lockIntent = Intent(VVPixelAccessibilityService.ACTION_LOCK)
                context.sendBroadcast(lockIntent)
            }
        }
    }

    companion object {
        const val ACTION_DOUBLE_TAP_CLICK = "com.example.vvpixel.ACTION_DOUBLE_TAP_CLICK"
        private var lastClickTime = 0L
    }
}

class CalculatorWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetContent(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action.startsWith(ACTION_CALC_KEY_PREFIX)) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

            val keyValue = action.substring(ACTION_CALC_KEY_PREFIX.length)
            Log.d("CalculatorWidget", "Calculator Key Clicked: $keyValue on Widget: $appWidgetId")

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var expr = prefs.getString(KEY_EXPR + appWidgetId, "") ?: ""
            var lastResult = prefs.getString(KEY_RESULT + appWidgetId, "0") ?: "0"
            var isAfterEquals = prefs.getBoolean(KEY_AFTER_EQUALS + appWidgetId, false)

            when (keyValue) {
                "CLEAR" -> {
                    expr = ""
                    lastResult = "0"
                    isAfterEquals = false
                }
                "DEL" -> {
                    if (isAfterEquals) {
                        expr = ""
                        lastResult = "0"
                        isAfterEquals = false
                    } else {
                        if (expr.isNotEmpty()) {
                            // If it ends with a space (operator is surrounded by space, i.e., " + ")
                            if (expr.endsWith(" ")) {
                                if (expr.length >= 3) {
                                    expr = expr.substring(0, expr.length - 3)
                                } else {
                                    expr = ""
                                }
                            } else {
                                expr = expr.substring(0, expr.length - 1)
                            }
                        }
                        if (expr.isEmpty()) {
                            lastResult = "0"
                        } else {
                            val trimmedEnd = expr.trim()
                            val endsWithOperator = trimmedEnd.endsWith("+") || trimmedEnd.endsWith("−") || trimmedEnd.endsWith("×") || trimmedEnd.endsWith("÷") || trimmedEnd.endsWith("x")
                            if (endsWithOperator) {
                                val prefix = trimmedEnd.substring(0, trimmedEnd.length - 1).trim()
                                lastResult = evaluateExpression(prefix)
                            } else {
                                lastResult = evaluateExpression(expr)
                            }
                        }
                    }
                }
                "EQ" -> {
                    if (expr.isNotEmpty()) {
                        val result = evaluateExpression(expr)
                        lastResult = result
                        expr = result
                        isAfterEquals = true
                    }
                }
                "DIV", "MUL", "SUB", "ADD" -> {
                    val op = when (keyValue) {
                        "DIV" -> " ÷ "
                        "MUL" -> " × "
                        "SUB" -> " − "
                        "ADD" -> " + "
                        else -> " + "
                    }
                    if (isAfterEquals) {
                        // Persist the result and chain operator
                        isAfterEquals = false
                    }
                    
                    val trimmed = expr.trim()
                    val endsWithOperator = trimmed.endsWith("+") || trimmed.endsWith("−") || trimmed.endsWith("×") || trimmed.endsWith("÷") || trimmed.endsWith("x")
                    if (endsWithOperator) {
                        var idx = expr.length - 1
                        while (idx >= 0 && (expr[idx] == ' ' || expr[idx].toString() in listOf("+", "−", "×", "÷", "x"))) {
                            idx--
                        }
                        expr = if (idx >= 0) {
                            expr.substring(0, idx + 1) + op
                        } else {
                            op
                        }
                    } else {
                        expr += op
                    }

                    // On operator entry, evaluate the intermediate expression so far
                    lastResult = evaluateExpression(expr)
                }
                else -> {
                    // It is a digit: 0..9 or "."
                    if (isAfterEquals) {
                        expr = keyValue
                        isAfterEquals = false
                        lastResult = evaluateExpression(expr)
                    } else {
                        if (expr.startsWith("Error")) {
                            expr = ""
                        }
                        expr += keyValue
                        lastResult = evaluateExpression(expr)
                    }
                }
            }

            prefs.edit().apply {
                putString(KEY_EXPR + appWidgetId, expr)
                putString(KEY_RESULT + appWidgetId, lastResult)
                putBoolean(KEY_AFTER_EQUALS + appWidgetId, isAfterEquals)
                apply()
            }

            // Sync update
            val appWidgetManager = AppWidgetManager.getInstance(context)
            updateWidgetContent(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidgetContent(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.calculator_widget_layout)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expr = prefs.getString(KEY_EXPR + appWidgetId, "") ?: ""
        val lastResult = prefs.getString(KEY_RESULT + appWidgetId, "0") ?: "0"

        views.setTextViewText(R.id.calc_expr_text, if (expr.isEmpty()) "0" else expr)
        
        val displayResult = when {
            lastResult.isEmpty() -> "0"
            lastResult == "0" -> "= 0"
            lastResult.startsWith("Error") -> lastResult
            else -> "= $lastResult"
        }
        views.setTextViewText(R.id.calc_result_text, displayResult)

        // Bind pending intents to buttons
        val keys = mapOf(
            R.id.calc_btn_7 to "7", R.id.calc_btn_8 to "8", R.id.calc_btn_9 to "9", R.id.calc_btn_div to "DIV",
            R.id.calc_btn_4 to "4", R.id.calc_btn_5 to "5", R.id.calc_btn_6 to "6", R.id.calc_btn_mul to "MUL",
            R.id.calc_btn_1 to "1", R.id.calc_btn_2 to "2", R.id.calc_btn_3 to "3", R.id.calc_btn_sub to "SUB",
            R.id.calc_btn_clear to "CLEAR", R.id.calc_btn_0 to "0", R.id.calc_btn_eq to "EQ", R.id.calc_btn_add to "ADD",
            R.id.calc_btn_back to "DEL", R.id.calc_btn_dot to "."
        )

        for ((btnId, keyVal) in keys) {
            val keyIntent = Intent(context, CalculatorWidget::class.java).apply {
                action = ACTION_CALC_KEY_PREFIX + keyVal
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                btnId + appWidgetId, // ensure unique request code per widget/key
                keyIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(btnId, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun evaluateExpression(expr: String): String {
        try {
            val sanitized = expr.replace("÷", "/").replace("×", "*").replace("x", "*").replace("−", "-").trim()
            if (sanitized.isEmpty()) return "0"

            val tokens = mutableListOf<String>()
            var currentNum = StringBuilder()
            
            for (char in sanitized) {
                if (char in "+-*/") {
                    if (currentNum.isNotEmpty()) {
                        tokens.add(currentNum.toString())
                        currentNum = StringBuilder()
                    }
                    tokens.add(char.toString())
                } else if (char != ' ') {
                    currentNum.append(char)
                }
            }
            if (currentNum.isNotEmpty()) {
                tokens.add(currentNum.toString())
            }

            if (tokens.isEmpty()) return "0"

            // Phase 1: MD (Multiplications and Divisions)
            var i = 0
            while (i < tokens.size) {
                if (tokens[i] == "*" || tokens[i] == "/") {
                    val op = tokens[i]
                    val prev = tokens[i - 1].toDoubleOrNull() ?: 0.0
                    val next = tokens[i + 1].toDoubleOrNull() ?: 0.0
                    val res = if (op == "*") {
                        prev * next
                    } else {
                        if (next == 0.0) return "Error (div by 0)"
                        prev / next
                    }
                    tokens[i - 1] = res.toString()
                    tokens.removeAt(i)
                    tokens.removeAt(i)
                    i--
                }
                i++
            }

            // Phase 2: AS (Additions and Subtractions)
            var total = tokens.firstOrNull()?.toDoubleOrNull() ?: 0.0
            i = 1
            while (i < tokens.size) {
                val op = tokens[i]
                val nextVal = tokens.getOrNull(i + 1)?.toDoubleOrNull() ?: 0.0
                if (op == "+") {
                    total += nextVal
                } else if (op == "-") {
                    total -= nextVal
                }
                i += 2
            }

            return if (total % 1.0 == 0.0) {
                total.toLong().toString()
            } else {
                String.format("%.4f", total).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            return "Error"
        }
    }

    companion object {
        const val PREFS_NAME = "com.example.vvpixel.CALC_WIDGET_PREFS"
        const val KEY_EXPR = "expr_"
        const val KEY_RESULT = "result_"
        const val ACTION_CALC_KEY_PREFIX = "com.example.vvpixel.CALC_KEY_"
        const val KEY_AFTER_EQUALS = "after_equals_"
    }
}
