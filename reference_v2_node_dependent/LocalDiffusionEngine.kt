package com.maryen.app.skills.image

import java.io.File

interface LocalDiffusionEngine {
    suspend fun generate(
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        steps: Int,
        cfg: Double,
        outFile: File
    )
}
