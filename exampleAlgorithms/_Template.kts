/**
 * Use this template to write your own algorithm scripts for robots in the hybrid model for
 * programmable matter. You can write any valid Kotlin code (imports, custom classes, whatever
 * floats your boat).
 *
 * The only requirement is that you keep the function fun getRobot(orientation: Int, node: Node):
 * Robot as it is used to instantiate the robots in the simulator when your script is loaded.
 *
 * Feel free to check out the example algorithms in this directory for guidance.
 */

/*
Add any imports you may need.

Default imports (no need to add them to the script):

import com.github.jfriemel.hybridsim.entities.*
import com.github.jfriemel.hybridsim.system.Commons
import com.badlogic.gdx.graphics.Color

You can still add these imports manually to make use of your IDEs autocomplete features.
Make sure that your other imports do not clash with the default imports.
For example, do not import java.awt.Color as it clashes with com.badlogic.gdx.graphics.Color.

For randomness, use Commons.random instead of kotlin.random.Random (or other randomness
implementations) as its seed can be specified using a command-line argument (--seed).
 */

/**
 * This function must remain in your script (although you may change the implementation to fit your
 * needs).
 */
fun getRobot(node: Node, orientation: Int): Robot {
    return RobotImpl(node, orientation)
}

// Put helper classes here, e.g.,
// private enum class Phase { PhaseOne, PhaseTwo, PhaseThree }

class RobotImpl(node: Node, orientation: Int) :
    Robot(
        node = node,
        orientation = orientation, // Replace with constant (0-5) if robots should share a compass
        carriesTile = false, // Change constants to fit your needs
        numPebbles = 2, // Change constants to fit your needs
        maxPebbles = 2, // Change constants to fit your needs
    ) {
    // Put your robot's variables here, e.g.,
    // private var phase = Phase.PhaseOne

    /**
     * This function is executed by the robot when it is activated. As per the model, the algorithm
     * needs to be convertible to an equivalent finite state automaton. In other words, it needs to
     * run in constant time and with constant space. For further constraints, check out this paper:
     * https://doi.org/10.1007/s11047-019-09774-2
     */
    override fun activate() {
        TODO("Replace with your algorithm implementation")
        /* Example:
        when (phase) {
            Phase.PhaseOne -> phaseOne()
            Phase.PhaseTwo -> phaseTwo()
            Phase.PhaseThree -> phaseThree()
        }
         */
    }

    /**
     * Indicate whether the robot is finished with executing its algorithm. When all robots are
     * finished (i.e., every finished() call returns true), the Scheduler stops automatically.
     * Remove this code if your robot cannot tell whether it is finished (e.g., in exploration
     * algorithm).
     */
    override fun finished(): Boolean {
        return super.finished()
    }

    /**
     * Your particle's color (e.g., based on the algorithm's execution phase). Reuturns Color.WHITE
     * by default. Remove this code if you do not need any other colors. Constructors and default
     * colors:
     * https://github.com/libgdx/libgdx/blob/83e298085817e690492fe6f2bffa72fafe765d21/gdx/src/com/badlogic/gdx/graphics/Color.java
     *
     * Palette used by example algorithms that should play well with common types of colorblindness
     * (adapted from https://jfly.uni-koeln.de/color/): Color.{ORANGE, TEAL, SKY, SCARLET, BLUE,
     * YELLOW, BROWN, BLACK}
     *
     * What this palette looks like to people with common types of colorblindness:
     * https://davidmathlogic.com/colorblind/#%23FFA500-%23008888-%2387CEEB-%23FF341C-%230000FF-%23FFFF00-%238B4513-%23000000
     */
    override fun getColor(): Color {
        return super.getColor()
    }

    // Put your helper functions here, e.g.,
    // private fun phaseOne() { ... }
    // private fun phaseTwo() { ... }
    // private fun phaseThree() { ... }
}
