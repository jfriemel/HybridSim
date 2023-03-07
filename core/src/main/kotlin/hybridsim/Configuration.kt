package hybridsim

import com.beust.klaxon.Klaxon
import hybridsim.entities.Node
import hybridsim.entities.Robot
import hybridsim.entities.Tile

object Configuration {

    var tiles: HashMap<Node, Tile> = HashMap()
    var robots: HashMap<Node, Robot> = HashMap()
    var targetNodes: MutableSet<Node> = HashSet()

    init {
        loadConfiguration("{\"robots\" : {\"Node(x=0, y=0)\": {\"orientation\" : 4, \"node\" : {\"x\" : 0, \"y\" : 0}}}, \"targetNodes\" : [{\"x\" : 1, \"y\" : 1}], \"tiles\" : {\"Node(x=0, y=-1)\": {\"node\" : {\"x\" : 0, \"y\" : -1}}, \"Node(x=0, y=0)\": {\"node\" : {\"x\" : 0, \"y\" : 0}}, \"Node(x=1, y=1)\": {\"node\" : {\"x\" : 1, \"y\" : 1}}, \"Node(x=0, y=1)\": {\"node\" : {\"x\" : 0, \"y\" : 1}}, \"Node(x=-1, y=1)\": {\"node\" : {\"x\" : -1, \"y\" : 1}}, \"Node(x=-1, y=0)\": {\"node\" : {\"x\" : -1, \"y\" : 0}}, \"Node(x=1, y=-1)\": {\"node\" : {\"x\" : 1, \"y\" : -1}}, \"Node(x=-1, y=-1)\": {\"node\" : {\"x\" : -1, \"y\" : -1}}}}\n")
        loadConfiguration(getJson())
    }

    /**
     * Takes a JSON string and loads the configuration from that string.
     *
     * @param json JSON string representing a configuration.
     */
    fun loadConfiguration(json: String) {
        Klaxon().parse<Configuration>(json = json)

//        Klaxon converts the keys of Maps to Strings. To get our Node keys back, we create temporary Maps, fill them
//        with the values parsed by Klaxon and then replace the Klaxon Maps with our temporary Maps.
//        There is probably a much cleaner way to do this (with Klaxon Converters), but I don't really care right now.
        val tmpTiles = HashMap<Node, Tile>()
        for (tile in tiles.values) {
            tmpTiles[tile.node] = tile
        }
        tiles = tmpTiles
        val tmpRobots = HashMap<Node, Robot>()
        for (robot in robots.values) {
            tmpRobots[robot.node] = robot
        }
        robots = tmpRobots
    }

    /**
     * Converts the current configuration string to a JSON string that can be saved to a file.
     *
     * @return JSON string representing the current configuration.
     */
    fun getJson(): String {
        return Klaxon().toJsonString(Configuration)
    }

}
