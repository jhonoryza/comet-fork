package sh.zeron.android.data

import org.json.JSONArray
import org.json.JSONObject

/** Registry row — wire shape `{kind,id,seq,deleted,delHlc?,fields,clocks}`. */
data class RegistryRow(
    val kind: String,
    val id: String,
    val deleted: Boolean,
    val fields: JSONObject,
) {
    fun field(name: String): String? = fields.optString(name).takeIf { it.isNotEmpty() && !fields.isNull(name) }
    fun fieldLong(name: String): Long? = if (fields.has(name) && !fields.isNull(name)) fields.optLong(name) else null

    companion object {
        fun parse(o: JSONObject): RegistryRow = RegistryRow(
            kind = o.getString("kind"),
            id = o.getString("id"),
            deleted = o.optBoolean("deleted", false),
            fields = o.optJSONObject("fields") ?: JSONObject(),
        )
    }
}

/**
 * Client-side registry table: authoritative rows + LWW-field overlay (Offset).
 * Android is a viewer — it doesn't push ops yet, so state/rows frames replace
 * rows wholesale. `update` never creates rows; merge respects per-field clocks.
 */
class RegistryDoc {
    private val rows = mutableMapOf<String, RegistryRow>() // key = kind:id

    fun applyState(full: Boolean, rowsIn: List<RegistryRow>, cursor: Long) {
        if (full) rows.clear()
        rowsIn.forEach { rows["${it.kind}:${it.id}"] = it }
    }

    fun overlayRows(kind: String): List<RegistryRow> =
        rows.values.filter { it.kind == kind && !it.deleted }.sortedBy { it.id }

    fun overlayRow(kind: String, id: String): RegistryRow? = rows["$kind:$id"]?.takeIf { !it.deleted }

    fun clear() = rows.clear()
}

/** Project registry rows into the workspace/domain models the UI reads. */
class RegistryAdapter(private val doc: RegistryDoc) {
    fun chats(): List<ChatRow> = doc.overlayRows("chats").map { row ->
        ChatRow(
            id = row.id,
            title = row.field("title") ?: row.field("id"),
            archived = row.field("archived")?.toBooleanStrictOrNull() ?: false,
            spaceId = row.field("spaceId"),
        )
    }.sortedWith(compareByDescending<ChatRow> { it.archived }.thenBy { it.title })

    fun spaces(): List<SpaceRow> = doc.overlayRows("spaces").map { row ->
        SpaceRow(id = row.id, path = row.field("path") ?: row.id)
    }
}