package com.example.fable.domain.reading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicZoomMathTest {
    @Test
    fun clampScaleBoundsToSupportedRange() {
        assertEquals(1f, ComicZoomMath.clampScale(0.4f), 0.001f)
        assertEquals(2.5f, ComicZoomMath.clampScale(2.5f), 0.001f)
        assertEquals(4f, ComicZoomMath.clampScale(6f), 0.001f)
    }

    @Test
    fun toggleDoubleTapScaleAlternatesBetweenOneAndTwoPointFive() {
        assertFalse(ComicZoomMath.isZoomed(1f))
        val zoomed = ComicZoomMath.toggleDoubleTapScale(1f)
        assertEquals(2.5f, zoomed, 0.001f)
        assertTrue(ComicZoomMath.isZoomed(zoomed))
        assertEquals(1f, ComicZoomMath.toggleDoubleTapScale(zoomed), 0.001f)
    }

    @Test
    fun clampOffsetResetsWhenUnzoomedAndBoundsPanningWhenZoomed() {
        assertEquals(0f to 0f, ComicZoomMath.clampOffset(120f, -80f, scale = 1f, viewportWidth = 1000f, viewportHeight = 1600f))

        // At scale 2.0 on 1000x1600 viewport, maxX = 500, maxY = 800
        val (clampedX, clampedY) = ComicZoomMath.clampOffset(
            offsetX = 750f,
            offsetY = -950f,
            scale = 2f,
            viewportWidth = 1000f,
            viewportHeight = 1600f
        )
        assertEquals(500f, clampedX, 0.001f)
        assertEquals(-800f, clampedY, 0.001f)
    }
}
