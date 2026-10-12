package com.vx7.marketing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object Exporter {
    private val NAVY = 0xFF0B1228.toInt()
    private val NAVY2 = 0xFF14204A.toInt()
    private val BLUE = 0xFF2677E8.toInt()
    private val GOLD = 0xFFD5B05D.toInt()
    private val INK = 0xFF121A33.toInt()
    private val ZEBRA = 0xFFEAF0FB.toInt()

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun lay(text: String, p: TextPaint, w: Float, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, w.toInt().coerceAtLeast(1))
            .setAlignment(align).setIncludePad(false).build()

    private fun logo(ctx: Context, size: Int): Bitmap {
        val raw = BitmapFactory.decodeResource(ctx.resources, R.drawable.vx7_logo)
        return Bitmap.createScaledBitmap(raw, size, size, true)
    }

    private fun outFile(ctx: Context, name: String): File {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val cutoff = System.currentTimeMillis() - 24L * 3600 * 1000
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
        return File(dir, name)
    }

    private fun baseName(r: CollectionRecord) = "VX7_Collection_${fileStamp(r.createdAt)}_R${r.id}"

    // ---------------------------------------------------------------- PDF (A4, multi-page)
    fun pdf(ctx: Context, r: CollectionRecord, es: List<Entry>): File {
        val W = 595
        val H = 842
        val M = 40f
        val CW = W - 2 * M
        val limit = H - 60f
        val doc = PdfDocument()
        val title = paint(22f, NAVY, true)
        val tag = paint(9f, GOLD, true)
        val head = paint(17f, BLUE, true)
        val body = paint(11f, INK)
        val bodyB = paint(11f, INK, true)
        val small = paint(9f, 0xFF6B7280.toInt())
        val white = paint(10f, Color.WHITE, true)
        val fill = Paint()
        val logoBmp = logo(ctx, 128)

        val colName = M + 38f
        val colQty = M + 300f
        val colUnit = M + 390f
        val nameW = 250f
        val qtyW = 80f
        val unitW = M + CW - colUnit - 4f

        var pageNo = 0
        var y = 0f
        lateinit var page: PdfDocument.Page
        lateinit var cv: Canvas

        fun footer() {
            cv.drawText("VX7 Marketing  \u2022  Product Collection Record  \u2022  Page $pageNo", M, H - 30f, small)
        }

        fun newPage() {
            if (pageNo > 0) {
                footer()
                doc.finishPage(page)
            }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            cv = page.canvas
            y = M
        }

        fun para(text: String, p: TextPaint, gap: Float = 4f) {
            val l = lay(text, p, CW)
            for (i in 0 until l.lineCount) {
                val h = (l.getLineBottom(i) - l.getLineTop(i)).toFloat()
                if (y + h > limit) newPage()
                val base = y + (l.getLineBaseline(i) - l.getLineTop(i))
                cv.drawText(text.substring(l.getLineStart(i), l.getLineEnd(i)).trimEnd('\n'), M, base, p)
                y += h
            }
            y += gap
        }

        fun tableHead() {
            fill.color = NAVY
            cv.drawRect(M, y, M + CW, y + 22f, fill)
            cv.drawText("No.", M + 8f, y + 15f, white)
            cv.drawText("Product", colName, y + 15f, white)
            cv.drawText("Quantity", colQty, y + 15f, white)
            cv.drawText("Unit", colUnit, y + 15f, white)
            y += 22f
        }

        newPage()
        cv.drawBitmap(logoBmp, null, RectF(M, y, M + 64f, y + 64f), null)
        cv.drawText("VX7 MARKETING", M + 78f, y + 30f, title)
        cv.drawText("TRACK  \u2022  PURCHASE  \u2022  GROW", M + 78f, y + 48f, tag)
        y += 78f
        fill.color = GOLD
        cv.drawRect(M, y, M + CW, y + 2f, fill)
        y += 20f
        cv.drawText("PRODUCT COLLECTION RECORD", M, y + 4f, head)
        y += 26f
        para("Date: ${fmtDate(r.createdAt)}        Time: ${fmtTime(r.createdAt)}", bodyB, 2f)
        if (r.shop.isNotBlank()) para("Shop: ${r.shop}", body, 2f)
        if (r.location.isNotBlank()) para("Location: ${r.location}", body, 2f)
        y += 10f
        tableHead()

        es.forEachIndexed { i, e ->
            val nm = lay(e.productName, body, nameW)
            val nt = if (e.note.isNotBlank()) lay(e.note, small, nameW) else null
            val un = lay(e.unit, body, unitW)
            val nameH = nm.height + (if (nt != null) nt.height + 2 else 0)
            val rowH = maxOf(nameH, un.height) + 10f
            if (y + rowH > limit) {
                newPage()
                tableHead()
            }
            if (i % 2 == 0) {
                fill.color = ZEBRA
                cv.drawRect(M, y, M + CW, y + rowH, fill)
            }
            cv.drawText("${i + 1}", M + 8f, y + 5f - body.fontMetrics.ascent, body)
            cv.save(); cv.translate(colName, y + 5f); nm.draw(cv); cv.restore()
            if (nt != null) {
                cv.save(); cv.translate(colName, y + 5f + nm.height + 2f); nt.draw(cv); cv.restore()
            }
            cv.drawText(fmtQty(e.quantity), colQty, y + 5f - bodyB.fontMetrics.ascent, bodyB)
            cv.save(); cv.translate(colUnit, y + 5f); un.draw(cv); cv.restore()
            y += rowH
        }
        fill.color = 0xFFC7D2E8.toInt()
        cv.drawRect(M, y, M + CW, y + 1f, fill)
        y += 16f
        if (y + 30f > limit) newPage()
        para("Total product entries: ${es.size}", bodyB, 10f)
        if (r.note.isNotBlank()) {
            para("Notes", bodyB, 2f)
            para(r.note, body)
        }
        footer()
        doc.finishPage(page)
        val f = outFile(ctx, baseName(r) + ".pdf")
        try {
            FileOutputStream(f).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        return f
    }

    // ---------------------------------------------------------------- PNG (auto height)
    fun png(ctx: Context, r: CollectionRecord, es: List<Entry>): File {
        val W = 1080
        val pad = 56f
        val inner = W - 2 * pad
        val brand = paint(60f, Color.WHITE, true)
        val tag = paint(28f, GOLD, true)
        val head = paint(44f, GOLD, true)
        val lab = paint(26f, GOLD, true)
        val val1 = paint(34f, Color.WHITE)
        val rowName = paint(34f, INK, true)
        val rowNote = paint(26f, 0xFF5B6475.toInt())
        val rowQty = paint(36f, BLUE, true)
        val th = paint(26f, Color.WHITE, true)
        val foot = paint(38f, GOLD, true)
        val small = paint(24f, 0xFFB8C2DD.toInt())
        val logoBmp = logo(ctx, 160)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        fun run(cv: Canvas?): Float {
            var y = 0f
            fun draw(l: StaticLayout, x: Float, yy: Float) {
                cv?.let { it.save(); it.translate(x, yy); l.draw(it); it.restore() }
            }
            cv?.drawColor(NAVY)
            y = pad
            cv?.drawBitmap(logoBmp, null, RectF(pad, y, pad + 160f, y + 160f), null)
            cv?.drawText("VX7 MARKETING", pad + 190f, y + 80f, brand)
            cv?.drawText("TRACK  \u2022  PURCHASE  \u2022  GROW", pad + 190f, y + 130f, tag)
            y += 160f + 36f
            fill.color = GOLD
            cv?.drawRect(pad, y, W - pad, y + 4f, fill)
            y += 40f
            val h = lay("PRODUCT COLLECTION RECORD", head, inner, Layout.Alignment.ALIGN_CENTER)
            draw(h, pad, y)
            y += h.height + 30f

            // info card
            val infos = ArrayList<Pair<String, String>>()
            infos.add("DATE" to fmtLongDate(r.createdAt))
            infos.add("TIME" to fmtTime(r.createdAt))
            if (r.shop.isNotBlank()) infos.add("SHOP" to r.shop)
            if (r.location.isNotBlank()) infos.add("LOCATION" to r.location)
            val cardPad = 32f
            val infoLayouts = infos.map { lay(it.first, lab, inner - 2 * cardPad) to lay(it.second, val1, inner - 2 * cardPad) }
            val infoH = infoLayouts.sumOf { (it.first.height + it.second.height + 18).toDouble() }.toFloat() + 2 * cardPad - 18f
            fill.color = NAVY2
            cv?.drawRoundRect(RectF(pad, y, W - pad, y + infoH), 28f, 28f, fill)
            var iy = y + cardPad
            infoLayouts.forEach { (a, b) ->
                draw(a, pad + cardPad, iy); iy += a.height + 4f
                draw(b, pad + cardPad, iy); iy += b.height + 14f
            }
            y += infoH + 36f

            // table card (white)
            val numW = 70f
            val qtyW = 300f
            val nameW = inner - numW - qtyW - 24f
            val rows = es.map { e ->
                val nm = lay(e.productName, rowName, nameW)
                val nt = if (e.note.isNotBlank()) lay(e.note, rowNote, nameW) else null
                val q = lay("${fmtQty(e.quantity)} ${e.unit}", rowQty, qtyW, Layout.Alignment.ALIGN_OPPOSITE)
                Triple(nm, nt, q)
            }
            val headH = 64f
            val rowHs = rows.map { (nm, nt, q) -> maxOf(nm.height + (nt?.height ?: 0) + (if (nt != null) 6 else 0), q.height) + 36f }
            val tableH = headH + rowHs.sum() + 12f
            fill.color = Color.WHITE
            cv?.drawRoundRect(RectF(pad, y, W - pad, y + tableH), 28f, 28f, fill)
            fill.color = BLUE
            cv?.drawRoundRect(RectF(pad, y, W - pad, y + headH + 12f), 28f, 28f, fill)
            fill.color = Color.WHITE
            cv?.drawRect(pad, y + headH, W - pad, y + headH + 12f, fill)
            cv?.drawText("No.", pad + 22f, y + 42f, th)
            cv?.drawText("Product", pad + numW + 22f, y + 42f, th)
            val qh = lay("Quantity", th, qtyW, Layout.Alignment.ALIGN_OPPOSITE)
            draw(qh, W - pad - 22f - qtyW, y + 20f)
            var ry = y + headH
            rows.forEachIndexed { i, (nm, nt, q) ->
                val rh = rowHs[i]
                if (i % 2 == 1) {
                    fill.color = ZEBRA
                    cv?.drawRect(pad, ry, W - pad, ry + rh, fill)
                }
                cv?.drawText("${i + 1}", pad + 22f, ry + 18f - rowName.fontMetrics.ascent, rowName)
                draw(nm, pad + numW + 22f, ry + 18f)
                if (nt != null) draw(nt, pad + numW + 22f, ry + 18f + nm.height + 6f)
                draw(q, W - pad - 22f - qtyW, ry + 18f)
                ry += rh
            }
            y += tableH + 40f

            val total = lay("Total product entries: ${es.size}", foot, inner, Layout.Alignment.ALIGN_CENTER)
            draw(total, pad, y)
            y += total.height + 24f
            if (r.note.isNotBlank()) {
                val nl = lay("NOTES", lab, inner)
                draw(nl, pad, y); y += nl.height + 8f
                val nb = lay(r.note, val1, inner)
                draw(nb, pad, y); y += nb.height + 30f
            }
            val ft = lay("Created with VX7 Marketing", small, inner, Layout.Alignment.ALIGN_CENTER)
            draw(ft, pad, y)
            y += ft.height + pad
            return y
        }

        val height = run(null).toInt()
        val cfg = if (height > 16000) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        val bmp = try {
            Bitmap.createBitmap(W, height, cfg)
        } catch (e: OutOfMemoryError) {
            throw IllegalStateException("This record is too large to render as one image. Please share it as a PDF instead.")
        }
        try {
            run(Canvas(bmp))
            val f = outFile(ctx, baseName(r) + ".png")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            return f
        } finally {
            bmp.recycle()
        }
    }

    fun share(ctx: Context, f: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(f.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, title))
    }
}
