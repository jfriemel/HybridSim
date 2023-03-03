package hybridsim

import hybridsim.entities.Node
import hybridsim.entities.Robot
import hybridsim.entities.Tile
import kotlin.random.Random

object Configuration {

    var tiles: HashMap<Node, Tile> = HashMap()
    var robots: HashMap<Node, Robot> = HashMap()

    init {
        robots[Node.origin] = Robot(Random.nextInt(0, 6), Node.origin)
        for (i in -1..1) {
            for (j in -1..1) {
                val node = Node(i, j)
                tiles[node] = Tile(node)
                if (Math.random() > 0.75) {
                    tiles[node]?.addPebble()
                }
            }
        }
    }

}
