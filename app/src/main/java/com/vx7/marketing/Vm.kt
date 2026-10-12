package com.vx7.marketing

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DraftLine(val productId: Long, val name: String, unit: String = "Box") {
    var qty by mutableStateOf("")
    var unit by mutableStateOf(unit)
    var custom by mutableStateOf("")
    var note by mutableStateOf("")
}

class PeriodState {
    var key by mutableStateOf("all")
    var from by mutableLongStateOf(0L)
    var to by mutableLongStateOf(Long.MAX_VALUE)

    fun set(k: String, f: Long, t: Long) { key = k; from = f; to = t }
    fun apply(k: String) {
        val now = System.currentTimeMillis()
        when (k) {
            "today" -> set(k, dayStart(now), dayEnd(now))
            "week" -> set(k, daysAgoStart(now, 6), dayEnd(now))
            "month" -> set(k, monthStart(now), dayEnd(now))
            else -> set("all", 0L, Long.MAX_VALUE)
        }
    }
    fun label(): String = when (key) {
        "all" -> "All dates"
        "today" -> "Today"
        "week" -> "Last 7 days"
        "month" -> "This month"
        else -> if (fileDay(from) == fileDay(to)) fmtDate(from) else "${fmtDate(from)} - ${fmtDate(to)}"
    }
}

sealed interface AddResult {
    class Created(val product: Product) : AddResult
    class Exists(val product: Product) : AddResult
    class Similar(val names: List<String>) : AddResult
    class Invalid(val message: String) : AddResult
}

class Vm(app: Application) : AndroidViewModel(app) {
    val db: AppDb = AppDb.get(app)
    private val prefs = app.getSharedPreferences("vx7", Context.MODE_PRIVATE)

    var theme by mutableStateOf(prefs.getString("theme", "system") ?: "system")
        private set
    var defaultUnit by mutableStateOf(prefs.getString("unit", "Box") ?: "Box")
        private set
    var message by mutableStateOf<String?>(null)

    val hist = PeriodState()
    val rep = PeriodState()

    fun setTheme(v: String) { theme = v; prefs.edit().putString("theme", v).apply() }
    fun setDefaultUnit(v: String) { defaultUnit = v; prefs.edit().putString("unit", v).apply() }

    // ------------------------------------------------------------ record draft
    var editingId by mutableLongStateOf(0L)
    var draftTime by mutableLongStateOf(System.currentTimeMillis())
    var shop by mutableStateOf("")
    var location by mutableStateOf("")
    var notes by mutableStateOf("")
    var formError by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false)
    val lines = mutableStateListOf<DraftLine>()

    fun newDraft() {
        editingId = 0L
        draftTime = System.currentTimeMillis()
        shop = ""; location = ""; notes = ""; formError = null
        lines.clear()
    }

    suspend fun loadDraft(id: Long): Boolean {
        val r = db.records().get(id) ?: return false
        val es = db.records().entries(id)
        newDraft()
        editingId = r.id
        draftTime = r.createdAt
        shop = r.shop; location = r.location; notes = r.note
        es.forEach { e ->
            val std = UNITS.contains(e.unit) && e.unit != "Other"
            val l = DraftLine(e.productId, e.productName, if (std) e.unit else "Other")
            l.qty = fmtQty(e.quantity)
            if (!std) l.custom = e.unit
            l.note = e.note
            lines.add(l)
        }
        return true
    }

    fun addProducts(ps: List<Product>) {
        val u = if (defaultUnit in UNITS) defaultUnit else "Box"
        ps.forEach { p -> if (lines.none { it.productId == p.id }) lines.add(DraftLine(p.id, p.name, u)) }
    }

    private fun validate(): String? {
        if (lines.isEmpty()) return "Add at least one product to the record."
        for (l in lines) {
            if (parseQty(l.qty) == null) return "Enter a valid quantity greater than zero for \"${l.name}\"."
            if (l.unit == "Other" && l.custom.isBlank()) return "Enter a custom unit name for \"${l.name}\"."
        }
        return null
    }

    /** Returns the saved record id, or null when validation/saving failed (formError is set). */
    suspend fun save(): Long? {
        if (saving) return null
        val err = validate()
        if (err != null) { formError = err; return null }
        saving = true
        try {
            val rec = CollectionRecord(id = editingId, createdAt = draftTime, shop = shop.trim(), location = location.trim(), note = notes.trim())
            val es = lines.map {
                Entry(recordId = 0, productId = it.productId, productName = it.name, quantity = parseQty(it.qty)!!,
                    unit = if (it.unit == "Other") it.custom.trim() else it.unit, note = it.note.trim())
            }
            return try {
                val id = if (editingId == 0L) db.records().saveNew(rec, es) else { db.records().saveEdit(rec, es); editingId }
                formError = null
                id
            } catch (e: Exception) {
                formError = "Could not save the record: ${e.message ?: "database error"}"
                null
            }
        } finally {
            saving = false
        }
    }

    suspend fun deleteRecord(id: Long): Boolean = try { db.records().delete(id); true } catch (e: Exception) { message = "Could not delete: ${e.message}"; false }

    // ------------------------------------------------------------ products
    suspend fun addProduct(name: String, category: String, notes: String, force: Boolean): AddResult {
        val n = name.trim().replace(Regex("\\s+"), " ")
        if (n.isEmpty()) return AddResult.Invalid("Enter a product name.")
        return try {
            val all = db.products().allOnce()
            all.firstOrNull { it.name.equals(n, ignoreCase = true) || normName(it.name) == normName(n) }?.let { return AddResult.Exists(it) }
            if (!force) {
                val sim = all.filter { similarNames(it.name, n) }.map { it.name }
                if (sim.isNotEmpty()) return AddResult.Similar(sim.take(4))
            }
            val p = Product(name = n, category = category.trim(), notes = notes.trim())
            AddResult.Created(p.copy(id = db.products().insert(p)))
        } catch (e: Exception) {
            AddResult.Invalid("Could not save the product: ${e.message ?: "database error"}")
        }
    }

    suspend fun updateProduct(p: Product, name: String, category: String, notes: String): String? {
        val n = name.trim().replace(Regex("\\s+"), " ")
        if (n.isEmpty()) return "Enter a product name."
        return try {
            val clash = db.products().allOnce().firstOrNull { it.id != p.id && it.name.equals(n, ignoreCase = true) }
            if (clash != null) return "Another product is already named \"${clash.name}\"."
            db.products().update(p.copy(name = n, category = category.trim(), notes = notes.trim()))
            null
        } catch (e: Exception) { "Could not save the product: ${e.message ?: "database error"}" }
    }

    suspend fun deleteProduct(p: Product) {
        try { db.products().delete(p); message = "Product deleted. Past records keep its name." } catch (e: Exception) { message = "Could not delete: ${e.message}" }
    }

    // ------------------------------------------------------------ export
    suspend fun makeFile(ctx: Context, id: Long, pdf: Boolean): File {
        val r = db.records().get(id) ?: throw IllegalStateException("Record not found.")
        val es = db.records().entries(id)
        return withContext(Dispatchers.Default) { if (pdf) Exporter.pdf(ctx, r, es) else Exporter.png(ctx, r, es) }
    }
}
