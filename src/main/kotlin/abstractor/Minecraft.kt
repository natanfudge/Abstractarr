package abstractor

import kotlinx.serialization.json.*
import metautils.util.downloadUtfStringFromUrl

object Minecraft {
    data class Library(val url: String, val filePath: String)

//    private val json = Json(JsonConfiguration.Stable)

    fun downloadVersionManifestList(): JsonObject {
        val versionManifestsString =
            downloadUtfStringFromUrl("https://launchermeta.mojang.com/mc/game/version_manifest.json")
        return Json.parseToJsonElement(versionManifestsString).jsonObject
    }

    fun downloadVersionManifest(versionManifestList: JsonObject, version: String): JsonObject {
        val url = versionManifestList.getValue("versions").jsonArray
            .map { it.jsonObject }
            .find { it.getValue("id").jsonPrimitive.content == version }
            ?.getValue("url")?.jsonPrimitive?.content
            ?: error("No such abstractor.Minecraft version '$version'")
        return Json.parseToJsonElement(downloadUtfStringFromUrl(url)).jsonObject
    }

    fun getLibraryUrls(versionManifest: JsonObject): List<Library> {
        return versionManifest.getValue("libraries").jsonArray.map {
            val artifact = it.jsonObject.getValue("downloads").jsonObject.getValue("artifact").jsonObject
            Library(
                artifact.getValue("url").jsonPrimitive.content,
                artifact.getValue("path").jsonPrimitive.content
            )
        }.distinctBy { it.filePath }
    }

    fun downloadLibraryUrlsFor(version: String): List<Library> =
        getLibraryUrls(
            downloadVersionManifest(
                downloadVersionManifestList(), version
            )
        )
}