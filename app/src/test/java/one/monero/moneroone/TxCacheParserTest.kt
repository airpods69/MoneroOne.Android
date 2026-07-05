package one.monero.moneroone

import io.horizontalsystems.monerokit.model.TransactionInfo
import one.monero.moneroone.core.wallet.parseCachedTxs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxCacheParserTest {

    @Test
    fun roundTripsWriterFormatWithFullFields() {
        // Format the writer emits: dir|amount|timestamp|hash|fee|blockheight|confirmations
        val cache = "in|1000000000000|1700000000|abc123|0|310000|15;" +
            "out|500000000000|1700000001|def456|12000|0|0"
        val parsed = parseCachedTxs(cache)

        assertEquals(2, parsed.size)

        val first = parsed[0]
        assertEquals(TransactionInfo.Direction.Direction_In, first.direction)
        assertEquals(1_000_000_000_000L, first.amount)
        assertEquals(1_700_000_000L, first.timestamp)
        assertEquals("abc123", first.hash)
        assertEquals(0L, first.fee)
        assertEquals(310_000L, first.blockheight)
        assertEquals(15L, first.confirmations)
        assertFalse(first.isPending) // blockheight > 0 → confirmed

        // Pending tx: blockheight 0 → isPending, fee preserved, confirmations 0.
        val second = parsed[1]
        assertEquals(TransactionInfo.Direction.Direction_Out, second.direction)
        assertEquals("def456", second.hash)
        assertEquals(12_000L, second.fee)
        assertEquals(0L, second.blockheight)
        assertEquals(0L, second.confirmations)
        assertTrue(second.isPending)
    }

    @Test
    fun skipsLegacyEntriesMissingTheExtraFields() {
        // Pre-upgrade 4-field entries lack fee/blockheight/confirmations → skip, don't
        // fabricate a Confirmed status. The next kit emit rewrites the cache with 7 fields.
        val cache = "in|1000000000000|1700000000|abc123;" +
            "in|500000000000|1700000001|hash2|0|310000|10"
        val parsed = parseCachedTxs(cache)

        assertEquals(1, parsed.size)
        assertEquals("hash2", parsed[0].hash)
    }

    @Test
    fun blankAndGarbageEntriesAreDropped() {
        val cache = ";in|notanumber|1700000000|hash1|0|0|0;;in|500000000000|1700000001|hash2|0|0|5;"
        val parsed = parseCachedTxs(cache)

        // "notanumber" fails toLongOrNull → dropped; blank entries filtered.
        assertEquals(1, parsed.size)
        assertEquals("hash2", parsed[0].hash)
        assertEquals(5L, parsed[0].confirmations)
    }

    @Test
    fun emptyCacheYieldsEmptyList() {
        assertTrue(parseCachedTxs("").isEmpty())
    }
}
