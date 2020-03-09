package com.template

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.NotarySpec
import net.corda.testing.node.User

/**
 * Allows you to run your nodes through an IDE (as opposed to using deployNodes). Do not use in a production
 * environment.
 */
fun main(args: Array<String>) {
    // No permissions required as we are not invoking flows.
    val user = User("user1", "test", permissions = setOf("ALL"))
    driver(DriverParameters( isDebug = true,
            extraCordappPackagesToScan = listOf("net.corda.finance"),
            notarySpecs = listOf(NotarySpec(CordaX500Name("Notary", "London","GB"), true)),
            waitForAllNodesToFinish = true)) {
        val (nodeA, nodeB, nodeC) = listOf(
                startNode(NodeParameters(providedName = CordaX500Name("ParticipantA", "London", "GB")), rpcUsers = listOf(user)).getOrThrow(),
                startNode(NodeParameters(providedName = CordaX500Name("ParticipantB", "New York", "US")), rpcUsers = listOf(user)).getOrThrow(),
                startNode(NodeParameters(providedName = CordaX500Name("ParticipantC", "Paris", "FR")), rpcUsers = listOf(user)).getOrThrow())

        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)
    }
}
