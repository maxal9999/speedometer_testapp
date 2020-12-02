package com.testapp.speedometer

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.codemonkeylabs.fpslibrary.TinyDancer
import com.testapp.speedometer.controls.SpeedometerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var speedometer: SpeedometerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        speedometer = findViewById<SpeedometerView>(R.id.speedometer)
        speedometer?.setMaxSpeed(220.0)
        speedometer?.setLabelConverter(object : SpeedometerView.LabelConverter {
            override fun getLabelFor(progress: Double, maxProgress: Double): String {
                return progress.toInt().toString()
            }
        })
        speedometer?.setMajorTickStep(20.0)
        speedometer?.setMinorTicks(1)
        speedometer?.addColoredRange(0.0, 120.0, Color.GREEN)
        speedometer?.addColoredRange(120.0, 180.0, Color.YELLOW)
        speedometer?.addColoredRange(180.0, 220.0, Color.RED)
        speedometer?.setSpeed(210.0, 1000, 10)

        btnStopGenerate.setOnClickListener {
            speedometer?.stopGenerate()
            btnStartGenerate.isEnabled = true
            btnStopGenerate.isEnabled = false
        }

        btnStartGenerate.setOnClickListener {
            speedometer?.startGenerate()
            btnStartGenerate.isEnabled = false
            btnStopGenerate.isEnabled = true
        }

        // For measuring FPS
        TinyDancer.create()
                .redFlagPercentage(.1f)
                .startingGravity(Gravity.TOP)
                .startingXPosition(200)
                .startingYPosition(600)
                .show(this);
    }

    override fun onDestroy() {
        super.onDestroy()
        speedometer?.clearColoredRanges()
        speedometer?.stopGenerate()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }
}