data class Post(
    val id: Long,
    val authorId: Long,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    var attachment: Attachment? = null,
)

data class Attachment(
    val url: String,
    val description: String,
    val type: AttachmentType,
)
enum class AttachmentType {
    IMAGE
}
data class Comment(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    var author: Author?,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
)

data class Author(
    val id: Long,
    val name: String,
    val avatar: String,
)

data class PostWithComments(
    val post: Post,
    val author: Author,
    val comments: List<Comment>,
)