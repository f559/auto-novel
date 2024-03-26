package infra.web.providers

import domain.entity.Page
import domain.entity.WebNovelAttention
import domain.entity.WebNovelAuthor
import domain.entity.WebNovelType
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.*

data class RemoteNovelMetadata(
    val title: String,
    val authors: List<WebNovelAuthor>,
    val type: WebNovelType,
    val attentions: List<WebNovelAttention>,
    val keywords: List<String>,
    val points: Int?,
    val totalCharacters: Int,
    val introduction: String,
    val toc: List<TocItem>,
) {
    data class TocItem(
        val title: String,
        val chapterId: String? = null,
        val createAt: Instant? = null,
    )
}

data class RemoteChapter(
    val paragraphs: List<String>,
)

data class RemoteNovelListItem(
    val novelId: String,
    val title: String,
    val attentions: List<WebNovelAttention>,
    val keywords: List<String>,
    val extra: String,
)

interface WebNovelProvider {
    suspend fun getRank(options: Map<String, String>): Page<RemoteNovelListItem>
    suspend fun getMetadata(novelId: String): RemoteNovelMetadata
    suspend fun getChapter(novelId: String, chapterId: String): RemoteChapter
}

suspend fun HttpResponse.document(): Document = Jsoup.parse(body<String>())
suspend fun HttpResponse.json(): JsonObject = body()

// Datetime util
fun parseJapanDateString(pattern: String, dateString: String): Instant =
    SimpleDateFormat(pattern)
        .apply { timeZone = TimeZone.getTimeZone("Asia/Tokyo") }
        .parse(dateString)
        .toInstant()
        .toKotlinInstant()

// Json util
fun JsonObject.boolean(field: String) = get(field)!!.jsonPrimitive.boolean
fun JsonObject.int(field: String) = get(field)!!.jsonPrimitive.int
fun JsonObject.array(field: String) = get(field)!!.jsonArray

fun JsonObject.string(field: String) = get(field)!!.jsonPrimitive.content
fun JsonObject.stringOrNull(field: String) = get(field)!!.jsonPrimitive.contentOrNull

fun JsonObject.obj(field: String) = get(field)!!.jsonObject
fun JsonObject.objOrNull(field: String) = get(field)!!.takeUnless { it is JsonNull }?.jsonObject

// Exception
class NovelIdShouldBeReplacedException(
    providerId: String,
    targetNovelId: String,
) : Exception("小说ID不合适，应当使用：/${providerId}/${targetNovelId}")

