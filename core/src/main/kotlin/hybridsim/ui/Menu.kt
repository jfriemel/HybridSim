package hybridsim.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.ScreenViewport
import hybridsim.Configuration
import hybridsim.Main
import ktx.actors.onClick
import ktx.scene2d.actors
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton
import java.io.File
import java.lang.Exception
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

private val logger = ktx.log.logger<Main>()

class Menu(batch: Batch) {

    var active: Boolean = true

    private var buttonLoadConfig: TextButton
    private var buttonSaveConfig: TextButton
    private var buttonLoadAlgorithm: TextButton

    val menuStage = Stage(ScreenViewport(OrthographicCamera()), batch).apply {
        actors {
            table {
                setFillParent(true)
                defaults().pad(2f)
                setPosition(Gdx.graphics.width / 2f - 100f, 0f)
                label("Menu (M)").color = Color.BLACK
                row()
                buttonLoadConfig = textButton("Load Configuration (L)")
                row()
                buttonSaveConfig = textButton("Save Configuration (K)")
                row()
                buttonLoadAlgorithm = textButton("Load Algorithm (X)")
            }
        }
    }

    private val jsonFilter = FileNameExtensionFilter("HybridSim configuration JSON files", "json")
    private val algoFilter = FileNameExtensionFilter("HybridSim algorithm scripts", "kts")

    init {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            logger.error { "Could not set UI look to system look" }
            logger.error { "Exception: $e" }
        }

        buttonLoadConfig.onClick { loadConfiguration() }
        buttonSaveConfig.onClick { saveConfiguration() }
    }

    fun draw() {
        if (active) {
            menuStage.act()
            menuStage.draw()
        }
    }

    fun resize(width: Int, height: Int) {
        menuStage.viewport.update(width, height, true)
        menuStage.actors.get(0).setPosition(width / 2f - 100f, 0f)
    }

    fun loadConfiguration() {
        if (!active) {
            return
        }
        val configFile = getFile(jsonFilter) ?: return
        if (!configFile.exists()) {
            logger.error { "Cannot load configuration, selected file $configFile does not exist" }
            return
        }
        Configuration.loadConfiguration(configFile.readText())
    }

    fun saveConfiguration() {
        if (!active) {
            return
        }
        var configFile = getFile(jsonFilter) ?: return
        if (configFile.extension != "json") {
            configFile = File(configFile.absolutePath.plus(".json"))
        }
        configFile.writeText(Configuration.getJson())
    }

    private fun getFile(filter: FileNameExtensionFilter): File? {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = filter
        val choice = fileChooser.showOpenDialog(null)
        if (choice == JFileChooser.APPROVE_OPTION) {
            return fileChooser.selectedFile
        }
        return null
    }

}
