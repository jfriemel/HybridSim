package com.github.jfriemel.hybridsim

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

/** Launches the desktop (LWJGL3) application. */
fun main() {
    Lwjgl3Application(Main(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("HybridSim")
        useVsync(true)
        val displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
        setForegroundFPS(displayMode.refreshRate)
        setWindowedMode(displayMode.width * 8 / 10, displayMode.height * 8 / 10)
        setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png")
    })
}
