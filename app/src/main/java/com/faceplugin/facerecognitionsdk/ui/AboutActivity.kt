package com.faceplugin.facerecognitionsdk.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.faceplugin.facerecognitionsdk.R
import com.google.android.material.appbar.MaterialToolbar

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val openSite = {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.company_website_url))))
        }
        findViewById<ImageView>(R.id.imgAboutLogo).setOnClickListener { openSite() }
        findViewById<TextView>(R.id.txtAboutWebsite).setOnClickListener { openSite() }
    }
}
