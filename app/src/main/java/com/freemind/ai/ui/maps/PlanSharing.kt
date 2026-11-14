package com.freemind.ai.ui.maps

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import androidx.core.content.FileProvider
import com.freemind.ai.R
import com.freemind.ai.domain.model.DaySlot
import com.freemind.ai.domain.model.Itinerary
import com.freemind.ai.domain.model.ItineraryStop
import com.freemind.ai.domain.model.dateOf
import com.freemind.ai.domain.model.priceLabel
import com.freemind.ai.domain.model.ratingLabel
import com.freemind.ai.domain.model.routeLink
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale

/** The words a shared plan needs, resolved to the app's language up front so the builders
 *  below stay plain Kotlin (and testable). */
internal class PlanShareLabels(
    val title: String,
    val dayHeader: (Int) -> String,
    val dateLabel: (LocalDate) -> String,
    val slot: (DaySlot) -> String,
    val verb: (ItineraryStop) -> String,
    val routeLabel: String,
    val notesHeader: String,
    val checklistHeader: String,
    val footer: String,
)

internal fun Context.planShareLabels(plan: Itinerary): PlanShareLabels = PlanShareLabels(
    title = getString(R.string.plan_title, plan.days.size, plan.destinationName),
    dayHeader = { getString(R.string.plan_day_header, it) },
    dateLabel = { it.shortLabel() },
    slot = {
        getString(
            when (it) {
                DaySlot.MORNING -> R.string.slot_morning
                DaySlot.MIDDAY -> R.string.slot_midday
                DaySlot.AFTERNOON -> R.string.slot_afternoon
                DaySlot.EVENING -> R.string.slot_evening
                DaySlot.ANYTIME -> R.string.slot_added
            },
        )
    },
    verb = { localizedVerb(it) },
    routeLabel = getString(R.string.share_route),
    notesHeader = getString(R.string.share_notes),
    checklistHeader = getString(R.string.share_checklist),
    footer = getString(R.string.share_footer),
)

private fun ItineraryStop.headline(labels: PlanShareLabels): String {
    val verb = labels.verb(this)
    return if (verb.isBlank()) place.title else "$verb ${place.title}"
}

/**
 * The plan as plain text for any messaging app: each stop, then a Google Maps link that
 * opens that day as a walking route, then the trip notes and checklist.
 */
internal fun Itinerary.toShareText(labels: PlanShareLabels): String = buildString {
    append(labels.title)
    dateOf(1)?.let { start ->
        val end = dateOf(days.size)
        append(" (").append(labels.dateLabel(start))
        if (end != null && end != start) append(" - ").append(labels.dateLabel(end))
        append(")")
    }
    days.forEach { day ->
        append("\n\n").append(labels.dayHeader(day.day))
        dateOf(day.day)?.let { append(" · ").append(labels.dateLabel(it)) }
        day.stops.forEach { stop ->
            append("\n- ").append(stop.headline(labels))
            stop.place.ratingLabel()?.let { append(" ($it)") }
            if (stop.place.description.isNotBlank()) append(" - ").append(stop.place.description)
            if (stop.note.isNotBlank()) append("\n  ").append(stop.note)
        }
        day.routeLink()?.let { append("\n").append(labels.routeLabel).append(": ").append(it) }
    }
    if (notes.isNotBlank()) {
        append("\n\n").append(labels.notesHeader).append("\n").append(notes.trim())
    }
    if (checklist.isNotEmpty()) {
        append("\n\n").append(labels.checklistHeader)
        checklist.forEach { append("\n").append(if (it.done) "[x] " else "[ ] ").append(it.text) }
    }
    append("\n\n").append(labels.footer)
}

// ---- PDF ---------------------------------------------------------------------

private const val PAGE_WIDTH = 595 // A4 at 72 dpi
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val STOP_INDENT = 14f

/**
 * Draws [plan] as a printable A4 document: one heading per day, each stop with its
 * address, rating and any note of the traveller's, then the trip notes and checklist.
 * Text goes through StaticLayout so long lines wrap and Arabic is shaped and aligned
 * right-to-left like on screen.
 */
