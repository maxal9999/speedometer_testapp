package com.testapp.speedometer.controls

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.*
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.RemoteException
import android.util.AttributeSet
import android.view.View
import com.testapp.speedometer.service.GenerateService
import com.testapp.speedometer.service.IGenerateService
import com.testapp.speedometer.service.IGenerateServiceCallback
import java.util.*
import kotlin.math.cos
import kotlin.math.sin


/**
 * Speedometer with needle.
 */
class SpeedometerView: View {
    private var maxSpeed = DEFAULT_MAX_SPEED
    var speed = 0.0
        set(speed) {
            var speedTmp = speed
            require(speedTmp >= 0) { "Non-positive value specified as a speed." }
            if (speedTmp > maxSpeed) speedTmp = maxSpeed
            field = speedTmp
            invalidate()
        }
    private var defaultColor = Color.rgb(180, 180, 180)
    private var majorTickStep = DEFAULT_MAJOR_TICK_STEP
    private var minorTicks = DEFAULT_MINOR_TICKS
    private var labelConverter: LabelConverter? =
        null
    private val ranges: MutableList<ColoredRange> =
        ArrayList()
    private var backgroundPaint: Paint? = null
    private var needlePaint: Paint? = null
    private var ticksPaint: Paint? = null
    private var txtPaint: Paint? = null
    private var colorLinePaint: Paint? = null
    private var labelTextSize = 0

    private var generateService: IGenerateService? = null
    private lateinit var handlerService: InternalHandler

    private val callbackService = object : IGenerateServiceCallback.Stub() {
        override fun valueChanged(value: Int) {
            handlerService.sendMessage(handlerService.obtainMessage(1, value, 0))
        }
    }

    private val connectionService: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            generateService = IGenerateService.Stub.asInterface(service)
            try {
                generateService?.registerCallback(callbackService, maxSpeed)
            } catch (e: RemoteException) {
            }

