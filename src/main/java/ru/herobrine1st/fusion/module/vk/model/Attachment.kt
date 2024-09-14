package ru.herobrine1st.fusion.module.vk.model

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.IOException
import java.time.Instant

@JsonDeserialize(using = AttachmentDeserializer::class)
sealed class Attachment {
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_OBJECT
    )
    @JsonSubTypes(
        Type(Photo::class),
        Type(Video::class),
        Type(Audio::class),
        Type(Document::class),
        Type(Graffiti::class),
        Type(Link::class),
        Type(Note::class),
        Type(ApplicationContext::class),
        Type(Poll::class),
        Type(WikiPage::class),
        Type(PhotoAlbum::class),
        Type(PhotosList::class),
        Type(MarketItem::class),
        Type(MarketCollection::class),
        Type(Sticker::class)
    )
    @JsonDeserialize // To use internal deserializer
    sealed class Internal : Attachment()
}

@JsonTypeName("photo")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonDeserialize
data class Photo(
    val id: Int,
    val albumId: Int,
    val ownerId: Int,
    val userId: Int,
    @JsonProperty(required = false)
    val postId: Int = -1,
    @JsonProperty(required = false)
    val accessKey: String? = null,
    @JsonProperty("text")
    val description: String,
    val date: Instant,
    val sizes: List<Size>,
    @JsonProperty(required = false) // can be unavailable for photos uploaded before 2012.
    val width: Int = -1,
    @JsonProperty(required = false) // can be unavailable for photos uploaded before 2012.
    val height: Int = -1
) : Attachment.Internal() {
    data class Size(
        val type: String,
        val url: String,
        val width: Int,
        val height: Int
    )
}

@JsonTypeName("video")
@JsonIgnoreProperties(ignoreUnknown = true)
object Video : Attachment.Internal() // Unsupported

@JsonTypeName("audio")
@JsonIgnoreProperties(ignoreUnknown = true)
object Audio : Attachment.Internal() // Unsupported

@JsonTypeName("doc")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Document(
    val id: Int,
    @JsonProperty("owner_id")
    val ownerId: Int,
    val title: String,
    val size: Int,
    val ext: String,
    val url: String,
    val date: Instant, //Unix time
    val type: Type,
    val preview: Preview
) : Attachment.Internal() {
    @Suppress("unused")
    enum class Type(@JsonValue val int: Int) {
        Text(1),
        Archive(2),
        Gif(3),
        Image(4),
        Audio(5),
        Video(6),
        Ebook(7),
        Undefined(8)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Preview(
        val photo: Photo?,
        val graffiti: Graffiti?,
        @JsonProperty("audio_message")
        val audioMessage: AudioMessage?
    ) {
        data class Photo(val sizes: List<Size>) {
            data class Size(
                @JsonProperty("src")
                val url: String,
                val width: Int,
                val height: Int,
                val type: Type
            ) {
                @Suppress("unused")
                enum class Type(@JsonValue val char: Char) {
                    Proportional100px('s'),
                    Proportional130px('m'),
                    Proportional604px('x'),
                    Proportional807px('y'),
                    Proportional1080x1050px('z'),
                    Original('o'),
                }
            }
        }

        data class Graffiti(
            @JsonProperty("src")
            val url: String,
            val width: Int,
            val height: Int
        )

        @JsonTypeName("audio_message")
        data class AudioMessage(
            val duration: Int,
            val waveform: List<Int>,
            @JsonProperty("link_ogg")
            val linkOgg: String,
            @JsonProperty("link_mp3")
            val linkMp3: String,
        )
    }
}

@JsonTypeName("graffiti")
@JsonIgnoreProperties(ignoreUnknown = true)
object Graffiti : Attachment.Internal() // Unsupported

@JsonTypeName("link")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Link(
    val url: String?,
    val title: String?,
    val caption: String?,
    val description: String?,
    val photo: Photo?
) : Attachment.Internal()

@JsonTypeName("note")
@JsonIgnoreProperties(ignoreUnknown = true)
object Note : Attachment.Internal() // Unsupported

@JsonTypeName("app")
@JsonIgnoreProperties(ignoreUnknown = true)
object ApplicationContext : Attachment.Internal() // Unsupported

@JsonTypeName("poll")
@JsonIgnoreProperties(ignoreUnknown = true)
object Poll : Attachment.Internal() // Unsupported

@JsonTypeName("page")
@JsonIgnoreProperties(ignoreUnknown = true)
object WikiPage : Attachment.Internal() // Unsupported

@JsonTypeName("album")
@JsonIgnoreProperties(ignoreUnknown = true)
object PhotoAlbum : Attachment.Internal() // Unsupported

@JsonTypeName("photos_list")
@JsonIgnoreProperties(ignoreUnknown = true)
object PhotosList : Attachment.Internal() // Unsupported

@JsonTypeName("market")
@JsonIgnoreProperties(ignoreUnknown = true)
object MarketItem : Attachment.Internal() // Unsupported

@JsonTypeName("market_album")
@JsonIgnoreProperties(ignoreUnknown = true)
object MarketCollection : Attachment.Internal() // Unsupported

@JsonTypeName("sticker")
@JsonIgnoreProperties(ignoreUnknown = true)
object Sticker : Attachment.Internal() // Unsupported

// 1. Remove "type" field
// 2. Let jackson deserialize
class AttachmentDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<Attachment>(vc) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): Attachment {
        val node = parser.codec.readTree<JsonNode>(parser)
        if (node !is ObjectNode) throw object : JsonProcessingException("Attachment node is not ObjectNode") {}
        node.remove("type")
        return parser.codec.treeToValue(node, Attachment.Internal::class.java)
    }
}
