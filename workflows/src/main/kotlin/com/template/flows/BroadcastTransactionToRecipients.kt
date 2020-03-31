
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

/**
 * Filters out any notary identities and removes our identity, then broadcasts the [SignedTransaction] to all the
 * remaining identities.
 */
@InitiatingFlow
class BroadcastTransactionToRecipients(
        val stx: SignedTransaction,
        val recipients: List<Party>) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        for (recipient in recipients) {
            val session = initiateFlow(recipient)
            subFlow(SendTransactionFlow(session, stx))
        }
    }
}