import com.template.contracts.PaymentContract
import com.template.states.ResponsibilityState
import net.corda.core.identity.CordaX500Name
import net.corda.finance.POUNDS
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class PaymentContractTests {
    private val ledgerServices = MockServices(listOf("com.example.contract", "com.example.flow"))
    private val poolCorp = TestIdentity(CordaX500Name("poolCorp", "London", "GB"))
    private val miniA = TestIdentity(CordaX500Name("MiniA", "London", "GB"))
    private val totalAmount = 10

    @Test
    fun `transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(PaymentContract.PAYMENT_CONTRACT_ID, ResponsibilityState(miniA.party, poolCorp.party, 50, totalAmount.POUNDS))
                fails()
                command(listOf(poolCorp.publicKey, miniA.publicKey), PaymentContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(PaymentContract.PAYMENT_CONTRACT_ID, ResponsibilityState(miniA.party, poolCorp.party, 50, totalAmount.POUNDS))
                output(PaymentContract.PAYMENT_CONTRACT_ID, ResponsibilityState(miniA.party, poolCorp.party, 50, totalAmount.POUNDS))
                command(listOf(poolCorp.publicKey, miniA.publicKey), PaymentContract.Commands.Create())
                `fails with`("No inputs should be consumed when requesting an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                output(PaymentContract.PAYMENT_CONTRACT_ID, ResponsibilityState(miniA.party, poolCorp.party, 50, totalAmount.POUNDS))
                command(listOf(poolCorp.publicKey, miniA.publicKey), PaymentContract.Commands.Create())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `pool must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(PaymentContract.PAYMENT_CONTRACT_ID, ResponsibilityState(miniA.party, poolCorp.party, 50, totalAmount.POUNDS))
                command(poolCorp.publicKey, PaymentContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `member must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(PaymentContract.PAYMENT_CONTRACT_ID, ResponsibilityState(miniA.party, poolCorp.party, 50, totalAmount.POUNDS))
                command(miniA.publicKey, PaymentContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `pool is not member`() {
        ledgerServices.ledger {
            transaction {
                output(PaymentContract.PAYMENT_CONTRACT_ID, ResponsibilityState(miniA.party, poolCorp.party, 50, totalAmount.POUNDS))
                command(listOf(poolCorp.publicKey, miniA.publicKey), PaymentContract.Commands.Create())
                `fails with`("The pool and the member cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        ledgerServices.ledger {
            transaction {
                output(PaymentContract.PAYMENT_CONTRACT_ID, ResponsibilityState(miniA.party, poolCorp.party, 50, totalAmount.POUNDS))
                command(listOf(poolCorp.publicKey, miniA.publicKey), PaymentContract.Commands.Create())
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }
}