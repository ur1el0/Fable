package com.example.fable.ui.screens.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import com.example.fable.domain.model.HighlightColor
import com.example.fable.domain.model.StoryHighlight
import com.example.fable.domain.reading.HighlightExporter
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun HighlightColor.accentColor(): Color = when (this) {
    HighlightColor.TERRACOTTA -> Color(0xFFC2542D)
    HighlightColor.AMBER -> Color(0xFFD99A4E)
    HighlightColor.SAGE -> Color(0xFF5D8A68)
}

fun HighlightColor.highlightFillColor(): Color = when (this) {
    HighlightColor.TERRACOTTA -> Color(0x47C2542D)
    HighlightColor.AMBER -> Color(0x52D99A4E)
    HighlightColor.SAGE -> Color(0x525D8A68)
}

private fun HighlightColor.argbAccent(): Int = when (this) {
    HighlightColor.TERRACOTTA -> 0xFFC2542D.toInt()
    HighlightColor.AMBER -> 0xFFD99A4E.toInt()
    HighlightColor.SAGE -> 0xFF5D8A68.toInt()
}

private fun HighlightColor.argbTintFill(): Int = when (this) {
    HighlightColor.TERRACOTTA -> 0x22C2542D
    HighlightColor.AMBER -> 0x26D99A4E
    HighlightColor.SAGE -> 0x265D8A68
}

fun buildHighlightedParagraph(
    paragraph: String,
    highlights: List<StoryHighlight>,
    searchQuery: String?
): AnnotatedString = buildAnnotatedString {
    append(paragraph)
    for (highlight in highlights) {
        val range = resolveHighlightRange(paragraph, highlight) ?: continue
        addStyle(
            style = SpanStyle(background = highlight.color.highlightFillColor()),
            start = range.first,
            end = range.last + 1
        )
    }
    val trimmedQuery = searchQuery?.trim().orEmpty()
    if (trimmedQuery.isNotEmpty()) {
        var searchIndex = paragraph.indexOf(trimmedQuery, ignoreCase = true)
        while (searchIndex >= 0) {
            val end = (searchIndex + trimmedQuery.length).coerceAtMost(paragraph.length)
            addStyle(
                style = SpanStyle(
                    background = Color(0x66F59E0B),
                    fontWeight = FontWeight.SemiBold
                ),
                start = searchIndex,
                end = end
            )
            searchIndex = paragraph.indexOf(trimmedQuery, startIndex = end, ignoreCase = true)
        }
    }
}

private fun resolveHighlightRange(paragraph: String, highlight: StoryHighlight): IntRange? {
    if (
        highlight.startOffset in 0 until paragraph.length &&
        highlight.endOffset in (highlight.startOffset + 1)..paragraph.length
    ) {
        val candidate = paragraph.substring(highlight.startOffset, highlight.endOffset)
        if (candidate.equals(highlight.selectedText, ignoreCase = true)) {
            return highlight.startOffset until highlight.endOffset
        }
    }
    val fallbackIndex = paragraph.indexOf(highlight.selectedText, ignoreCase = true)
    if (fallbackIndex >= 0 && highlight.selectedText.isNotEmpty()) {
        return fallbackIndex until (fallbackIndex + highlight.selectedText.length).coerceAtMost(paragraph.length)
    }
    return null
}

