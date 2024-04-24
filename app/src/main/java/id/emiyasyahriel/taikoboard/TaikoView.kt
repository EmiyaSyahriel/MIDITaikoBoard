package id.emiyasyahriel.taikoboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

@Suppress("SameParameterValue")
class TaikoView : View {

    private var buttonList : ArrayList<ButtonState> = arrayListOf()
    private var buttonSize = 100
    private var colors = getWhite(1)
    private var rectBuffer = Rect(0,0,0,0)
    private var density = 1f
    private fun d(f:Float):Float = f * density
    fun d(f:Int):Int = (f * density).roundToInt()
    private var rotate = false

    private var colorOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 50f
    }
    private var colorFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    private val _act get()= context as MainActivity

    constructor(context: Context):super(context){
        init()
    }
    constructor(context: Context, attributeSet: AttributeSet):super(context,attributeSet){
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet, styleRes:Int):super(context,attributeSet,styleRes){
        init()
    }

    private fun initStateList(){
        buttonList.clear()
        for(i in 0 .. AppState.keyCount){
            buttonList.add(ButtonState(-1))
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

    private fun onTouchDown(pos: PointF, id:Int){
        val xPos = (pos.x / buttonSize).floorToInt()
        if(xPos < buttonList.size && buttonList[xPos].pointerId == -1)
        {
            buttonList[xPos].pointerId = id
        }
        _act.onDown(xPos)
    }

    private fun onTouchUp(id:Int){
        buttonList.forEachIndexed {i, it ->
            if(it.pointerId == id){
                it.pointerId = -1
                _act.onUp(i)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buttonSize = (width / AppState.keyCount)
        rotate = width < height
        initDisplaySize()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var retval = false
        val pId = event.getPointerId(event.actionIndex)
        val pIn = event.findPointerIndex(pId)
        val pX = event.getX(pIn)
        val pY = event.getY(pIn)
        val pA = event.actionMasked
        val pos = PointF(pX,pY)
        when(pA){
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_HOVER_ENTER -> {
                onTouchDown(pos, pId)
                retval = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_HOVER_EXIT -> {
                onTouchUp(pId)
                retval = true
            }
        }
        postInvalidate()
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
            if(buttonList[i].pointerId != -1){
                colorFill.alpha = 100
                colorFill.color = Color.argb(100,0,0,0)
                canvas.drawRect(rectBuffer, colorFill)
                colorFill.alpha = 255
            }

            canvas.drawRect(rectBuffer,colorOutline)
        }
    }
}

