package io.legado.app.model

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webbook.BookChapterList
import io.legado.app.model.webbook.BookContent
import io.legado.app.model.webbook.BookInfo
import io.legado.app.model.webbook.BookList
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class WebBook(val bookSource: BookSource) {

    val sourceUrl: String
        get() = bookSource.bookSourceUrl

    /**
     * 搜索
     */
    fun searchBook(
        key: String,
        page: Int? = 1,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<List<SearchBook>> {
        return Coroutine.async(scope, context) {
            bookSource.searchUrl?.let { searchUrl ->
                val analyzeUrl = AnalyzeUrl(
                    ruleUrl = searchUrl,
                    key = key,
                    page = page,
                    baseUrl = sourceUrl,
                    headerMapF = bookSource.getHeaderMap()
                )
                var baseUrl = analyzeUrl.ruleUrl
                val body = if (analyzeUrl.useWebView) {
                    analyzeUrl.getResultByWebView(bookSource.bookSourceUrl)
                } else {
                    val res = analyzeUrl.getResponseAwait()
                    baseUrl = NetworkUtils.getUrl(res)
                    res.body()
                }
                BookList.analyzeBookList(
                    body,
                    bookSource,
                    analyzeUrl,
                    baseUrl,
                    true
                )
            } ?: arrayListOf()
        }
    }

    /**
     * 发现
     */
    fun exploreBook(
        url: String,
        page: Int? = 1,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<List<SearchBook>> {
        return Coroutine.async(scope, context) {
            val analyzeUrl = AnalyzeUrl(
                ruleUrl = url,
                page = page,
                baseUrl = sourceUrl,
                headerMapF = bookSource.getHeaderMap()
            )
            var baseUrl = analyzeUrl.ruleUrl
            val body = if (analyzeUrl.useWebView) {
                analyzeUrl.getResultByWebView(bookSource.bookSourceUrl)
            } else {
                val res = analyzeUrl.getResponseAwait()
                baseUrl = NetworkUtils.getUrl(res)
                res.body()
            }
            BookList.analyzeBookList(
                body,
                bookSource,
                analyzeUrl,
                baseUrl,
                false
            )
        }
    }

    /**
     * 书籍信息
     */
    fun getBookInfo(
        book: Book,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<Book> {
        book.type = bookSource.bookSourceType
        return Coroutine.async(scope, context) {
            val body = if (!book.infoHtml.isNullOrEmpty()) {
                book.infoHtml
            } else {
                val analyzeUrl = AnalyzeUrl(
                    book = book,
                    ruleUrl = book.bookUrl,
                    baseUrl = sourceUrl,
                    headerMapF = bookSource.getHeaderMap()
                )
                if (analyzeUrl.useWebView) {
                    bookSource.getContentRule()
                    analyzeUrl.getResultByWebView(bookSource.bookSourceUrl)
                } else {
                    analyzeUrl.getResponseAwait().body()
                }
            }
            BookInfo.analyzeBookInfo(book, body, bookSource, book.bookUrl)
            book
        }
    }

    /**
     * 目录
     */
    fun getChapterList(
        book: Book,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<List<BookChapter>> {
        book.type = bookSource.bookSourceType
        return Coroutine.async(scope, context) {
            val body = if (book.bookUrl == book.tocUrl && !book.tocHtml.isNullOrEmpty()) {
                book.tocHtml
            } else {
                val analyzeUrl = AnalyzeUrl(
                    book = book,
                    ruleUrl = book.tocUrl,
                    baseUrl = book.bookUrl,
                    headerMapF = bookSource.getHeaderMap()
                )
                if (analyzeUrl.useWebView) {
                    analyzeUrl.getResultByWebView(bookSource.bookSourceUrl)
                } else {
                    analyzeUrl.getResponseAwait().body()
                }
            }
            BookChapterList.analyzeChapterList(this, book, body, bookSource, book.tocUrl)
        }
    }

    /**
     * 章节内容
     */
    fun getContent(
        book: Book,
        bookChapter: BookChapter,
        nextChapterUrl: String? = null,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<String> {
        return Coroutine.async(scope, context) {
            if (bookSource.getContentRule().content.isNullOrEmpty()) {
                return@async bookChapter.url
            }
            val body = if (bookChapter.url == book.bookUrl && !book.tocHtml.isNullOrEmpty()) {
                book.tocHtml
            } else {
                val analyzeUrl =
                    AnalyzeUrl(
                        book = book,
                        ruleUrl = bookChapter.url,
                        baseUrl = book.tocUrl,
                        headerMapF = bookSource.getHeaderMap()
                    )
                if (analyzeUrl.useWebView) {
                    analyzeUrl.getResultByWebView(bookSource.bookSourceUrl)
                } else {
                    analyzeUrl.getResponseAwait().body()
                }
            }
            BookContent.analyzeContent(
                this,
                body,
                book,
                bookChapter,
                bookSource,
                bookChapter.url,
                nextChapterUrl
            )
        }
    }
}