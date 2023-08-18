/**
 * Use this template to write your own generator scripts for configurations in the hybrid model for programmable matter.
 * You can write any valid Kotlin code (imports, custom classes, whatever floats your boat).
 *
 * The only requirement is that you keep the function
 *     getGenerator(): Generator
 * as it is used to instantiate the generator in the simulator when your script is loaded.
 *
 * Feel free to check out the example generator scripts in this directory for guidance.
 */

/*
Add any imports you may need (e.g., import kotlin.random.Random)

Default imports (no need to add them to the script):

import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.system.ConfigurationDescriptor
import com.github.jfriemel.hybridsim.system.Generator

Make sure that your imports do not clash with the default imports.
 */

/** This function must remain in your script (although you may change the implementation to fit your needs). */
fun getGenerator(): Generator {
    return GeneratorImpl()
}

class GeneratorImpl(): Generator() {

    /**
     * This function is called by the Configuration object, when a new configuration needs to be generated.
     * Please modify the contents of the function, but do not remove it or change its signature.
     */
    override fun generate(numTiles: Int, numRobots: Int, numOverhang: Int): ConfigurationDescriptor {
        // The set of nodes occupied by tiles in your generated configuration
        val tileNodes: MutableSet<Node> = mutableSetOf()
        // The set of nodes occupied by robots in your generated configuration
        val robotNodes: MutableSet<Node> = mutableSetOf()
        // The set of target nodes in your generated configuration
        // Can be left empty if you do not care about generating target nodes
        val targetNodes: MutableSet<Node> = mutableSetOf()

        TODO("Replace with your generator implementation where you fill the sets above with nodes as you see fit")
        /* Example:
        repeat(numTiles) {
            tileNodes.add(Node(Random.nextInt(), Random.nextInt()))  // Requires 'import kotlin.random.Random'
        }
         */

        return ConfigurationDescriptor(tileNodes, robotNodes, targetNodes)
    }

}
