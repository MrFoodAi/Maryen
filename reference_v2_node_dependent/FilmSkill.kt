package com.maryen.app.skills.film

import android.content.Context
import com.maryen.app.core.llm.LlmEngine
import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.orchestrator.Intent
import com.maryen.app.node.NodeClient
import com.maryen.app.skills.Skill
import com.maryen.app.skills.SkillResult
import com.maryen.app.skills.image.ImageSkill
import com.maryen.app.skills.video.VideoSkill
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Corto cinematografico end-to-end.
 * Pipeline:
 *   1. LLM scrive sceneggiatura strutturata JSON (scene, dialoghi, mood).
 *   2. Per ogni scena: keyframe via ImageSkill (CINEMATIC, ratio 16:9).
 *   3. Estrai face-embedding dal protagonista, riusala su ogni scena (consistency).
 *   4. VideoSkill IMG2VID per ogni clip.
 *   5. MusicSkill colonna sonora, VoiceClone doppiaggio battute.
 *   6. Montaggio su nodo (ffmpeg) con color grade Kodak 2383.
 *
 * Senza nodo: produce solo storyboard (immagini + sceneggiatura PDF).
 */
class FilmSkill(
    private val ctx: Context,
    private val llm: LlmEngine,
    private val image: ImageSkill,
    private val video: VideoSkill,
    private val node: NodeClient
) : Skill {
    override val id = "film"
    override val label = "Produci corto"
    override val requiresConsent = false

    override suspend fun run(intent: Intent, memory: MemoryStore): SkillResult {
        val brief = intent.args["brief"]?.toString()
            ?: return SkillResult(false, "Dammi il soggetto del corto.")
        val minutes = (intent.args["minutes"] as? Int) ?: 2
        val genre = intent.args["genre"]?.toString() ?: "drammatico"

        val scriptJson = llm.completeJson(
            system = SYSTEM_SCRIPT,
            user = "Brief: $brief\nDurata: $minutes minuti\nGenere: $genre"
        )
        val script = JSONObject(scriptJson.cleanJson())
        val scenes: JSONArray = script.getJSONArray("scenes")

        val outDir = File(ctx.filesDir, "gen/film_${System.currentTimeMillis()}").apply { mkdirs() }
        File(outDir, "script.json").writeText(scriptJson)

        val keyframes = mutableListOf<String>()
        for (i in 0 until scenes.length()) {
            val s = scenes.getJSONObject(i)
            val kfPrompt = "${s.getString("visual")}, cinematic, 35mm, anamorphic, Kodak 2383, ${genre}"
            val kfIntent = Intent(
                skill = "image",
                args = mapOf(
                    "prompt" to kfPrompt,
                    "width" to 1344, "height" to 768,
                    "hq" to true,
                    "faceId" to s.optString("characterId", null)
                ),
                confidence = 1.0
            )
            val r = image.run(kfIntent, memory)
            keyframes += r.artifacts.firstOrNull().orEmpty()
        }

        if (!node.isAvailable()) {
            return SkillResult(
                ok = true,
                spokenSummary = "Senza il nodo PC ho preparato solo lo storyboard: ${keyframes.size} fotogrammi e la sceneggiatura.",
                artifacts = keyframes + File(outDir, "script.json").absolutePath
            )
        }

        val finalMp4 = File(outDir, "final.mp4")
        node.runFilm(
            scriptPath = File(outDir, "script.json").absolutePath,
            keyframes = keyframes,
            outPath = finalMp4.absolutePath,
            colorGrade = "kodak_2383",
            grain = 0.15,
            letterbox = true
        )

        memory.writeArtifact(kind = "film", path = finalMp4.absolutePath, prompt = brief)
        return SkillResult(
            ok = true,
            spokenSummary = "Corto pronto, ${scenes.length()} scene, durata stimata ${minutes} minuti.",
            artifacts = listOf(finalMp4.absolutePath)
        )
    }

    private fun String.cleanJson(): String =
        trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    companion object {
        private val SYSTEM_SCRIPT = """
            Sei sceneggiatore cinematografico italiano. Restituisci SOLO JSON:
            {
              "title": "...",
              "logline": "...",
              "characters": [{"id":"hero","name":"...","look":"..."}],
              "scenes": [
                {"id":1,"visual":"descrizione visiva ricca","characterId":"hero",
                 "duration":6,"dialogue":[{"who":"hero","line":"..."}],
                 "mood":"tense|warm|melancholic"}
              ],
              "score": {"genre":"orchestral","mood":"..."}
            }
        """.trimIndent()
    }
}