            generateService?.startGenerate()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            generateService?.unregisterCallback()
            generateService = null
        }
    }

    constructor(context: Context?) : super(context) {
        init(context)
        val density = resources.displayMetrics.density
        setLabelTextSize(Math.round(DEFAULT_LABEL_TEXT_SIZE_DP * density))
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
        val density = resources.displayMetrics.density
        setLabelTextSize(Math.round(DEFAULT_LABEL_TEXT_SIZE_DP * density))
    }

    fun setMaxSpeed(maxSpeed: Double) {
        require(maxSpeed > 0) { "Non-positive value specified as max speed." }
        this.maxSpeed = maxSpeed
        invalidate()
    }

    fun setSpeed(progressInput: Double, duration: Long, startDelay: Long) {
        var progress = progressInput
        require(progress >= 0) { "Negative value specified as a speed." }
        if (progress > maxSpeed) progress = maxSpeed
        val valueForAnimate = ValueAnimator.ofObject(
                TypeEvaluator<Double> { fraction, startValue, endValue -> startValue + fraction * (endValue - startValue) },
                java.lang.Double.valueOf(speed),
                java.lang.Double.valueOf(progress)
        )
        valueForAnimate.duration = duration
        valueForAnimate.startDelay = startDelay
        valueForAnimate.addUpdateListener { animation ->
            val value = animation.animatedValue as Double
            speed = value
        }
        valueForAnimate.start()
    }

    fun setDefaultColor(defaultColor: Int) {
        this.defaultColor = defaultColor
        invalidate()
    }

    fun setMajorTickStep(majorTickStep: Double) {
        require(majorTickStep > 0) { "Non-positive value specified as a major tick step." }
        this.majorTickStep = majorTickStep
        invalidate()
    }

    fun setMinorTicks(minorTicks: Int) {
        this.minorTicks = minorTicks
        invalidate()
    }

    fun setLabelConverter(labelConverter: LabelConverter?) {
        this.labelConverter = labelConverter
        invalidate()
    }

    fun clearColoredRanges() {
        ranges.clear()
        invalidate()
    }

    fun addColoredRange(beginInput: Double, endInput: Double, color: Int) {
        var begin = beginInput
        var end = endInput
        require(begin < end) { "Incorrect number range specified!" }
        if (begin < -5.0 / 160 * maxSpeed) begin = -5.0 / 160 * maxSpeed
        if (end > maxSpeed * (5.0 / 160 + 1)) end = maxSpeed * (5.0 / 160 + 1)
        ranges.add(ColoredRange(color, begin, end))
        invalidate()
    }

    fun setLabelTextSize(labelTextSize: Int) {
        this.labelTextSize = labelTextSize
        if (txtPaint != null) {
            txtPaint!!.textSize = labelTextSize.toFloat()
            invalidate()
        }
    }

    fun startGenerate()
    {
        generateService?.startGenerate()
    }

    fun stopGenerate()
    {
        generateService?.stopGenerate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Clear canvas
        canvas.drawColor(Color.TRANSPARENT)

        // Draw Ticks and colored arc
        drawTicks(canvas)

        // Draw Needle
        drawNeedle(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var width: Int
        var height: Int

        //Measure Width
        width = if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            widthSize
        } else {
            -1
        }

        //Measure Height
        height = if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            heightSize
        } else {
            -1
        }
        if (height >= 0 && width >= 0) {
            width = height.coerceAtMost(width)
            height = width / 2
        } else if (width >= 0) {
            height = width / 2
        } else if (height >= 0) {
            width = height * 2
        } else {
            width = 0
            height = 0
        }

        setMeasuredDimension(width, height)
    }

    private fun drawNeedle(canvas: Canvas) {
        val oval = getOval(canvas, 1f)
        val radius = oval.width() * 0.35f + 10
        val smallOval = getOval(canvas, 0.2f)
        val angle = 10 + (speed / maxSpeed * 160).toFloat()
        val cosAngle = cos((180 - angle) / 180 * Math.PI)
        val sinAngle = sin(angle / 180 * Math.PI)
        canvas.drawLine(
                (oval.centerX() + cosAngle * smallOval.width() * 0.5f).toFloat(),
                (oval.centerY() - sinAngle * smallOval.width() * 0.5f).toFloat(),
                (oval.centerX() + cosAngle * radius).toFloat(),
                (oval.centerY() - sinAngle * radius).toFloat(),
                needlePaint!!
        )
        canvas.drawArc(smallOval, 180f, 180f, true, backgroundPaint!!)
    }

    private fun drawTicks(canvas: Canvas) {
        val availableAngle = 160f
        val majorStep = (majorTickStep / maxSpeed * availableAngle).toFloat()
        val minorStep = majorStep / (1 + minorTicks)
        val majorTicksLength = 30f
        val minorTicksLength = majorTicksLength / 2
        val oval = getOval(canvas, 1f)
        val radius = oval.width() * 0.35f
        var currentAngle = 10f
        var curProgress = 0.0
        while (currentAngle <= 170) {
            val cosAngle = cos((180 - currentAngle) / 180 * Math.PI)
            val sinAngle = sin(currentAngle / 180 * Math.PI)
            canvas.drawLine(
                    (oval.centerX() + cosAngle * (radius - majorTicksLength / 2)).toFloat(),
                    (oval.centerY() - sinAngle * (radius - majorTicksLength / 2)).toFloat(),
                    (oval.centerX() + cosAngle * (radius + majorTicksLength / 2)).toFloat(),
                    (oval.centerY() - sinAngle * (radius + majorTicksLength / 2)).toFloat(),
                    ticksPaint!!
            )
            for (i in 1..minorTicks) {
                val angle = currentAngle + i * minorStep
                val minorCosAngle = cos((180 - angle) / 180 * Math.PI)
                val minorSinAngle = sin(angle / 180 * Math.PI)
                if (angle >= 170 + minorStep / 2) {
                    break
                }
                canvas.drawLine(
                        (oval.centerX() + minorCosAngle * radius).toFloat(),
                        (oval.centerY() - minorSinAngle * radius).toFloat(),
                        (oval.centerX() + minorCosAngle * (radius + minorTicksLength)).toFloat(),
                        (oval.centerY() - minorSinAngle * (radius + minorTicksLength)).toFloat(),
                        ticksPaint!!
                )
            }
            if (labelConverter != null) {
                canvas.save()
                canvas.rotate(180 + currentAngle, oval.centerX(), oval.centerY())
                val txtX = oval.centerX() + radius + majorTicksLength / 2 + 8
                val txtY = oval.centerY()
                canvas.rotate(+90f, txtX, txtY)
                canvas.drawText(
                        labelConverter!!.getLabelFor(curProgress, maxSpeed)!!,
                        txtX,
                        txtY,
                        txtPaint!!
                )
                canvas.restore()
            }
            currentAngle += majorStep
            curProgress += majorTickStep
        }
        val smallOval = getOval(canvas, 0.7f)
        colorLinePaint!!.color = defaultColor
        canvas.drawArc(smallOval, 185f, 170f, false, colorLinePaint!!)
        for (range in ranges) {
            colorLinePaint!!.color = range.color
            canvas.drawArc(
                    smallOval,
                    (190 + range.begin / maxSpeed * 160).toFloat(),
                    ((range.end - range.begin) / maxSpeed * 160).toFloat(),
                    false,
                    colorLinePaint!!
            )
        }
    }

    private fun getOval(canvas: Canvas, factor: Float): RectF {
        val oval: RectF
        val canvasWidth = canvas.width - paddingLeft - paddingRight
        val canvasHeight = canvas.height - paddingTop - paddingBottom
        oval = if (canvasHeight * 2 >= canvasWidth) {
            RectF(0f, 0f, canvasWidth * factor, canvasWidth * factor)
        } else {
            RectF(0f, 0f, canvasHeight * 2 * factor, canvasHeight * 2 * factor)
        }
        oval.offset(
                (canvasWidth - oval.width()) / 2 + paddingLeft,
                (canvasHeight * 2 - oval.height()) / 2 + paddingTop
        )
        return oval
    }

    private fun init(context: Context?) {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint!!.style = Paint.Style.FILL
        backgroundPaint!!.color = Color.rgb(127, 127, 127)
        txtPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        txtPaint!!.color = Color.WHITE
        txtPaint!!.textSize = labelTextSize.toFloat()
        txtPaint!!.textAlign = Paint.Align.CENTER
        txtPaint!!.isLinearText = true
        ticksPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        ticksPaint!!.strokeWidth = 3.0f
        ticksPaint!!.style = Paint.Style.STROKE
        ticksPaint!!.color = defaultColor
        colorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        colorLinePaint!!.style = Paint.Style.STROKE
        colorLinePaint!!.strokeWidth = 5f
        colorLinePaint!!.color = defaultColor
        needlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        needlePaint!!.strokeWidth = 5f
        needlePaint!!.style = Paint.Style.STROKE
        needlePaint!!.color = Color.argb(200, 255, 0, 0)

        val intent = Intent(context, GenerateService::class.java)
        context?.bindService(intent, connectionService, Context.BIND_AUTO_CREATE)

        handlerService = InternalHandler { progressInput -> setSpeed(progressInput.toDouble(), 1000, 10) }
    }

    private class InternalHandler(
            private val callbackFunc: (Int) -> Unit
    ) : Handler() {
        override fun handleMessage(msg: Message) {
            callbackFunc(msg.arg1)
        }
    }

    interface LabelConverter {
        fun getLabelFor(progress: Double, maxProgress: Double): String?
    }

    class ColoredRange(var color: Int, var begin: Double, var end: Double)

    companion object {
        const val DEFAULT_MAX_SPEED = 100.0
        const val DEFAULT_MAJOR_TICK_STEP = 20.0
        const val DEFAULT_MINOR_TICKS = 1
        const val DEFAULT_LABEL_TEXT_SIZE_DP = 12
    }
}