import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val gson = Gson()
val BASE_URL = "http://127.0.0.1:9999/api"
val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()

fun main() {
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            try {
                getAllPosts(client)
                    .map { post ->
                        async {
                            val author = getAuthor(client, post.authorId)
                            val comments = getPostComments(client, post.id)
                                .map { comment ->
                                    async {
                                        comment.author = getAuthor(client, comment.authorId)
                                        comment
                                    }
                                }.awaitAll()

                            PostWithComments(
                                post,
                                author,
                                comments
                            )
                        }
                    }.awaitAll()
                    .also {
                        it.forEach {
                            println(it)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
    Thread.sleep(20_000L)
}

suspend fun OkHttpClient.apiCall(url: String): Response {
    return suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(::newCall)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
    }
}

suspend fun <T> makeRequest(url: String, client: OkHttpClient, typeToken: TypeToken<T>): T =
    withContext(Dispatchers.IO) {
        client.apiCall(url)
            .let { response ->
                if (!response.isSuccessful) {
                    response.close()
                    throw RuntimeException(response.message)
                }
                val body = response.body ?: throw RuntimeException("response body is null")
                gson.fromJson(body.string(), typeToken.type)
            }
    }

suspend fun getAllPosts(client: OkHttpClient): List<Post> {
    return makeRequest("$BASE_URL/slow/posts/", client, object : TypeToken<List<Post>>() {})
}

suspend fun getPostComments(client: OkHttpClient, postId: Long): List<Comment> {
    return makeRequest("$BASE_URL/slow/posts/$postId/comments", client, object : TypeToken<List<Comment>>() {})
}

suspend fun getAuthor(client: OkHttpClient, authorId: Long): Author {
    return makeRequest("$BASE_URL/authors/$authorId", client, object : TypeToken<Author>() {})
}