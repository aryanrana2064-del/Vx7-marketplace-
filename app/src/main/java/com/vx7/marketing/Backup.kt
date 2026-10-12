package com.vx7.marketing

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Versioned JSON backup. Restore always MERGES: nothing existing is deleted or overwritten. */
object Backup {
    const val FORMAT = "VX7-Marketing-Backup"
    const val VERSION = 1

    class BProduct(val id: Long, val name: String, val category: String, val notes: String)
    class BEntry(val productId: Long, val productName: String, val quantity: Double, val unit: String, val note: String)
    class BRecord(val createdAt: Long, val shop: String, val location: String, val note: String, val entries: List<BEntry>)
    class BNote(val title: String, val body: String, val createdAt: Long)
    class Parsed(val products: List<BProduct>, val records: List<BRecord>, val notes: List<BNote>)
    class Result(val productsAdded: Int, val recordsAdded: Int, val recordsSkipped: Int, val notesAdded: Int)

    suspend fun export(db: AppDb): String {
        val products = db.products().allOnce()
        val records = db.records().allRecords()
        val entries = db.records().allEntries().groupBy { it.recordId }
        val notes = db.notes().allOnce()
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("products", JSONArray().also { a ->
            products.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("category", it.category).put("notes", it.notes)) }
        })
        root.put("records", JSONArray().also { a ->
            records.forEach { r ->
                val ea = JSONArray()
                (entries[r.id] ?: emptyList()).forEach {
                    ea.put(JSONObject().put("productId", it.productId).put("productName", it.productName)
                        .put("quantity", it.quantity).put("unit", it.unit).put("note", it.note))
                }
                a.put(JSONObject().put("createdAt", r.createdAt).put("shop", r.shop).put("location", r.location)
                    .put("note", r.note).put("entries", ea))
            }
        })
        root.put("notes", JSONArray().also { a ->
            notes.forEach { a.put(JSONObject().put("title", it.title).put("body", it.body).put("createdAt", it.createdAt)) }
        })
        return root.toString(2)
    }

    /** Validates fully before anything touches the database. Throws IllegalArgumentException with a readable message. */
    fun parse(text: String): Parsed {
        val root = try { JSONObject(text) } catch (e: Exception) { throw IllegalArgumentException("This file is not a valid JSON backup.") }
        if (root.optString("format") != FORMAT) throw IllegalArgumentException("This is not a VX7 Marketing backup file.")
        val ver = root.optInt("version", -1)
        if (ver < 1 || ver > VERSION) throw IllegalArgumentException("Unsupported backup version ($ver). Update the app and try again.")
        val pa = root.optJSONArray("products") ?: throw IllegalArgumentException("Backup is missing the products list.")
        val ra = root.optJSONArray("records") ?: throw IllegalArgumentException("Backup is missing the records list.")
        val products = (0 until pa.length()).map { i ->
            val o = pa.optJSONObject(i) ?: throw IllegalArgumentException("Product #${i + 1} is damaged.")
            val name = o.optString("name").trim()
            if (name.isEmpty()) throw IllegalArgumentException("Product #${i + 1} has no name.")
            BProduct(o.optLong("id", -1), name, o.optString("category"), o.optString("notes"))
        }
        val records = (0 until ra.length()).map { i ->
            val o = ra.optJSONObject(i) ?: throw IllegalArgumentException("Record #${i + 1} is damaged.")
            if (!o.has("createdAt")) throw IllegalArgumentException("Record #${i + 1} has no date.")
            val ea = o.optJSONArray("entries") ?: throw IllegalArgumentException("Record #${i + 1} has no entries list.")
            val entries = (0 until ea.length()).map { j ->
                val e = ea.optJSONObject(j) ?: throw IllegalArgumentException("Record #${i + 1}, entry #${j + 1} is damaged.")
                val q = e.optDouble("quantity", Double.NaN)
                if (!(q > 0.0) || !q.isFinite()) throw IllegalArgumentException("Record #${i + 1}, entry #${j + 1} has an invalid quantity.")
                val name = e.optString("productName").trim()
                val unit = e.optString("unit").trim()
                if (name.isEmpty() || unit.isEmpty()) throw IllegalArgumentException("Record #${i + 1}, entry #${j + 1} is missing a product or unit.")
                BEntry(e.optLong("productId", -1), name, q, unit, e.optString("note"))
            }
            BRecord(o.optLong("createdAt"), o.optString("shop"), o.optString("location"), o.optString("note"), entries)
        }
        val na = root.optJSONArray("notes") ?: JSONArray()
        val notes = (0 until na.length()).map { i ->
            val o = na.optJSONObject(i) ?: throw IllegalArgumentException("Note #${i + 1} is damaged.")
            BNote(o.optString("title"), o.optString("body"), o.optLong("createdAt"))
        }
        return Parsed(products, records, notes)
    }

    private fun sig(createdAt: Long, shop: String, location: String, note: String, es: List<Triple<String, Double, String>>) =
        "$createdAt|$shop|$location|$note|" + es.joinToString(";") { "${it.first}~${it.second}~${it.third}" }

    suspend fun restore(db: AppDb, p: Parsed): Result = db.withTransaction {
        val existing = db.products().allOnce().associateBy { it.name.lowercase() }.toMutableMap()
        val idMap = HashMap<Long, Long>()
        var added = 0
        for (bp in p.products) {
            val hit = existing[bp.name.lowercase()]
            if (hit != null) {
                idMap[bp.id] = hit.id
            } else {
                val id = db.products().insert(Product(name = bp.name, category = bp.category, notes = bp.notes))
                existing[bp.name.lowercase()] = Product(id, bp.name, bp.category, bp.notes)
                idMap[bp.id] = id
                added++
            }
        }
        val dao = db.records()
        val entriesByRecord = dao.allEntries().groupBy { it.recordId }
        val sigs = dao.allRecords().map { r ->
            sig(r.createdAt, r.shop, r.location, r.note, (entriesByRecord[r.id] ?: emptyList()).map { Triple(it.productName, it.quantity, it.unit) })
        }.toMutableSet()
        var recAdded = 0
        var recSkipped = 0
        for (br in p.records) {
            val s = sig(br.createdAt, br.shop, br.location, br.note, br.entries.map { Triple(it.productName, it.quantity, it.unit) })
            if (!sigs.add(s)) { recSkipped++; continue }
            val rid = dao.insertRecord(CollectionRecord(createdAt = br.createdAt, shop = br.shop, location = br.location, note = br.note))
            dao.insertEntries(br.entries.map { Entry(recordId = rid, productId = idMap[it.productId] ?: 0L, productName = it.productName, quantity = it.quantity, unit = it.unit, note = it.note) })
            recAdded++
        }
        val haveNotes = db.notes().allOnce().map { "${it.createdAt}|${it.title}|${it.body}" }.toMutableSet()
        var notesAdded = 0
        for (n in p.notes) {
            if (haveNotes.add("${n.createdAt}|${n.title}|${n.body}")) {
                db.notes().add(QuickNote(title = n.title, body = n.body, createdAt = n.createdAt)); notesAdded++
            }
        }
        Result(added, recAdded, recSkipped, notesAdded)
    }
}
