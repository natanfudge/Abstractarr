package abstractor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonObject
import metautils.util.downloadUtfStringFromUrl

object Minecraft {
    data class Library(val url: String, val filePath: String)

    private val json = Json(JsonConfiguration.Stable)

    fun downloadVersionManifestList(): JsonObject {
        val versionManifestsString = downloadUtfStringFromUrl("https://launchermeta.mojang.com/mc/game/version_manifest.json")
        return json.parseJson(versionManifestsString).jsonObject
    }

    fun downloadVersionManifest(versionManifestList: JsonObject, version: String): JsonObject {
        val url = versionManifestList.getArray("versions")
            .map { it.jsonObject }
            .find { it.getPrimitive("id").content == version }
            ?.getPrimitive("url")?.content
            ?: error("No such abstractor.Minecraft version '$version'")
        return json.parseJson(downloadUtfStringFromUrl(url)).jsonObject
    }

    fun getLibraryUrls(versionManifest: JsonObject): List<Library> {
        return versionManifest.getArray("libraries").map {
            val artifact = it.jsonObject.getObject("downloads").getObject("artifact")
            Library(
                artifact.getPrimitive("url").content,
                artifact.getPrimitive("path").content
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