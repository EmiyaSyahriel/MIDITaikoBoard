package id.emiyasyahriel.taikoboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class TaikoView : View {

    var buttonList : ArrayList<Boolean> = arrayListOf()
    var buttonSize = 100
    var colors = getWhite(1)
    var rectBuffer = Rect(0,0,0,0)
    var density = 1f
    fun d(f:Float):Float = f * density
    fun d(f:Int):Int = (f * density).roundToInt()
    var rotate = false
    var onKeyEvent : ArrayList<ITaikoViewReceiver> = arrayListOf()

    var colorOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 50f
    }
    var colorFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    constructor(context: Context):super(context){
        init()
    }
    constructor(context: Context, attributeSet: AttributeSet):super(context,attributeSet){
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet, styleRes:Int):super(context,attributeSet,styleRes){
        init()
    }

    fun addEventListener(v:ITaikoViewReceiver){ onKeyEvent.add(v) }
    fun removeEventListener(v:ITaikoViewReceiver){ onKeyEvent.remove(v) }

    private fun initStateList(){
        buttonList.clear()
        for(i in 0 .. AppState.keyCount){
            buttonList.add(false)
        }
    }

    private fun initStateColors(){
        colors = if(AppState.keyCount < AppState.colors.size) AppState.colors[(AppState.keyCount-1)] else getWhite(AppState.keyCount)
    }

    private fun initDisplaySize(){
        density = context.resources.displayMetrics.scaledDensity
        colorOutline.strokeWidth = d(2f)
    }

    private fun init(){
        initStateList()
        buttonSize = (width / AppState.keyCount)
        initStateColors()
        initDisplaySize()
    }

    private fun onTouchDown(pos: PointF){
        val xPos = (pos.x / buttonSize).floorToInt()
        if(xPos < buttonList.size){ buttonList[xPos] = true }
        onKeyEvent.forEach { it.onDown(xPos) }
    }

    private fun onTouchUp(pos: PointF){
        val xPos = (pos.x / buttonSize).floorToInt()
        if(xPos < buttonList.size){ buttonList[xPos] = false }
        onKeyEvent.forEach { it.onUp(xPos) }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buttonSize = (width / AppState.keyCount)
        rotate = width < height
        initDisplaySize()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var retval = false
        for(i in 0 until event.pointerCount){
            var pId = event.getPointerId(i)
            val pX = event.getX(i)
            val pY = event.getY(i)
            val pA = event.actionMasked
            val pos = PointF(pX,pY)
            when(pA){
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_HOVER_ENTER -> {
                    onTouchDown(pos)
                    retval = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_HOVER_EXIT -> {
                    onTouchUp(pos)
                    retval = true
                }
            }
        }
        return retval || performClick() || super.onTouchEvent(event)
    }

    private fun getWhite(size:Int):ArrayList<Int>{
        val retval = ArrayList<Int>()
        for(i in 0 until size) retval.add(AppState.whiteColor)
        return retval
    }
    private fun getKeyColor(c:Int):Int{
        return when(c){
            0 -> {AppState.whiteColor}
            1 -> {AppState.magentaColor}
            2 -> {AppState.yellowColor}
            else -> {Color.MAGENTA}
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var left: Int
        var right: Int
        for(i in 0 until AppState.keyCount){
            colorFill.color = getKeyColor(colors[i])
            left = buttonSize * i
            right = buttonSize * (i + 1)

            rectBuffer.set(left,0,right,height)

            canvas.drawRect(rectBuffer,colorFill)

            // Tapped down effect
            if(buttonList[i]){
                colorFill.alpha = 100
                colorFill.color = Color.argb(100,0,0,0)
                canvas.drawRect(rectBuffer, colorFill)
                colorFill.alpha = 255
            }

            canvas.drawRect(rectBuffer,colorOutline)
        }
        postInvalidate()
    }
}

