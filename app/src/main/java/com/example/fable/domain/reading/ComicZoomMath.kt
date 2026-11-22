package com.example.fable.domain.reading

object ComicZoomMath {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 4f
    const val DOUBLE_TAP_SCALE = 2.5f
    const val ZOOM_ACTIVE_THRESHOLD = 1.02f

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    fun isZoomed(scale: Float): Boolean = scale > ZOOM_ACTIVE_THRESHOLD

    fun toggleDoubleTapScale(currentScale: Float): Float =
        if (isZoomed(currentScale)) MIN_SCALE else DOUBLE_TAP_SCALE

    fun clampOffset(
        offsetX: Float,
        offsetY: Float,
        scale: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Pair<Float, Float> {
        val clampedScale = clampScale(scale)
        if (!isZoomed(clampedScale) || viewportWidth <= 0f || viewportHeight <= 0f) {
            return 0f to 0f
        }
        val maxX = ((viewportWidth * (clampedScale - 1f)) / 2f).coerceAtLeast(0f)
        val maxY = ((viewportHeight * (clampedScale - 1f)) / 2f).coerceAtLeast(0f)
        return offsetX.coerceIn(-maxX, maxX) to offsetY.coerceIn(-maxY, maxY)
    }
}
