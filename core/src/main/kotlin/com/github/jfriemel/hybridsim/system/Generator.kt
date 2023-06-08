package com.github.jfriemel.hybridsim.system

import com.github.jfriemel.hybridsim.entities.Node
import com.github.jfriemel.hybridsim.entities.Robot
import com.github.jfriemel.hybridsim.entities.Tile
import kotlin.math.min

open class Generator {

    open fun generate(numTiles: Int, numRobots: Int, numOverhang: Int = -1) {
        Configuration.clear()

        if (numTiles <= 0) {
            return
        }

        // Tiles
        Configuration.addTile(Tile(Node.origin))
        val tileCandidates = Node.origin.neighbors().toMutableSet()
        repeat(numTiles - 1) {
            val nextNode = tileCandidates.random()
            Configuration.addTile(Tile(nextNode))
            tileCandidates.addAll(
                nextNode.neighbors().minus(Configuration.tiles.keys)
            )
            tileCandidates.remove(nextNode)
        }

        // Robots
        val robotCandidates = Configuration.tiles.keys.toMutableSet()
        repeat(min(numRobots, numTiles)) {
            val nextNode = robotCandidates.random()
            Configuration.addRobot(Robot(nextNode))
            robotCandidates.remove(nextNode)
        }

        if (numOverhang < 0) {
            return
        } else if (numOverhang == 0) {
            Configuration.tiles.keys.forEach { node -> Configuration.addTarget(node) }
            return
        }

        // Target tiles
        val targetTileOrigin = Configuration.tiles.keys.filter { node ->  // Pick node at boundary of tile configuration
            isAtOuterBoundary(node, numTiles)
        }.random()
        Configuration.addTarget(targetTileOrigin)
        val targetTileCandidates = targetTileOrigin.neighbors().intersect(Configuration.tiles.keys).toMutableSet()
        repeat(numTiles - min(numOverhang, numTiles - 1) - 1) {
            val nextNode = targetTileCandidates.random()
            Configuration.addTarget(nextNode)
            targetTileCandidates.addAll(
                nextNode.neighbors()
                    .intersect(Configuration.tiles.keys)
                    .minus(Configuration.targetNodes)
            )
            targetTileCandidates.remove(nextNode)
        }

        // Demand nodes
        val demandCandidates = Configuration.targetNodes
            .flatMap { node -> node.neighbors() }
            .minus(Configuration.tiles.keys)
            .toMutableSet()
        repeat(min(numOverhang, numTiles - 1)) {
            val nextNode = demandCandidates.random()
            Configuration.addTarget(nextNode)
            demandCandidates.addAll(
                nextNode.neighbors()
                    .minus(Configuration.tiles.keys)
                    .minus(Configuration.targetNodes)
            )
            demandCandidates.remove(nextNode)
        }
    }

    private fun isAtOuterBoundary(node: Node, numTiles: Int): Boolean {
        val boundary = node.neighbors().filter { nbr -> nbr !in Configuration.tiles }.toMutableSet()
        if (boundary.isEmpty()) {
            return false
        }

        val toAdd = emptySet<Node>().toMutableSet()
        while (boundary.size <= numTiles) {
            toAdd.clear()
            for (bnode in boundary) {
                toAdd.addAll(bnode.neighbors().filter { nbr -> nbr !in Configuration.tiles })
            }
            val prevSize = boundary.size
            boundary.addAll(toAdd)
            if (boundary.size == prevSize) {
                break
            }
        }
        return boundary.size > numTiles
    }

}
