package com.maryen.app.skills.video

import android.content.Context
import com.maryen.app.core.memory.MemoryStore
import com.maryen.app.core.orchestrator.Intent
import com.maryen.app.node.NodeClient
import com.maryen.app.skills.Skill
import com.maryen.app.skills.SkillResult
import java.io.File

/**
 * Generazione video.
 * - LOCAL: AnimateDiff-Lightning 4-step su SD-Turbo, max 16 frame @ 8fps, 512x512.
 *          Output GIF/MP4 corto (~2s) per anteprime rapide.
 * - NODE : CogVideoX-5B o HunyuanVideo se PC con VRAM >= 16GB.
 * Modalità: TXT2VID, IMG2VID (da keyframe), VID2VID.
 */
class VideoSkill(
    private val ctx: Context,
    private val node: NodeClient,
    private val local: LocalVideoEngine
) : Skill {
    override val id = "video"
    override val label = "Genera video"
    override val requiresConsent = false

    override suspend fun run(intent: Intent, memory: MemoryStore): SkillResult {
        val prompt = intent.args["prompt"]?.toString()
            ?: return SkillResult(false, "Descrivimi il video che vuoi.")
        val seconds = (intent.args["seconds"] as? Int) ?: 4
        val fps = (intent.args["fps"] as? Int) ?: 24
        val mode = (intent.args["mode"]?.toString() ?: "txt2vid").lowercase()
        val keyframe = intent.args["keyframe"]?.toString()
        val faceId = intent.args["faceId"]?.toString()
        val hq = (intent.args["hq"] as? Boolean) ?: false

        val out = File(ctx.filesDir, "gen/vid_${System.currentTimeMillis()}.mp4").apply {
            parentFile?.mkdirs()
        }

        val useNode = node.isAvailable() && (seconds > 3 || hq || mode == "vid2vid" || faceId != null)

        if (useNode) {
            node.runVideo(
                prompt = prompt,
                seconds = seconds,
                fps = fps,
                mode = mode,
                keyframe = keyframe,
                faceId = faceId,
                hq = hq,
                outPath = out.absolutePath
            )
        } else {
            local.generate(
                prompt = prompt,
                seconds = seconds.coerceAtMost(3),
                fps = 8,
                width = 512, height = 512,
                steps = 4,
                keyframe = keyframe,
                outFile = out
            )
        }

        memory.writeArtifact(kind = "video", path = out.absolutePath, prompt = prompt)
        return SkillResult(
            ok = true,
            spokenSummary = "Video pronto, ${seconds} secondi. Lo trovi in galleria interna.",
            artifacts = listOf(out.absolutePath)
        )
    }
}
EOF
cat > app/src/main/java/com/maryen/app/skills/video/LocalVideoEngine.kt <<'EOF'
package com.maryen.app.skills.video

import java.io.File

interface LocalVideoEngine {
    suspend fun generate(
        prompt: String,
        seconds: Int,
        fps: Int,
        width: Int,
        height: Int,
        steps: Int,
        keyframe: String?,
        outFile: File
    )
}
