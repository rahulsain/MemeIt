package com.rahuls.memeit.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.rahuls.memeit.R

class Spalshscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spalshscreen)
        Handler().postDelayed({
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        },1600)

    }
}