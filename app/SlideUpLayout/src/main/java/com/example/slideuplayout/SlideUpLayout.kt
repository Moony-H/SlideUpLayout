package com.example.slideuplayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs

class SlideUpLayout: ViewGroup {

    //attrs
    private var mSlideLayoutExposureHeight = -1
    private var mSlideLayoutShadowColor = -1
    private var mSlideLayoutMaxHeight = -1
    private var mScrollViewResId = -1

    //Views
    private lateinit var mMainView: View
    private lateinit var mSlideView: View
    private lateinit var mScrollView: View


    private val mViewConfiguration = ViewConfiguration.get(context)
    private var isFirst = true
    private var prevY: Float = 0.0f
    private var isSliding = false
    private var isSlideViewTouched = false
    private val touchSlop = mViewConfiguration.scaledTouchSlop

    private var slideEnabled = true

    //slideViewState
    private var slideViewState = 0
    private var slideViewReleased = false

    //helper
    private val mViewDragHelper: ViewDragHelper
    private var mVelocityTracker: VelocityTracker? = null

    //others
    private var maxVelocity = 0F
    private val fling = 10000
    private var isScrollViewTop = false
    private var nonTouchArea = listOf<Rect>()
    private var isScrollViewTouched = false

    private var onSlideViewCaptured = { state: Int -> }
    private var onSlideViewReleased = { state: Int -> }
    private var onSlideViewSettled = { state: Int -> }
    private var mOnLayout = {}

