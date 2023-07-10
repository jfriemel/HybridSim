package com.github.jfriemel.hybridsim.system

import com.github.jfriemel.hybridsim.entities.Node
import kotlin.math.min

open class Generator {

    open fun generate(numTiles: Int, numRobots: Int, numOverhang: Int = -1): ConfigurationDescriptor {
        val descriptor = ConfigurationDescriptor(mutableSetOf(), mutableSetOf(), mutableSetOf())

        if (numTiles <= 0) {
            return descriptor
        }

        // Tiles
        descriptor.tileNodes.add(Node.origin)
        val tileCandidates = Node.origin.neighbors().toMutableSet()
        repeat(numTiles - 1) {
            val nextNode = tileCandidates.random()
            descriptor.tileNodes.add(nextNode)
            tileCandidates.addAll(
                nextNode.neighbors().minus(descriptor.tileNodes)
            )
            tileCandidates.remove(nextNode)
        }

        // Robots
        val robotCandidates = descriptor.tileNodes.toMutableSet()
        repeat(min(numRobots, numTiles)) {
            val nextNode = robotCandidates.random()
            descriptor.robotNodes.add(nextNode)
            robotCandidates.remove(nextNode)
        }

        if (numOverhang < 0) {
            return descriptor
        } else if (numOverhang == 0) {
            descriptor.targetNodes.addAll(descriptor.tileNodes)
            return descriptor
        }

        // Target tiles
        val targetTileOrigin = descriptor.tileNodes.filter { node ->  // Pick node at boundary of tile configuration
            isAtOuterBoundary(node, numTiles, descriptor)
        }.random()
        descriptor.targetNodes.add(targetTileOrigin)
        val targetTileCandidates = targetTileOrigin.neighbors().intersect(descriptor.tileNodes).toMutableSet()
        repeat(numTiles - min(numOverhang, numTiles - 1) - 1) {
            val nextNode = targetTileCandidates.random()
            descriptor.targetNodes.add(nextNode)
            targetTileCandidates.addAll(
                nextNode.neighbors()
                    .intersect(descriptor.tileNodes)
                    .minus(descriptor.targetNodes)
            )
            targetTileCandidates.remove(nextNode)
        }

        // Demand nodes
        val demandCandidates = descriptor.targetNodes
            .flatMap { node -> node.neighbors() }
            .minus(descriptor.tileNodes)
            .toMutableSet()
        repeat(min(numOverhang, numTiles - 1)) {
            val nextNode = demandCandidates.random()
            descriptor.targetNodes.add(nextNode)
            demandCandidates.addAll(
                nextNode.neighbors()
                    .minus(descriptor.tileNodes)
                    .minus(descriptor.targetNodes)
            )
            demandCandidates.remove(nextNode)
        }

        return descriptor
    }

    private fun isAtOuterBoundary(node: Node, numTiles: Int, descriptor: ConfigurationDescriptor): Boolean {
        val boundary = node.neighbors().filter { nbr -> nbr !in descriptor.tileNodes }.toMutableSet()
        if (boundary.isEmpty()) {
            return false
        }

        val toAdd = emptySet<Node>().toMutableSet()
        while (boundary.size <= numTiles) {
            toAdd.clear()
            for (bnode in boundary) {
                toAdd.addAll(bnode.neighbors().filter { nbr -> nbr !in descriptor.tileNodes })
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
