package com.scriptam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.scriptam.app.ui.navigation.ScriptamNavGraph
import com.scriptam.app.ui.theme.ScriptamTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScriptamTheme {
                ScriptamNavGraph()
            }
        }
    }
}
