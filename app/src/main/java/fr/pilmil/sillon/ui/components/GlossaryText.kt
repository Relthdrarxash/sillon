package fr.pilmil.sillon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import fr.pilmil.sillon.data.AgronomyGlossary
import fr.pilmil.sillon.data.GlossaryEntry
import kotlin.math.roundToInt

/**
 * Affiche un texte en soulignant les termes agronomiques reconnus dans [AgronomyGlossary].
 * Un appui sur un terme souligné ouvre une bulle d'explication au-dessus du mot.
 */
@Composable
fun GlossaryText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = style.lineHeight,
    underlineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val matches = remember(text) { AgronomyGlossary.findMatches(text) }
    if (matches.isEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
        )
        return
    }

    val annotated = remember(text, matches, underlineColor) {
        buildAnnotatedString {
            append(text)
            for (m in matches) {
                addStyle(
                    style = SpanStyle(
                        color = underlineColor,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start = m.start,
                    end = m.end,
                )
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var activeEntry by remember { mutableStateOf<GlossaryEntry?>(null) }
    var wordBounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }
    val marginPx = with(density) { 12.dp.roundToPx() }

    Column(modifier = modifier) {
        Text(
            text = annotated,
            style = style,
            color = color,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            onTextLayout = { layoutResult = it },
            modifier = Modifier.pointerInput(matches) {
                detectTapGestures { tapPos ->
                    val layout = layoutResult ?: return@detectTapGestures
                    val charIndex = layout.getOffsetForPosition(tapPos)
                    val hit = matches.firstOrNull { charIndex in it.start until it.end }
                    if (hit != null) {
                        val startBox = layout.getBoundingBox(hit.start)
                        val endBox = layout.getBoundingBox((hit.end - 1).coerceAtLeast(hit.start))
                        wordBounds = Rect(
                            left = minOf(startBox.left, endBox.left),
                            top = minOf(startBox.top, endBox.top),
                            right = maxOf(startBox.right, endBox.right),
                            bottom = maxOf(startBox.bottom, endBox.bottom),
                        )
                        activeEntry = hit.entry
                    }
                }
            },
        )

        val shown = activeEntry
        if (shown != null) {
            val positionProvider = remember(wordBounds, gapPx, marginPx) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset {
                        val wordCenterX = anchorBounds.left + ((wordBounds.left + wordBounds.right) / 2f).roundToInt()
                        val wordTop = anchorBounds.top + wordBounds.top.roundToInt()
                        val wordBottom = anchorBounds.top + wordBounds.bottom.roundToInt()

                        val x = (wordCenterX - popupContentSize.width / 2)
                            .coerceIn(marginPx, (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx))
                        val aboveY = wordTop - popupContentSize.height - gapPx
                        val y = if (aboveY >= marginPx) {
                            aboveY
                        } else {
                            (wordBottom + gapPx).coerceAtMost((windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx))
                        }
                        return IntOffset(x, y)
                    }
                }
            }
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { activeEntry = null },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier.widthIn(min = 190.dp, max = 300.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = { activeEntry = null },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = shown.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shown.definition,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Toucher pour fermer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
