package com.maryen.app.skills.site

import android.content.Context
import com.maryen.app.core.llm.LlmEngine
import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.orchestrator.Intent
import com.maryen.app.node.NodeClient
import com.maryen.app.skills.Skill
import com.maryen.app.skills.SkillResult
import com.maryen.app.skills.image.ImageSkill
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Costruttore siti.
 * Stack supportati: HTML, ASTRO, NEXT, SVELTE, VUE.
 * Output: cartella + zip pronto da hostare, e (se nodo) deploy via Caddy.
 */
class SiteBuilderSkill(
    private val ctx: Context,
    private val llm: LlmEngine,
    private val image: ImageSkill,
    private val node: NodeClient
) : Skill {
    override val id = "site"
    override val label = "Costruisci sito"
    override val requiresConsent = true   // tocca rete se deploy

    override suspend fun run(intent: Intent, memory: MemoryStore): SkillResult {
        val brief = intent.args["brief"]?.toString()
            ?: return SkillResult(false, "Dimmi che sito vuoi.")
        val stack = (intent.args["stack"]?.toString() ?: "html").lowercase()
        val deploy = (intent.args["deploy"] as? Boolean) ?: false

        val specRaw = llm.completeJson(
            system = SYSTEM_SPEC,
            user = "Brief: $brief\nStack scelto: $stack"
        )
        val spec = JSONObject(specRaw.cleanJson())

        val outDir = File(ctx.filesDir, "gen/site_${System.currentTimeMillis()}").apply { mkdirs() }

        // Pagine
        val pages: JSONArray = spec.getJSONArray("pages")
        for (i in 0 until pages.length()) {
            val p = pages.getJSONObject(i)
            val code = llm.completeText(
                system = "Genera il file completo per la pagina richiesta, stack=$stack. Solo codice, niente spiegazioni.",
                user = "Pagina: ${p.getString("name")}\nDescrizione: ${p.getString("desc")}\nComponenti: ${p.optJSONArray("components")}"
            )
            val ext = when (stack) {
                "html" -> "html"; "astro" -> "astro"; "next" -> "tsx"
                "svelte" -> "svelte"; "vue" -> "vue"; else -> "html"
            }
            File(outDir, "${p.getString("name")}.$ext").writeText(code)
        }

        // Asset visivi
        val assets = spec.optJSONArray("imagePrompts") ?: JSONArray()
        for (i in 0 until assets.length()) {
            val ap = assets.getJSONObject(i)
            val r = image.run(
                Intent("image", mapOf(
                    "prompt" to ap.getString("prompt"),
                    "width" to ap.optInt("w", 1280),
                    "height" to ap.optInt("h", 720),
                    "hq" to true
                ), 1.0),
                memory
            )
            r.artifacts.firstOrNull()?.let {
                File(it).copyTo(File(outDir, "assets/${ap.getString("name")}.png").apply { parentFile?.mkdirs() }, overwrite = true)
            }
        }

        // Zip
        val zip = File(outDir.parentFile, "${outDir.name}.zip")
        zipDir(outDir, zip)

        // Deploy opzionale via nodo
        val liveUrl = if (deploy && node.isAvailable()) {
            node.deployStatic(zip.absolutePath, subdomain = spec.optString("slug", "sito"))
        } else null

        memory.writeArtifact("site", zip.absolutePath, brief)
        return SkillResult(
            ok = true,
            spokenSummary = liveUrl?.let { "Sito generato e pubblicato su $it." }
                ?: "Sito generato. Zip pronto.",
            artifacts = listOf(zip.absolutePath)
        )
    }

    private fun zipDir(src: File, dest: File) {
        ZipOutputStream(dest.outputStream()).use { zos ->
            src.walkTopDown().filter { it.isFile }.forEach { f ->
                zos.putNextEntry(ZipEntry(f.relativeTo(src).path))
                f.inputStream().copyTo(zos)
                zos.closeEntry()
            }
        }
    }

    private fun String.cleanJson(): String =
        trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    companion object {
        private val SYSTEM_SPEC = """
            Sei architetto di siti. Restituisci SOLO JSON:
            {
              "slug":"...",
              "pages":[{"name":"index","desc":"...","components":["hero","cta","features"]}],
              "imagePrompts":[{"name":"hero","prompt":"...","w":1920,"h":1080}]
            }
        """.trimIndent()
    }
}
