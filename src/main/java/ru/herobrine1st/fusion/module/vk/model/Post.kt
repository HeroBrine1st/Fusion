package ru.herobrine1st.fusion.module.vk.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.time.Instant

// Fuck it @JsonIgnoreProperties("geo", "can_pin", "can_edit", "can_delete", "post_source", "ads_easy_promote", "hash", "type")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Post(
    val id: Int,
    val ownerId: Int,
    val fromId: Int,
    @JsonProperty(required = false)
    val createdBy: Int = -1,
    val date: Instant,
    val text: String,
    @JsonProperty(required = false)
    val replyOwnerId: Int = -1,
    @JsonProperty(required = false)
    val replyPostId: Int = -1,
    @JsonProperty(required = false)
    val friendsOnly: Boolean = false,
    val comments: Comments,
    val likes: Likes,
    val reposts: Reposts,
    val views: Views,
    val postType: PostType,
    val attachments: List<Attachment>,
    @JsonProperty(required = false)
    val signerId: Int = -1,
    @JsonProperty(required = false)
    val copy_history: List<Post> = emptyList(),
    @JsonProperty(required = false)
    val isPinned: Boolean = false,
    @JsonProperty(required = false)
    val markedAsAds: Boolean = false,
    @JsonProperty(required = false)
    val isFavorite: Boolean = false
)

data class Views(val count: Int)

@Suppress("unused")
enum class PostType {
    POST,
    COPY,
    REPLY,
    POSTPONE,
    SUGGEST;

    @JsonValue
    val apiName = name.lowercase()
}

@JsonIgnoreProperties("user_reposted")
data class Reposts(
    val count: Int
)

@JsonIgnoreProperties("user_likes", "can_like", "can_publish")
data class Likes(
    val count: Int,
)

data class Comments(
    val count: Int,
    @JsonProperty(required = false)
    val canPost: Boolean = false,
    @JsonProperty(required = false)
    val groupsCanPost: Boolean = false,
)
