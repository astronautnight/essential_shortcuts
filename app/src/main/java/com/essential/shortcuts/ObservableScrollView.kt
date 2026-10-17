package com.essential.shortcuts

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class ObservableScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private val maxOverscrollDistance: Int

    companion object {
        private const val MAX_Y_OVERSCROLL_DISTANCE = 150
    }

    init {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        maxOverscrollDistance = (density * MAX_Y_OVERSCROLL_DISTANCE).toInt()
    }

    override fun overScrollBy(
        deltaX: Int,
        deltaY: Int,
        scrollX: Int,
        scrollY: Int,
        scrollRangeX: Int,
        scrollRangeY: Int,
        maxOverScrollX: Int,
        maxOverScrollY: Int,
        isTouchEvent: Boolean
    ): Boolean {
        return super.overScrollBy(
            deltaX,
            deltaY,
            scrollX,
            scrollY,
            scrollRangeX,
            scrollRangeY,
            maxOverScrollX,
            maxOverscrollDistance,
            isTouchEvent
        )
    }
}