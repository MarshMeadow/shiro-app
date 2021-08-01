package com.lagradost.shiro.utils.extractors

import com.lagradost.shiro.utils.ExtractorApi
import com.lagradost.shiro.utils.ExtractorLink
import com.lagradost.shiro.utils.Qualities
import com.lagradost.shiro.utils.ShiroApi.Companion.USER_AGENT
import com.lagradost.shiro.utils.mvvm.logError

class MultiQuality : ExtractorApi() {
    override val name: String = "MultiQuality"
    override val mainUrl: String = "https://gogo-play.net"
    private val sourceRegex = Regex("""file:\s*'(.*?)',label:\s*'(.*?)'""")
    private val m3u8Regex = Regex(""".*?(\d*).m3u8""")
    private val urlRegex = Regex("""(.*?)([^/]+$)""")
    override val requiresReferer = false

    override fun getExtractorUrl(id: String): String {
        return "$mainUrl/loadserver.php?id=$id"
    }

    private fun getQuality(string: String): Int {
        return when (string) {
            "360" -> Qualities.SD.value
            "480" -> Qualities.SD.value
            "720" -> Qualities.HD.value
            "1080" -> Qualities.FullHd.value
            else -> Qualities.Unknown.value
        }
    }

    override fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        try {
            val extractedLinksList: MutableList<ExtractorLink> = mutableListOf()
            with(khttp.get(url)) {
                sourceRegex.findAll(this.text).forEach { sourceMatch ->
                    val extractedUrl = sourceMatch.groupValues[1]
                    // Trusting this isn't mp4, may fuck up stuff
                    if (extractedUrl.endsWith(".m3u8")) {
                        val headers = mapOf("User-Agent" to USER_AGENT, "Referer" to url)
                        with(khttp.get(extractedUrl, headers = headers)) {
                            m3u8Regex.findAll(this.text).forEach { match ->
                                extractedLinksList.add(
                                    ExtractorLink(
                                        "$name ${match.groupValues[1]}p",
                                        urlRegex.find(this.url)!!.groupValues[1] + match.groupValues[0],
                                        url,
                                        getQuality(match.groupValues[1]),
                                        isM3u8 = true
                                    )
                                )
                            }

                        }
                    } else if (extractedUrl.endsWith(".mp4")) {
                        extractedLinksList.add(
                            ExtractorLink(
                                "$name ${sourceMatch.groupValues[2].removeSuffix(" P")}",
                                extractedUrl,
                                url.replace(" ", "%20"),
                                Qualities.Unknown.value,
                            )
                        )
                    }
                }
                return extractedLinksList
            }
        } catch (e: Exception) {
logError(e)
        }
        return null
    }
}