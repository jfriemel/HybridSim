import kotlin.math.min
import kotlin.random.Random

fun getGenerator(): Generator = GeneratorImpl()

class GeneratorImpl(): Generator() {

    override fun generate(numTiles: Int, numRobots: Int, numOverhang: Int): ConfigurationDescriptor {
        val descriptor = ConfigurationDescriptor(mutableSetOf(), mutableSetOf(), mutableSetOf())

        if (numTiles <= 0) {
            return descriptor
        }

        // Tiles
        // Create 3n/2 tiles, maintain simple connectivity
        descriptor.tileNodes.add(Node.origin)
        val tileCandidates = Node.origin.neighbors().toMutableSet()
        repeat(numTiles + numTiles / 2 - 1) {
            var nextNode: Node
            do {
                nextNode = tileCandidates.random()
            } while (!isValidCandidate(nextNode, descriptor.tileNodes))
            descriptor.tileNodes.add(nextNode)
            tileCandidates.addAll(
                nextNode.neighbors().minus(descriptor.tileNodes)
            )
            tileCandidates.remove(nextNode)
        }
        // Remove n tiles, end up with n/2 tiles, maintain simple connectivity
        val tileRemoveCandidates = descriptor.tileNodes.toMutableSet()
        repeat(numTiles) {
            var nextNode: Node
            do {
                nextNode = tileRemoveCandidates.random()
            } while (!isValidCandidate(nextNode, descriptor.tileNodes))
            descriptor.tileNodes.remove(nextNode)
            tileRemoveCandidates.addAll(
                nextNode.neighbors().intersect(descriptor.tileNodes)
            )
            tileRemoveCandidates.remove(nextNode)
        }
        // Add n/2 tiles, end up with n tiles, allow tiles to close holes
        val closingTileCandidates = descriptor.tileNodes
            .flatMap { node -> node.neighbors() }
            .minus(descriptor.tileNodes)
            .toMutableSet()
        repeat(numTiles / 2) {
            val nextNode = closingTileCandidates.random()
            descriptor.tileNodes.add(nextNode)
            closingTileCandidates.addAll(
                nextNode.neighbors().minus(descriptor.tileNodes)
            )
            closingTileCandidates.remove(nextNode)
        }

        // Robots
        val robotCandidates = descriptor.tileNodes.toMutableSet()
        repeat(min(numRobots, numTiles)) {
            val nextNode = robotCandidates.random()
            descriptor.robotNodes.add(nextNode)
            robotCandidates.remove(nextNode)
        }

        // Skip if no overhang
        if (numOverhang < 0) {
            return descriptor
        } else if (numOverhang == 0) {
            descriptor.targetNodes.addAll(descriptor.tileNodes)
            return descriptor
        }

        // Target tiles
        val dir = Random.nextInt(0, 6)
        var targetTileOrigin = descriptor.tileNodes.random()
        var currentNode = targetTileOrigin
        repeat(numTiles) {
            currentNode = currentNode.nodeInDir(dir)
            if (currentNode in descriptor.tileNodes) {
                targetTileOrigin = currentNode
            }
        }
        descriptor.targetNodes.add(targetTileOrigin)
        val targetTileCandidates = targetTileOrigin.neighbors().intersect(descriptor.tileNodes).toMutableSet()
        repeat(numTiles - min(numOverhang, numTiles - 1) - 1) {
            var nextNode: Node
            do {
                nextNode = targetTileCandidates.random()
            } while (!isValidCandidate(nextNode, descriptor.targetNodes))
            descriptor.targetNodes.add(nextNode)
            targetTileCandidates.addAll(
                nextNode.neighbors()
                    .minus(descriptor.targetNodes)
                    .intersect(descriptor.tileNodes)
            )
            targetTileCandidates.remove(nextNode)
        }

        // Demand nodes
        val demandNodeCandidates = descriptor.targetNodes
            .flatMap { node -> node.neighbors() }
            .minus(descriptor.tileNodes)
            .toMutableSet()
        repeat(min(numOverhang, numTiles - 1)) {
            var nextNode: Node
            do {
                nextNode = demandNodeCandidates.random()
            } while (!isValidCandidate(nextNode, descriptor.targetNodes))
            descriptor.targetNodes.add(nextNode)
            demandNodeCandidates.addAll(
                nextNode.neighbors()
                    .minus(descriptor.targetNodes)
                    .minus(descriptor.tileNodes)
            )
            demandNodeCandidates.remove(nextNode)
        }

        return descriptor
    }

    private fun isValidCandidate(candidate: Node, nodeSet: Set<Node>): Boolean {
        val otherDirs = (0..5).filter { dir -> candidate.nodeInDir(dir) !in nodeSet }
        return otherDirs.size == 6 || otherDirs.filter { label -> (label + 1).mod(6) !in otherDirs }.size == 1
    }

}