object QuoteCardExporter {
    suspend fun shareQuoteCardImage(
        context: Context,
        quoteText: String,
        storyTitle: String,
        storyAuthor: String,
        chapterNumber: Int?,
        note: String?,
        color: HighlightColor
    ): Result<Unit> = runCatching {
        val bitmap = withContext(Dispatchers.Default) {
            renderQuoteCardBitmap(
                quoteText = quoteText,
                storyTitle = storyTitle,
                storyAuthor = storyAuthor,
                chapterNumber = chapterNumber,
                note = note,
                highlightColor = color
            )
        }
        val quoteFile = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared_quotes").apply { mkdirs() }
            val file = File(dir, "fable_quote_card.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            quoteFile
        )
        val shareText = HighlightExporter.formatQuoteShareText(
            quoteText = quoteText,
            storyTitle = storyTitle,
            storyAuthor = storyAuthor,
            chapterNumber = chapterNumber,
            note = note
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share quote card"))
    }

    fun sharePlainText(
        context: Context,
        chooserTitle: String,
        subject: String,
        text: String
    ): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    internal fun renderQuoteCardBitmap(
        quoteText: String,
        storyTitle: String,
        storyAuthor: String,
        chapterNumber: Int?,
        note: String?,
        highlightColor: HighlightColor
    ): Bitmap {
        val width = 1080
        val outerMargin = 48f
        val innerPadding = 64f
        val contentWidth = (width - (outerMargin + innerPadding) * 2).toInt()

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5A6474.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111721.toInt()
            textSize = 46f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val noteHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = highlightColor.argbAccent()
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val noteBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF334155.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111721.toInt()
            textSize = 38f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF5A6474.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3B59F6.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val formattedQuote = "“${quoteText.trim()}”"
        val quoteLayout = StaticLayout.Builder
            .obtain(formattedQuote, 0, formattedQuote.length, quotePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(12f, 1f)
            .build()

        val trimmedNote = note?.trim().orEmpty()
        val noteLayout = if (trimmedNote.isNotEmpty()) {
            StaticLayout.Builder
                .obtain(trimmedNote, 0, trimmedNote.length, noteBodyPaint, contentWidth - 56)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(6f, 1f)
                .build()
        } else {
            null
        }

        val titleLayout = StaticLayout.Builder
            .obtain(storyTitle, 0, storyTitle.length, titlePaint, (contentWidth * 0.75f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .build()

        val noteBlockHeight = if (noteLayout != null) noteLayout.height + 92 else 0
        val cardHeight = (innerPadding * 2 + 64 + quoteLayout.height + 44 + noteBlockHeight + 48 + titleLayout.height + 54).toInt()
        val totalHeight = (cardHeight + outerMargin * 2).toInt()

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFF2EFE9.toInt())

        val cardRect = RectF(outerMargin, outerMargin, width - outerMargin, totalHeight - outerMargin)
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFCFBF8.toInt()
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = highlightColor.argbAccent()
            alpha = 110
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, cardFillPaint)
        canvas.drawRoundRect(cardRect, 44f, 44f, cardBorderPaint)

        val leftX = outerMargin + innerPadding
        val rightX = width - outerMargin - innerPadding
        var cursorY = outerMargin + innerPadding + 28f

        canvas.drawText("FABLE READS", leftX, cursorY, headerPaint)
        if (chapterNumber != null) {
            val chapLabel = "CHAPTER $chapterNumber"
            val chapWidth = headerPaint.measureText(chapLabel)
            canvas.drawText(chapLabel, rightX - chapWidth, cursorY, headerPaint)
        }

        cursorY += 42f
        canvas.save()
        canvas.translate(leftX, cursorY)
        quoteLayout.draw(canvas)
        canvas.restore()
        cursorY += quoteLayout.height + 36f

        if (noteLayout != null) {
            val noteBoxHeight = noteLayout.height + 72f
            val noteRect = RectF(leftX, cursorY, rightX, cursorY + noteBoxHeight)
            val noteBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = highlightColor.argbTintFill()
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(noteRect, 20f, 20f, noteBoxPaint)
            canvas.drawText("READER NOTE", leftX + 28f, cursorY + 34f, noteHeaderPaint)
            canvas.save()
            canvas.translate(leftX + 28f, cursorY + 46f)
            noteLayout.draw(canvas)
            canvas.restore()
            cursorY += noteBoxHeight + 32f
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFE3E8ED.toInt()
            strokeWidth = 2f
        }
        canvas.drawLine(leftX, cursorY, rightX, cursorY, dividerPaint)
        cursorY += 32f

        canvas.save()
        canvas.translate(leftX, cursorY)
        titleLayout.draw(canvas)
        canvas.restore()
        cursorY += titleLayout.height + 36f

        canvas.drawText("— $storyAuthor", leftX, cursorY, authorPaint)
        canvas.drawText("fable", rightX, cursorY, brandPaint)

        return bitmap
    }
}
