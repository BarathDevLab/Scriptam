package com.scriptam.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.scriptam.app.R
import com.scriptam.app.ScriptamApp
import com.scriptam.app.data.db.ScriptEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Launched by the launcher when the user places a Scriptam widget on their home screen.
 * Lets the user pick which script to bind to the new widget instance.
 *
 * Contract:
 *  - Must call setResult(RESULT_OK, intent) with EXTRA_APPWIDGET_ID before finishing.
 *  - If the user cancels, call setResult(RESULT_CANCELED) — the widget will be removed.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedScript: ScriptEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immediately set RESULT_CANCELED so if user backs out, widget isn't placed.
        setResult(RESULT_CANCELED)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_config)

        val listView = findViewById<ListView>(R.id.config_script_list)
        val addButton = findViewById<Button>(R.id.config_add_button)

        lifecycleScope.launch {
            val repository = (application as ScriptamApp).scriptRepository
            val scripts = repository.observeAllScripts().first()

            val adapter = ScriptListAdapter(scripts)
            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                selectedScript = scripts[position]
                addButton.isEnabled = true
                adapter.setSelected(position)
            }

            addButton.setOnClickListener {
                val script = selectedScript ?: return@setOnClickListener
                bindWidgetAndFinish(script)
            }
        }
    }

    private fun bindWidgetAndFinish(script: ScriptEntity) {
        // accentColor is stored as a Long ARGB value
        val accentColorInt = script.accentColor.toInt()

        // Save binding in prefs
        WidgetPrefs.saveScriptBinding(
            ctx = this,
            widgetId = appWidgetId,
            scriptId = script.id,
            title = script.title,
            accentColor = accentColorInt
        )

        // Push the initial widget view
        val manager = AppWidgetManager.getInstance(this)
        ScriptWidget.refreshWidget(this, manager, appWidgetId)

        // Return OK to the launcher
        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    // -------------------------------------------------------------------------

    private inner class ScriptListAdapter(
        private val items: List<ScriptEntity>
    ) : ArrayAdapter<ScriptEntity>(this, android.R.layout.simple_list_item_1, items) {

        private var selectedPos = -1

        fun setSelected(pos: Int) {
            selectedPos = pos
            notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val tv = view.findViewById<TextView>(android.R.id.text1)
            tv.text = items[position].title
            tv.textSize = 15f
            view.setBackgroundColor(
                if (position == selectedPos) 0x1A6C63FF else 0x00000000
            )
            tv.setTextColor(0xFFFFFFFF.toInt())
            tv.setPadding(32, 24, 32, 24)
            return view
        }
    }
}
