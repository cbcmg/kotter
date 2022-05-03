package com.varabyte.kotter.terminal.compose

import androidx.compose.ui.graphics.Color

interface TextAttributes {
    val fg: Color?
    val bg: Color?
    val isBold: Boolean
    val isUnderlined: Boolean
    val isStruckThrough: Boolean
    val isInverted: Boolean

    companion object {
        val Empty: TextAttributes = MutableTextAttributes()
    }
}

/**
 * @param _fg The backing field for [fg] (and [bg], if [isInverted] is true). Basically, a raw value which ignores
 *   inversion logic.
 * @param _bg The backing field for [bg] (and [fg], if [isInverted] is true). Basically, a raw value which ignores
 *   inversion logic.
 */
data class MutableTextAttributes(
    var _fg: Color?,
    var _bg: Color?,
    override var isBold: Boolean,
    override var isUnderlined: Boolean,
    override var isStruckThrough: Boolean,
    override var isInverted: Boolean,
) : TextAttributes {
    constructor() : this(null, null, false, false, false, false)

    override var fg: Color?
        get() = if (isInverted) _bg else _fg
        set(value) { _fg = value }

    override var bg: Color?
        get() = if (isInverted) _fg else _bg
        set(value) { _bg = value }

    fun clear() {
        _fg = null
        _bg = null
        isBold = false
        isUnderlined = false
        isStruckThrough = false
        isInverted = false
    }
}