internal fun writePlanPdf(file: File, plan: Itinerary, labels: PlanShareLabels) {
    val doc = PdfDocument()
    val rtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL

    fun paint(sizeSp: Float, bold: Boolean = false, color: Int = Color.BLACK, italic: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizeSp
        this.color = color
        typeface = Typeface.create(Typeface.DEFAULT, if (bold && italic) Typeface.BOLD_ITALIC else if (bold) Typeface.BOLD else if (italic) Typeface.ITALIC else Typeface.NORMAL)
    }
    val titlePaint = paint(22f, bold = true)
    val dayPaint = paint(15f, bold = true, color = Color.rgb(0x1B, 0x5E, 0x20))
    val stopPaint = paint(12f, bold = true)
    val bodyPaint = paint(10.5f, color = Color.DKGRAY)
    val notePaint = paint(10.5f, italic = true, color = Color.rgb(0x0D, 0x47, 0xA1))
    val footerPaint = paint(9f, color = Color.GRAY)

    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var canvas: Canvas? = null
    var y = MARGIN

    fun newPage() {
        page?.let { doc.finishPage(it) }
        pageNumber += 1
        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page!!.canvas
        y = MARGIN
    }
    newPage()

    fun draw(text: CharSequence, paint: TextPaint, spaceAfter: Float = 3f, indent: Float = 0f) {
        if (text.isEmpty()) return
        val width = (PAGE_WIDTH - 2 * MARGIN - indent).toInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        if (y + layout.height > PAGE_HEIGHT - MARGIN) newPage()
        val c = canvas!!
        c.save()
        // The indent sits on the reading-start side: the right edge in right-to-left text.
        c.translate(if (rtl) MARGIN else MARGIN + indent, y)
        layout.draw(c)
        c.restore()
        y += layout.height + spaceAfter
    }

    draw(labels.title, titlePaint, spaceAfter = 4f)
    plan.dateOf(1)?.let { start ->
        val end = plan.dateOf(plan.days.size)
        val range = labels.dateLabel(start) + if (end != null && end != start) " - " + labels.dateLabel(end) else ""
        draw(range, bodyPaint, spaceAfter = 10f)
    }

    plan.days.forEach { day ->
        y += 8f
        val heading = labels.dayHeader(day.day) + (plan.dateOf(day.day)?.let { " · " + labels.dateLabel(it) } ?: "")
        draw(heading, dayPaint, spaceAfter = 5f)
        day.stops.forEach { stop ->
            draw("${labels.slot(stop.timeSlot)} · ${stop.headline(labels)}", stopPaint, spaceAfter = 1f, indent = STOP_INDENT)
            if (stop.place.description.isNotBlank()) draw(stop.place.description, bodyPaint, spaceAfter = 1f, indent = STOP_INDENT)
            val meta = listOfNotNull(stop.place.ratingLabel(), stop.place.priceLabel()).joinToString(" · ")
            if (meta.isNotEmpty()) draw(meta, bodyPaint, spaceAfter = 1f, indent = STOP_INDENT)
            if (stop.note.isNotBlank()) draw(stop.note, notePaint, spaceAfter = 1f, indent = STOP_INDENT)
            y += 5f
        }
    }

    if (plan.notes.isNotBlank()) {
        y += 8f
        draw(labels.notesHeader, dayPaint, spaceAfter = 4f)
        draw(plan.notes.trim(), bodyPaint, spaceAfter = 6f)
    }
    if (plan.checklist.isNotEmpty()) {
        y += 8f
        draw(labels.checklistHeader, dayPaint, spaceAfter = 4f)
        plan.checklist.forEach { draw((if (it.done) "☑ " else "☐ ") + it.text, bodyPaint, spaceAfter = 2f) }
    }
    y += 10f
    draw(labels.footer, footerPaint)

    page?.let { doc.finishPage(it) }
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
}

/** Writes the plan's PDF into the shareable cache directory and returns its content Uri. */
internal fun Context.exportPlanPdf(plan: Itinerary, labels: PlanShareLabels): Uri {
    val safeName = plan.destinationName.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').ifEmpty { "trip" }
    val file = File(cacheDir, "freemind_trip_$safeName.pdf")
    writePlanPdf(file, plan, labels)
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

internal fun Context.sharePdf(uri: Uri, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        // ClipData is what carries the read grant through the chooser to the receiving app.
        clipData = ClipData.newUri(contentResolver, "trip.pdf", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
