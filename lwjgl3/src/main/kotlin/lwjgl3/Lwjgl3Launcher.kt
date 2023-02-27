package lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import hybridsim.Main

/** Launches the desktop (LWJGL3) application.  */
fun main() {
    Lwjgl3Application(Main(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("HybridSim")
        useVsync(true)
        setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate)
        setWindowedMode(1024, 768)
        setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png")
    })
}
