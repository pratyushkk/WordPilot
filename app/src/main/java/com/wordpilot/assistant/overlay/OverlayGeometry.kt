package com.wordpilot.assistant.overlay

import com.wordpilot.assistant.ui.settings.AppearanceSettings

object OverlayGeometry {

    const val CONTENT_PADDING_DP = 8f
    const val MIN_SCREEN_MARGIN_DP = 8f
    const val BOTTOM_BAR_SAFETY_MARGIN_DP = 48f
    const val TOP_BAR_SAFETY_MARGIN_DP = 16f

    fun getCollapsedSizePx(density: Float, bubbleSizeDp: Float): Int {
        val totalDp = bubbleSizeDp + (CONTENT_PADDING_DP * 2)
        return (totalDp * density).toInt()
    }

    fun getExpandedWidthPx(density: Float, screenWidthPx: Int, toolbarMaxWidthDp: Float): Int {
        val screenWidthDp = screenWidthPx / density
        val maxAllowedWidthDp = (screenWidthDp - 24f).coerceAtLeast(160f)
        val expandedWidthDp = toolbarMaxWidthDp.coerceIn(160f, maxAllowedWidthDp)
        return ((expandedWidthDp + (CONTENT_PADDING_DP * 2)) * density).toInt()
    }

    fun getExpandedHeightPx(density: Float, toolbarHeightDp: Float, bubbleSizeDp: Float): Int {
        val expandedHeightDp = maxOf(toolbarHeightDp, bubbleSizeDp) + (CONTENT_PADDING_DP * 2)
        return (expandedHeightDp * density).toInt()
    }

    fun getMoreOptionsWidthPx(density: Float, screenWidthPx: Int): Int {
        val screenWidthDp = screenWidthPx / density
        val maxAllowedWidthDp = (screenWidthDp - 24f).coerceAtLeast(200f)
        val panelWidthDp = 340f.coerceAtMost(maxAllowedWidthDp)
        return (panelWidthDp * density).toInt()
    }

    fun getMoreOptionsHeightPx(density: Float): Int {
        return (360f * density).toInt()
    }

    fun getPreviewWidthPx(density: Float, screenWidthPx: Int): Int {
        val screenWidthDp = screenWidthPx / density
        val maxAllowedWidthDp = (screenWidthDp - 24f).coerceAtLeast(200f)
        val previewWidthDp = 340f.coerceAtMost(maxAllowedWidthDp)
        return (previewWidthDp * density).toInt()
    }

    fun getPreviewHeightPx(density: Float): Int {
        return (320f * density).toInt()
    }

    fun getTuckedX(
        isNearRightEdge: Boolean,
        bubbleSizeDp: Float,
        screenWidthPx: Int,
        density: Float,
        tuckPercent: Float = 0.30f
    ): Int {
        val bubbleWidthPx = (bubbleSizeDp * density).toInt()
        val paddingPx = (CONTENT_PADDING_DP * density).toInt()
        val tuckPx = (bubbleWidthPx * tuckPercent).toInt()
        return if (isNearRightEdge) {
            screenWidthPx - (bubbleWidthPx - tuckPx) - paddingPx
        } else {
            -(tuckPx + paddingPx)
        }
    }

    fun calculateEdgeAnchoredX(
        isNearRightEdge: Boolean,
        bubbleRestingX: Int,
        collapsedWidthPx: Int,
        targetWidthPx: Int,
        screenWidthPx: Int,
        density: Float
    ): Int {
        val marginPx = (MIN_SCREEN_MARGIN_DP * density).toInt()
        return if (isNearRightEdge) {
            val bubbleRightEdgePx = (bubbleRestingX + collapsedWidthPx).coerceAtMost(screenWidthPx)
            (bubbleRightEdgePx - targetWidthPx).coerceAtLeast(marginPx)
        } else {
            bubbleRestingX.coerceAtMost(screenWidthPx - targetWidthPx - marginPx).coerceAtLeast(marginPx)
        }
    }

    fun clampY(
        restingY: Int,
        windowHeightPx: Int,
        screenHeightPx: Int,
        density: Float
    ): Int {
        val bottomMargin = (BOTTOM_BAR_SAFETY_MARGIN_DP * density).toInt()
        val topMargin = (TOP_BAR_SAFETY_MARGIN_DP * density).toInt()
        val maxY = (screenHeightPx - windowHeightPx - bottomMargin).coerceAtLeast(topMargin)
        return restingY.coerceIn(topMargin, maxY)
    }
}
