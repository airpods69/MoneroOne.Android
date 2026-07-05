package one.monero.moneroone

import io.horizontalsystems.monerokit.model.TransactionInfo
import one.monero.moneroone.core.wallet.parseCachedTxs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TxCacheParserTest {

    @Test
    fun parses4FieldEntriesAndSkipsLegacy3Field() {
        val cache = "in|1000000000000|1700000000|abc123;out|2000000000000|1700000001|def456;in|99|1700000002"
        val parsed = parseCachedTxs(cache)

        assertEquals(2, parsed.size)

        val first = parsed[0]
        assertEquals(TransactionInfo.Direction.Direction_In, first.direction)
        assertEquals(1_000_000_000_000L, first.amount)
        assertEquals(1_700_000_000L, first.timestamp)
        assertEquals("abc123", first.hash)
        // Seed contract: confirmations forced to 10 so cached txs render as Confirmed.
        assertEquals(10L, first.confirmations)

        val second = parsed[1]
        assertEquals(TransactionInfo.Direction.Direction_Out, second.direction)
        assertEquals("def456", second.hash)
    }

    @Test
    fun blankAndGarbageEntriesAreDropped() {
        val cache = ";in|notanumber|1700000000|hash1;;in|500000000000|1700000001|hash2;"
        val parsed = parseCachedTxs(cache)

        // "notanumber" fails toLongOrNull → dropped; blank entries filtered.
        assertEquals(1, parsed.size)
        assertEquals("hash2", parsed[0].hash)
    }

    @Test
    fun emptyCacheYieldsEmptyList() {
        assertTrue(parseCachedTxs("").isEmpty())
    }
}