    private var forcedSlideViewToTop=false
    private var forcedSlideViewToBottom=false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)


    constructor(context: Context, attrs: AttributeSet?, style: Int) : super(context, attrs, style) {
        Log.d("SlideLayout", "created")
        attrs?.let {
            val typeArray = context.obtainStyledAttributes(attrs, R.styleable.SlideLayout)
            mSlideLayoutExposureHeight = typeArray.getDimensionPixelSize(
                R.styleable.SlideLayout_moony_SlideLayout_exposureHeight,
                164
            )
            mSlideLayoutMaxHeight = typeArray.getDimensionPixelOffset(
                R.styleable.SlideLayout_moony_SlideLayout_maxHeight,
                1500
            )
            mSlideLayoutShadowColor =
                typeArray.getColor(R.styleable.SlideLayout_moony_SlideLayout_color, -1)
            mScrollViewResId =
                typeArray.getResourceId(R.styleable.SlideLayout_moony_SlideLayout_scrollViewId, -1)
            slideEnabled =
                typeArray.getBoolean(R.styleable.SlideLayout_moony_SlideLayout_slideEnabled, true)
            typeArray.recycle()
        }
        mViewDragHelper = ViewDragHelper.create(this, 0.1f, ViewDragHelperCallback())


    }



    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val childCount=this.childCount
        if(childCount!=2){
            throw IllegalStateException("SlideUpLayout's child number must be two")
        }
        mMainView = getChildAt(0)
        mSlideView = getChildAt(1)
        if (mScrollViewResId != -1)
            mScrollView = findViewById(mScrollViewResId)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSpec = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpec = MeasureSpec.getSize(heightMeasureSpec)

        //이 레이아웃의 width 와 height 가 match_parent 인지 확인.
        if (widthMode != MeasureSpec.EXACTLY)
            throw IllegalStateException("width 를 무조건 match_parent 아니면 정확한 치수로 해주세요 width must have match_parent or exact value")
        if (heightMode != MeasureSpec.EXACTLY)
            throw IllegalStateException("height 를 무조건 match_parent 아니면 정확한 치수로 해주세요 height must have match_parent or exact value")


        //이 레이아웃의 자식 뷰가 두개인지 확인.
        val child = childCount
        if (child != 2)
            throw IllegalStateException("이 레이아웃은 자식뷰가 2개 이어야 합니다.")


        //자식뷰도 match_parent 인지 확인 후, 사이즈 정해줌.
        val mainLayoutParams = mMainView.layoutParams
        val slideLayoutParams = mSlideView.layoutParams
        val matchParent = LayoutParams.MATCH_PARENT
        mainLayoutParams?.let {
            if (it.height != matchParent || it.width != matchParent)
                throw IllegalStateException("자식 레이아웃도 match_parent 이어야 합니다.")
            else {
                val mainLayoutWidthSpec =
                    MeasureSpec.makeMeasureSpec(widthSpec, MeasureSpec.EXACTLY)
                val mainLayoutHeightSpec =
                    MeasureSpec.makeMeasureSpec(heightSpec, MeasureSpec.EXACTLY)
                mMainView.measure(mainLayoutWidthSpec, mainLayoutHeightSpec)
            }
        }
        slideLayoutParams?.let {
            if (it.width != matchParent || it.height != matchParent)
                throw IllegalStateException("자식 레이아웃도 match_parent 이어야 합니다.")
            else {
                val slideLayoutWidthSpec =
                    MeasureSpec.makeMeasureSpec(widthSpec, MeasureSpec.EXACTLY)
                val slideLayoutHeightSpec =
                    MeasureSpec.makeMeasureSpec(mSlideLayoutMaxHeight, MeasureSpec.EXACTLY)
                mSlideView.measure(slideLayoutWidthSpec, slideLayoutHeightSpec)
            }
        }


        //클릭 이벤트 소비
        mSlideView.isClickable = true




        setMeasuredDimension(widthSpec, heightSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        val mainL = left + paddingLeft
        val mainR = mainL + mMainView.measuredWidth - paddingRight
        val mainT = 0 - paddingTop
        val mainB = mainT + mMainView.measuredHeight + paddingBottom
        mMainView.layout(mainL, mainT, mainR, mainB)

        if (isFirst) {

            val slideL = left
            val slideR = slideL + mSlideView.measuredWidth
            val slideT = mMainView.measuredHeight - mSlideLayoutExposureHeight
            val slideB = slideT + mSlideView.measuredHeight
            mSlideView.layout(slideL, slideT, slideR, slideB)
            isFirst = false
            mOnLayout()
        } else {
            val slideL = left
            val slideR = slideL + mSlideView.measuredWidth
            val slideT = mSlideView.top
            val slideB = slideT + mSlideView.measuredHeight
            mSlideView.layout(slideL, slideT, slideR, slideB)
        }


    }

    override fun onDraw(canvas: Canvas?) {

        super.onDraw(canvas)
    }


    @SuppressLint("Recycle", "ClickableViewAccessibility")
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        //단 한번이라도 return true를 하면 여기서는 MOVE,Up 을 처리하지 않는다.
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isNonTouchArea(ev.x, ev.y)) {
                    return super.onInterceptTouchEvent(ev)
                }
                maxVelocity = 0F
                //속도 트랙커 초기화
                mVelocityTracker?.clear()
                //속도 트랙커가 null 이면 생성.
                mVelocityTracker = mVelocityTracker ?: VelocityTracker.obtain()

                //무브먼트 추가
                mVelocityTracker?.addMovement(ev)
                //initialize prevY

                isScrollViewTouched = isInScrollView(ev.x, ev.y)

                prevY = ev.y



                if (mSlideView.top <= prevY)
                    isSlideViewTouched = true



                return super.onInterceptTouchEvent(ev)

            }
            MotionEvent.ACTION_MOVE -> {

                if (isNonTouchArea(ev.x, ev.y)) {
                    return super.onInterceptTouchEvent(ev)
                }

                if (!slideEnabled)
                    return super.onInterceptTouchEvent(ev)

                if (!isSliding) {
                    //슬라이딩 중이 아니면 슬라이딩을 검사.

                    val y = ev.y
                    var dy = prevY - y
                    isScrollViewTop = scrollViewTopState()



                    if (!isSlideViewTouched)
                        return super.onInterceptTouchEvent(ev)

                    if (isScrollViewTouched && !isScrollViewTop) {
                        return super.onInterceptTouchEvent(ev)
                    }

                    if (isScrollViewTop && dy > 0 && slideViewState == 1) {
                        //스크롤 뷰를 끝까지 올리기만 하면 무조건 인터셉트 되기 때문에
                        //초기 상태의 스크롤 뷰가 내려가지 않음. 그러니까 그걸 방지.
                        return super.onInterceptTouchEvent(ev)
                    }
                    dy = abs(dy)

                    if (dy >= touchSlop) {

                        isSliding = true
                        //캡쳐 할 수 있게 강제 ACTION_DOWN event 설정.
                        val forcedEvent = MotionEvent.obtain(ev)
                        forcedEvent.action = MotionEvent.ACTION_DOWN
                        mViewDragHelper.processTouchEvent(forcedEvent)
                        return true
                    } else {
                        isSliding = false
                        return super.onInterceptTouchEvent(ev)
                    }
                } else {
                    //슬라이딩 중이 아니면(어차피 isSliding 은 false)
                    return isSliding
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {


                isSliding = false
                isSlideViewTouched = false
                isScrollViewTouched = false

                return mViewDragHelper.shouldInterceptTouchEvent(ev)


            }


            else -> {
                return mViewDragHelper.shouldInterceptTouchEvent(ev)
            }
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //return true 를 해야 ViewDragHelper 가 동작한다.
        mViewDragHelper.processTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isNonTouchArea(event.x, event.y))
                    return super.onTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerId = event.getPointerId(event.actionIndex)
                mVelocityTracker?.addMovement(event)
                //millis
                mVelocityTracker?.computeCurrentVelocity(1000)
                mVelocityTracker?.let {
                    //velocity 최대값
                    if (abs(maxVelocity) < abs(it.getYVelocity(pointerId)))
                        maxVelocity = it.getYVelocity(pointerId)
                }


            }
            MotionEvent.ACTION_UP -> {


                //슬라이딩과 터치 된지 확인한거 초기화

                isSliding = false
                isSlideViewTouched = false


            }
        }

        return super.onTouchEvent(event)

    }

    override fun computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        } else {

            if (slideViewReleased) {
                onSlideViewSettled(slideViewState)
            }
        }
    }


    inner class ViewDragHelperCallback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {

            return child == mSlideView
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            onSlideViewCaptured(slideViewState)
            slideViewReleased = false

            super.onViewCaptured(capturedChild, activePointerId)

        }


        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            Log.d("test","onViewReleased")
            val percentage = getPercentage(mSlideView.top)
            slideViewReleased = true
            var up = false
            var down = false
            if (fling <= abs(maxVelocity)) {
                if (maxVelocity < 0)
                    up = true
                else
                    down = true
            }
            if(up || percentage>=50 || forcedSlideViewToTop){
                forcedSlideViewToTop=false
                setSlideViewToTop()
            }else if(down ||percentage<50 || forcedSlideViewToBottom){
                forcedSlideViewToBottom=false
                setSlideViewToBottom()
            }




            onSlideViewReleased(slideViewState)
            invalidate()

        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {

            //중요! 로그 찍어보니까 top 은 계속 변하고 이 child 의 맨 꼭대기 지점의 좌표를 이야기 하는 거였다...!!
            val minTop = mMainView.height - mSlideLayoutMaxHeight
            val maxTop = mMainView.height - mSlideLayoutExposureHeight - paddingBottom

            var slideBound = top
            if (slideBound < minTop)
                slideBound = minTop

            if (slideBound > maxTop)
                slideBound = maxTop
            return slideBound
        }

        override fun onViewDragStateChanged(state: Int) {

            super.onViewDragStateChanged(state)
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {

            super.onViewPositionChanged(changedView, left, top, dx, dy)
        }

    }

    private fun getPercentage(top: Int): Float {
        val maxHeightTop = mMainView.height - mSlideLayoutMaxHeight
        val minHeightTop = mMainView.height - mSlideLayoutExposureHeight
        return 100 - ((100 * (top - maxHeightTop)) / (minHeightTop - maxHeightTop).toFloat())


    }


    private fun isInScrollView(x: Float, y: Float): Boolean {
        if (mScrollViewResId != -1) {
            val scrollViewL = mSlideView.left + mScrollView.left
            val scrollViewR = scrollViewL + mScrollView.width
            val scrollViewT = mSlideView.top + mScrollView.top
            val scrollViewB = scrollViewT + mScrollView.height

            val location = intArrayOf(0, 0)
            mScrollView.getLocationOnScreen(location)


            return (x >= scrollViewL && x <= scrollViewR && y >= scrollViewT && y <= scrollViewB)

        } else {
            return false
        }

        //return if(mScrollViewResId==-1) false
        //else (x>=scrollViewL&&x<=scrollViewR&&y>=scrollViewT&&y<=scrollViewB)

    }

    private fun setScrollViewBounds() {


        if (mScrollViewResId != -1) {
            val scrollViewL = mSlideView.left + mScrollView.left
            val scrollViewR = scrollViewL + mScrollView.width
            val scrollViewT = mSlideView.top + mScrollView.top
            val scrollViewB = scrollViewT + mScrollView.height


        }


    }

    private fun setSlideViewToTop() {
        mViewDragHelper.settleCapturedViewAt(
            mSlideView.left,
            mMainView.height - mSlideLayoutMaxHeight
        )
        slideViewState = 1

    }

    private fun setSlideViewToBottom() {
        mViewDragHelper.settleCapturedViewAt(
            mSlideView.left,
            mMainView.height - mSlideLayoutExposureHeight
        )
        slideViewState = 0

    }

    private fun scrollViewTopState(): Boolean {
        //스크롤 뷰가 탑이냐?(스크롤 뷰의 현재 상태)
        if (mScrollViewResId != -1)
            return !mScrollView.canScrollVertically(-1)

        return true

    }

    private fun isNonTouchArea(x: Float, y: Float): Boolean {


        nonTouchArea.forEach { rect ->
            if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                return true
            }
        }
        return false

    }

    fun setSlideViewTop() {
        mViewDragHelper.captureChildView(mSlideView,0)
        forcedSlideViewToTop=true
        this.onTouchEvent(MotionEvent.obtain(0L,0L, MotionEvent.ACTION_UP,0f,0f,0))
    }

    fun setSlideViewBottom() {
        mViewDragHelper.captureChildView(mSlideView,0)
        forcedSlideViewToBottom=true
        this.onTouchEvent(MotionEvent.obtain(0L,0L, MotionEvent.ACTION_UP,0f,0f,0))
    }

    fun setNonTouchArea(list: List<Rect>) {
        nonTouchArea = list


    }


    fun setScrollView(ResId: Int) {
        mScrollViewResId = ResId
        mScrollView = findViewById(ResId)

    }

    fun getScrollview(): View {
        return mScrollView
    }

    fun getExposureHeight(): Int {
        return if (mSlideLayoutExposureHeight == -1)
            0
        else
            mSlideLayoutExposureHeight

    }

    fun getSlideViewTop(): Int {
        return mSlideView.top
    }

    fun getSlideViewLeft(): Int {
        return mSlideView.left
    }

    fun getSlideViewRight(): Int {
        return mSlideView.right
    }

    fun getSlideViewBottom(): Int {
        return mSlideView.bottom
    }


    fun setOnSlideViewCaptured(doing: (state: Int) -> Unit) {

        onSlideViewCaptured = doing
    }

    fun setOnSlideViewReleased(doing: (state: Int) -> Unit) {
        onSlideViewReleased = doing
    }

    fun setOnSlideViewSettled(doing: (state: Int) -> Unit) {
        onSlideViewSettled = doing
    }

    fun setOnLayout(doing: () -> Unit) {
        mOnLayout = doing

    }

    fun setSlideEnabled(enabled: Boolean) {
        this.slideEnabled = enabled
    }

    fun setExposureHeight(height: Int) {
        mSlideLayoutExposureHeight = dpToPixel(height)
        val slideL = left
        val slideR = slideL + mSlideView.measuredWidth
        val slideT = mMainView.measuredHeight - mSlideLayoutExposureHeight
        val slideB = slideT + mSlideView.measuredHeight
        slideViewState = 0
        mSlideView.layout(slideL, slideT, slideR, slideB)
    }

    private fun dpToPixel(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }


}