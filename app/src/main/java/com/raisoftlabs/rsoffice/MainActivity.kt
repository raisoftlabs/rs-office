package com.raisoftlabs.rsoffice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raisoftlabs.rsoffice.ui.RSOfficeApp
import com.raisoftlabs.rsoffice.ui.RSOfficeTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // PDFBox needs its bundled font/metric resources registered before any document opens.
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            RSOfficeTheme {
                RSOfficeApp()
            }
        }
    }
}
