package com.garam.whenwheremeet.data.repository

import kotlinx.browser.document
import org.w3c.dom.HTMLMetaElement

data class WebFirebaseConfig(
    val apiKey: String,
    val projectId: String,
    val databaseId: String,
) {
    val isConfigured: Boolean = apiKey.isNotBlank() && projectId.isNotBlank() && databaseId.isNotBlank()
}

fun webFirebaseConfig(): WebFirebaseConfig = WebFirebaseConfig(
    apiKey = metaContent("wwm-firebase-api-key"),
    projectId = metaContent("wwm-firebase-project-id"),
    databaseId = metaContent("wwm-firebase-database-id").ifBlank { "default" },
)

private fun metaContent(name: String): String =
    (document.querySelector("meta[name='$name']") as? HTMLMetaElement)
        ?.content
        ?.trim()
        .orEmpty()
