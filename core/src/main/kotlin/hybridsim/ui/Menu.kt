package hybridsim.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.actors.onClick
import ktx.scene2d.actors
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton

class Menu(batch: Batch) {

    var active: Boolean = true

    private var buttonLoadConfig: TextButton

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
                textButton("Save Configuration (K)")
                row()
                textButton("Load Algorithm (X)")
            }
        }
    }

    init {
        buttonLoadConfig.onClick {
            if (!active) {
                return@onClick
            }
        }
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

}
