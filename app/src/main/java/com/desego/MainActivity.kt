package com.desego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.desego.navigation.AppNavigation
import com.desego.ui.theme.DemoApiDesegoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialTableToken = intent?.data?.let { uri ->
            if (uri.scheme == "desego" && uri.host == "mesa") uri.lastPathSegment else null
        }
        setContent {
            DemoApiDesegoTheme {
                AppNavigation(initialTableToken = initialTableToken)
            }
        }
    }
}