package com.vx7.marketing

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar

val Navy = Color(0xFF0B1228)
val Blue = Color(0xFF2677E8)
val Gold = Color(0xFFD5B05D)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF5B9BFF), onPrimary = Navy, secondary = Gold, onSecondary = Navy,
        background = Navy, surface = Color(0xFF141E3C), onBackground = Color(0xFFF1F4FB), onSurface = Color(0xFFF1F4FB),
        surfaceVariant = Color(0xFF1C2A55), onSurfaceVariant = Color(0xFFB8C2DD)
    ) else lightColorScheme(
        primary = Blue, onPrimary = Color.White, secondary = Gold, onSecondary = Navy,
        background = Color(0xFFF3F6FC), surface = Color.White, onBackground = Navy, onSurface = Navy,
        surfaceVariant = Color(0xFFE6ECF8), onSurfaceVariant = Color(0xFF4B566E)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

fun contentPad(pad: PaddingValues) = PaddingValues(
    start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 12.dp, bottom = pad.calculateBottomPadding() + 24.dp
)

@Composable
fun Bar(title: String, onBack: (() -> Unit)?) {
    Surface(color = Navy) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            else Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionTitle(t: String) = Text(t, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))

@Composable
fun Empty(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Confirm(title: String, text: String, yes: String, destructive: Boolean, onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onYes) { Text(yes, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onNo) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePick(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val st = rememberDatePickerState(initialSelectedDateMillis = localToUtcPicker(initial))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { val s = st.selectedDateMillis; onDismiss(); if (s != null) onPick(utcPickerToLocal(s)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(st) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePick(initial: Long, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val c = Calendar.getInstance().apply { timeInMillis = initial }
    val st = rememberTimePickerState(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onDismiss(); onPick(st.hour, st.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(st) }
    )
}

@Composable
fun PeriodBar(ps: PeriodState) {
    var pick by remember { mutableStateOf("") }
    var tmpFrom by remember { mutableLongStateOf(0L) }
    val chips = listOf("all" to "All", "today" to "Today", "week" to "Last 7 Days", "month" to "This Month", "date" to "Pick Date", "range" to "Custom Range")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { (k, l) ->
            FilterChip(selected = ps.key == k, onClick = {
                when (k) {
                    "date" -> pick = "date"
                    "range" -> pick = "from"
                    else -> ps.apply(k)
                }
            }, label = { Text(l) })
        }
    }
    Text(ps.label(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    val now = System.currentTimeMillis()
    if (pick == "date") DatePick(now, { pick = "" }) { d -> ps.set("date", dayStart(d), dayEnd(d)) }
    if (pick == "from") DatePick(now, { pick = "" }) { d -> tmpFrom = d; pick = "to" }
    if (pick == "to") DatePick(tmpFrom, { pick = "" }) { d -> ps.set("range", dayStart(minOf(tmpFrom, d)), dayEnd(maxOf(tmpFrom, d))) }
}

fun doShare(scope: CoroutineScope, ctx: Context, vm: Vm, id: Long, pdf: Boolean, busy: (Boolean) -> Unit) {
    scope.launch {
        busy(true)
        try {
            val f = vm.makeFile(ctx, id, pdf)
            Exporter.share(ctx, f, if (pdf) "application/pdf" else "image/png", if (pdf) "VX7 Product Collection Record (PDF)" else "VX7 Product Collection Record")
        } catch (e: Exception) {
            vm.message = "Could not create the ${if (pdf) "PDF" else "image"}: ${e.message ?: "unknown error"}"
        } finally {
            busy(false)
        }
    }
}

@Composable
fun RecordCard(row: RecordRow, onOpen: () -> Unit, actions: (@Composable RowScope.() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onOpen), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(row.record.shop.ifBlank { "Collection record" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${fmtDate(row.record.createdAt)}  \u2022  ${fmtTime(row.record.createdAt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${row.entryCount} product ${if (row.entryCount == 1) "entry" else "entries"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            if (actions != null) Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

fun RecordActions(vm: Vm, id: Long, go: (String) -> Unit, onDelete: (Long) -> Unit): @Composable RowScope.() -> Unit = {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    IconButton(enabled = !busy, onClick = { doShare(scope, ctx, vm, id, true) { busy = it } }) { Icon(Icons.Default.PictureAsPdf, "Share PDF") }
    IconButton(enabled = !busy, onClick = { doShare(scope, ctx, vm, id, false) { busy = it } }) { Icon(Icons.Default.Image, "Share image") }
    IconButton(onClick = { scope.launch { if (vm.loadDraft(id)) go("form") else vm.message = "Record not found." } }) { Icon(Icons.Default.Edit, "Edit record") }
    IconButton(onClick = { onDelete(id) }) { Icon(Icons.Default.Delete, "Delete record", tint = MaterialTheme.colorScheme.error) }
    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
}

// ------------------------------------------------------------------ Dashboard
private val goldBrush = Brush.horizontalGradient(listOf(Color(0xFFF2D58A), Gold, Color(0xFFA9812F)))
private val heroBrush = Brush.linearGradient(listOf(Navy, Color(0xFF123A8C), Color(0xFF0A1C4A)))

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when { h < 12 -> "Good morning"; h < 17 -> "Good afternoon"; else -> "Good evening" }
}

@Composable
fun HomeScreen(vm: Vm, pad: PaddingValues, go: (String) -> Unit) {
    val now = remember { System.currentTimeMillis() }
    val today by remember { vm.db.records().search("", dayStart(now), dayEnd(now), Int.MAX_VALUE) }.collectAsStateWithLifecycle(emptyList())
    val recent by remember { vm.db.records().search("", 0L, Long.MAX_VALUE, 5) }.collectAsStateWithLifecycle(emptyList())
    LazyColumn(contentPadding = contentPad(pad), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(heroBrush).border(1.5.dp, goldBrush, RoundedCornerShape(30.dp))) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.vx7_logo), "VX7 Marketing logo", Modifier.size(68.dp).border(2.dp, goldBrush, CircleShape).padding(3.dp).clip(CircleShape))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("VX7 MARKETING", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 1.5.sp)
                            Text("TRACK  \u2022  PURCHASE  \u2022  GROW", color = Gold, fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(greeting(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(fmtLongDate(now), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassStat("RECORDS TODAY", today.size.toString(), Modifier.weight(1f))
                        GlassStat("PRODUCT ENTRIES", today.sumOf { it.entryCount }.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(goldBrush).clickable { vm.newDraft(); go("form") }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Navy)
                            Spacer(Modifier.width(8.dp))
                            Text("NEW RECORD", color = Navy, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Tile(Icons.Default.Inventory2, "Products") { go("products") }
                Tile(Icons.Default.History, "History") { go("history") }
                Tile(Icons.Default.BarChart, "Reports") { go("reports") }
                Tile(Icons.Default.EditNote, "Notes") { go("notes") }
            }
        }
        item { SectionTitle("TODAY'S RECORDS") }
        if (today.isEmpty()) item { Empty(Icons.Default.Inventory2, "No records yet today. Tap NEW RECORD to start.") }
        items(today, key = { it.record.id }) { r -> TimelineCard(r) { go("detail:${r.record.id}") } }
        item { SectionTitle("RECENT RECORDS") }
        if (recent.isEmpty()) item { Empty(Icons.Default.History, "Your saved records will appear here.") }
        items(recent, key = { "r" + it.record.id }) { r -> TimelineCard(r) { go("detail:${r.record.id}") } }
        item {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { go("backup") }) { Text("Backup & Restore") }
                TextButton(onClick = { go("settings") }) { Text("Settings") }
                TextButton(onClick = { go("about") }) { Text("About") }
            }
        }
    }
}

@Composable
fun GlassStat(label: String, value: String, m: Modifier) {
    Column(m.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.09f)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp)).padding(14.dp)) {
        Text(value, color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun Tile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.width(80.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Color(0xFF0A3E9E)))).border(1.5.dp, goldBrush, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TimelineCard(row: RecordRow, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onOpen), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(Gold, Blue))))
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.record.shop.ifBlank { "Collection record" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${fmtDate(row.record.createdAt)}  \u2022  ${fmtTime(row.record.createdAt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.clip(RoundedCornerShape(50)).background(Gold.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("${row.entryCount} ${if (row.entryCount == 1) "item" else "items"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun Stat(title: String, value: String, m: Modifier) {
    Card(m, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------ Products
@Composable
fun ProductDialog(vm: Vm, product: Product?, initialName: String, onClose: () -> Unit, onSaved: (Product) -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(product?.name ?: initialName) }
    var cat by remember { mutableStateOf(product?.category ?: "") }
    var notes by remember { mutableStateOf(product?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var similar by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (product == null) "Add product" else "Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(100); similar = emptyList(); error = null }, label = { Text("Product name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(cat, { cat = it.take(60) }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it.take(300) }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                if (similar.isNotEmpty()) Text("A similar product already exists: ${similar.joinToString(", ")}. Tap \"Add anyway\" only if this is a different product.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    if (product == null) {
                        when (val r = vm.addProduct(name, cat, notes, similar.isNotEmpty())) {
                            is AddResult.Created -> { onSaved(r.product); onClose() }
                            is AddResult.Exists -> { similar = emptyList(); error = "\"${r.product.name}\" already exists." }
                            is AddResult.Similar -> similar = r.names
                            is AddResult.Invalid -> error = r.message
                        }
                    } else {
                        val e = vm.updateProduct(product, name, cat, notes)
                        if (e == null) { onSaved(product.copy(name = name.trim(), category = cat.trim(), notes = notes.trim())); onClose() } else error = e
                    }
                    busy = false
                }
            }) { Text(if (similar.isNotEmpty()) "Add anyway" else "Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
}

@Composable
fun ProductsScreen(vm: Vm, pad: PaddingValues) {
    var q by rememberSaveable { mutableStateOf("") }
    val list by remember(q) { vm.db.products().search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var deleting by remember { mutableStateOf<Product?>(null) }
    val scope = rememberCoroutineScope()
    LazyColumn(contentPadding = contentPad(pad), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("  Add Product") }
            OutlinedTextField(q, { q = it }, label = { Text("Search products") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        if (list.isEmpty()) item { Empty(Icons.Default.Inventory2, if (q.isBlank()) "No products yet. Add your first product." else "No products match your search.") }
        items(list, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Bold)
                        if (p.category.isNotBlank()) Text(p.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        if (p.notes.isNotBlank()) Text(p.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { editing = p }) { Icon(Icons.Default.Edit, "Edit product") }
                    IconButton(onClick = { deleting = p }) { Icon(Icons.Default.Delete, "Delete product", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    if (adding) ProductDialog(vm, null, "", { adding = false }) { vm.message = "Product added." }
    editing?.let { p -> ProductDialog(vm, p, "", { editing = null }) { vm.message = "Product updated. Past records keep the old name." } }
    deleting?.let { p ->
        Confirm("Delete product?", "\"${p.name}\" will be removed from your product list. Past records keep its name.", "Delete", true,
            { scope.launch { vm.deleteProduct(p) }; deleting = null }, { deleting = null })
    }
}

@Composable
fun ProductPicker(vm: Vm, existing: Set<Long>, onDone: (List<Product>) -> Unit, onClose: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val list by remember(q) { vm.db.products().search(q.trim()) }.collectAsStateWithLifecycle(emptyList())
    val picked = remember { mutableStateListOf<Product>() }
    var adding by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                    Text("Select products", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Button(enabled = picked.isNotEmpty(), onClick = { onDone(picked.toList()) }) { Text("Add (${picked.size})") }
                }
                OutlinedTextField(q, { q = it }, label = { Text("Search products") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Icon(Icons.Default.Add, null); Text("  New product" + if (q.isNotBlank()) ": ${q.trim()}" else "")
                }
                LazyColumn(Modifier.weight(1f)) {
                    if (list.isEmpty()) item { Empty(Icons.Default.Inventory2, if (q.isBlank()) "No products yet. Add one above." else "No match. Use \"New product\" to add it.") }
                    items(list, key = { it.id }) { p ->
                        val already = p.id in existing
                        val on = already || picked.any { it.id == p.id }
                        Row(Modifier.fillMaxWidth().clickable(enabled = !already) {
                            if (on) picked.removeAll { it.id == p.id } else picked.add(p)
                        }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = on, enabled = !already, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                if (already) Text("Already in this record", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else if (p.category.isNotBlank()) Text(p.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    if (adding) ProductDialog(vm, null, q.trim(), { adding = false }) { p -> if (picked.none { it.id == p.id }) picked.add(p) }
}

// ------------------------------------------------------------------ New / Edit record
@Composable
fun UnitMenu(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(value); Icon(Icons.Default.ArrowDropDown, null) }
        DropdownMenu(open, { open = false }) { UNITS.forEach { u -> DropdownMenuItem(text = { Text(u) }, onClick = { onChange(u); open = false }) } }
    }
}

@Composable
fun FormScreen(vm: Vm, pad: PaddingValues, go: (String) -> Unit, back: () -> Unit, replace: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var picker by remember { mutableStateOf(false) }
    var dateDlg by remember { mutableStateOf(false) }
    var timeDlg by remember { mutableStateOf(false) }
    var savedId by rememberSaveable { mutableStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    val kb = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
    LazyColumn(Modifier.imePadding(), contentPadding = contentPad(pad), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { dateDlg = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.DateRange, null); Text("  " + fmtDate(vm.draftTime)) }
                OutlinedButton(onClick = { timeDlg = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.AccessTime, null); Text("  " + fmtTime(vm.draftTime)) }
            }
        }
        item { OutlinedTextField(vm.shop, { vm.shop = it.take(100) }, label = { Text("Shop name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(vm.location, { vm.location = it.take(150) }, label = { Text("Collection location (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PRODUCTS (${vm.lines.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Button(onClick = { picker = true }) { Icon(Icons.Default.Add, null); Text("  Add products") }
            }
        }
        if (vm.lines.isEmpty()) item { Empty(Icons.Default.Inventory2, "No products selected yet. Tap Add products.") }
        items(vm.lines, key = { it.productId }) { l ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(l.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.lines.remove(l) }) { Icon(Icons.Default.Close, "Remove ${l.name}") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(l.qty, { l.qty = it.take(12) }, label = { Text("Quantity") }, singleLine = true, keyboardOptions = kb, modifier = Modifier.weight(1f))
                        UnitMenu(l.unit, { l.unit = it }, Modifier.weight(1f).padding(top = 8.dp))
                    }
                    if (l.unit == "Other") OutlinedTextField(l.custom, { l.custom = it.take(30) }, label = { Text("Custom unit name") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    OutlinedTextField(l.note, { l.note = it.take(200) }, label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
        }
        item { OutlinedTextField(vm.notes, { vm.notes = it.take(1000) }, label = { Text("General notes (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
        item {
            vm.formError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 6.dp)) }
            Button(enabled = !vm.saving, modifier = Modifier.fillMaxWidth().height(52.dp), onClick = {
                val wasEdit = vm.editingId != 0L
                scope.launch {
                    val id = vm.save()
                    if (id != null) {
                        if (wasEdit) { vm.message = "Record updated."; vm.newDraft(); back() } else savedId = id
                    }
                }
            }) { Icon(Icons.Default.Save, null); Text(if (vm.editingId == 0L) "  Save Record" else "  Save Changes") }
        }
    }
    if (picker) ProductPicker(vm, vm.lines.map { it.productId }.toSet(), { vm.addProducts(it); picker = false }, { picker = false })
    if (dateDlg) DatePick(vm.draftTime, { dateDlg = false }) { vm.draftTime = withDate(vm.draftTime, it) }
    if (timeDlg) TimePick(vm.draftTime, { timeDlg = false }) { h, m -> vm.draftTime = withTime(vm.draftTime, h, m) }
    if (savedId != 0L) {
        val id = savedId
        Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Surface(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Record saved", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Your collection record was saved on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { savedId = 0L; vm.newDraft(); replace("detail:$id") }, modifier = Modifier.fillMaxWidth()) { Text("View record") }
                    OutlinedButton(onClick = { savedId = 0L; vm.newDraft() }, modifier = Modifier.fillMaxWidth()) { Text("Create another record") }
                    OutlinedButton(enabled = !busy, onClick = { doShare(scope, ctx, vm, id, true) { busy = it } }, modifier = Modifier.fillMaxWidth()) { Text("Share PDF") }
                    OutlinedButton(enabled = !busy, onClick = { doShare(scope, ctx, vm, id, false) { busy = it } }, modifier = Modifier.fillMaxWidth()) { Text("Share image") }
                }
            }
        }
    }
}
