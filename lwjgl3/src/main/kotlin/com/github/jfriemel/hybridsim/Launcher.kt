package com.github.jfriemel.hybridsim

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file

fun main(args: Array<String>) = Main().main(args)

/** Launches either the desktop (LWJGL3) or the CLI application. */
class Main : CliktCommand() {

    private val gui by option(help = "Launch GUI, enabled by default")
        .boolean()
        .default(true)
    private val algorithm by option()
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val configuration by option()
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val generator by option()
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    override fun run() {
        if (gui) {
            Lwjgl3Application(MainGUI(algorithm, configuration, generator), Lwjgl3ApplicationConfiguration().apply {
                setTitle("HybridSim")
                useVsync(true)
                val displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
                setForegroundFPS(displayMode.refreshRate)
                setWindowedMode(displayMode.width * 8 / 10, displayMode.height * 8 / 10)
                setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png")
            })
        } else {
            MainCLI().main()
        }
    }

}
