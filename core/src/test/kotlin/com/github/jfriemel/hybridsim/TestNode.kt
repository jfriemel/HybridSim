package com.github.jfriemel.hybridsim

import com.github.jfriemel.hybridsim.entities.Node
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.random.Random

/** Test [Node] functionality. */
class TestNode {

    @ParameterizedTest(name = "direction {0}")
    @CsvSource(
        "0, 2, 6",
        "1, 3, 7",
        "2, 3, 8",
        "3, 2, 8",
        "4, 1, 8",
        "5, 1, 7",
    )
    fun `neighbors on even column`(dir: Int, nbrX: Int, nbrY: Int) {
        val node = Node(2, 7)
        Assertions.assertEquals(Node(nbrX, nbrY), node.nodeInDir(dir))
    }

    @ParameterizedTest(name = "direction {0}")
    @CsvSource(
        "0, -3, -5",
        "1, -2, -5",
        "2, -2, -4",
        "3, -3, -3",
        "4, -4, -4",
        "5, -4, -5",
    )
    fun `neighbors on odd column`(dir: Int, nbrX: Int, nbrY: Int) {
        val node = Node(-3, -4)
        Assertions.assertEquals(Node(nbrX, nbrY), node.nodeInDir(dir))
    }

    @Test
    fun `scientific coordinates on even column`() {
        val node = Node(-8, 2)
        val scCoords = node.scientificCoordinates()
        Assertions.assertEquals(-8.0, scCoords.first)
        Assertions.assertEquals(2.0, scCoords.second)
    }

    @Test
    fun `scientific coordinates on odd column`() {
        val node = Node(5, -6)
        val scCoords = node.scientificCoordinates()
        Assertions.assertEquals(5.0, scCoords.first)
        Assertions.assertEquals(-6.5, scCoords.second)
    }

    @ParameterizedTest(name = "scientific ({0}, {1}), internal ({2}, {3})")
    @CsvSource(
        "0.0, 3.0, 0, 3",
        "5.0, 2.5, 5, 3",
    )
    fun `node from scientific coordinates`(scX: Double, scY: Double, nodeX: Int, nodeY: Int) {
        val scNode = Node.sciCoordsToNode(scX, scY)
        Assertions.assertEquals(Node(nodeX, nodeY), scNode)
    }

    @Test
    fun `random node coordinate conversion`() {
        repeat(10_000) {
            val node = Node(Random.nextInt(), Random.nextInt())
            val scCoords = node.scientificCoordinates()
            Assertions.assertEquals(node, Node.sciCoordsToNode(scCoords.first, scCoords.second))
        }
    }

}
