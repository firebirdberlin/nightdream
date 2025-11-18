package com.firebirdberlin.nightdream.services

import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object CoroutineBridge {

    fun parseFeed(parser: RssParser, url: String): CompletableFuture<RssChannel> = GlobalScope.future(Dispatchers.IO) {
        parser.getRssChannel(url)
    }
}
