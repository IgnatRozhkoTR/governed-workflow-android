package com.ignatrozhko.governedworkflow.discovery

/** An IPv4 network as a 32-bit address and prefix length. */
data class Ipv4Subnet(val networkAddress: Int, val prefixLength: Int) {

    init {
        require(prefixLength in 0..MAX_PREFIX) { "Prefix length must be within 0..32, was $prefixLength" }
    }

    /** Usable host addresses: the network and broadcast addresses are excluded for prefixes up to /30. */
    fun hostAddresses(): List<String> {
        val size = 1L shl (MAX_PREFIX - prefixLength)
        val first = networkAddress.toLong() and ADDRESS_MASK
        val hostRange = if (size <= 2) 0 until size else 1 until size - 1
        return hostRange.map { offset -> formatAddress((first + offset).toInt()) }
    }

    companion object {
        private const val MAX_PREFIX = 32
        private const val ADDRESS_MASK = 0xFFFFFFFFL

        /** Default scan width: a home LAN is rarely wider than /24 and a wider sweep would take minutes. */
        const val WIDEST_SCAN_PREFIX = 24

        /**
         * The range to sweep for [address]: its own subnet when that is /24 or narrower, otherwise the
         * /24 that contains it.
         */
        fun scanRangeFor(address: ByteArray, prefixLength: Int): Ipv4Subnet {
            require(address.size == 4) { "Not an IPv4 address" }
            val prefix = maxOf(prefixLength, WIDEST_SCAN_PREFIX).coerceAtMost(MAX_PREFIX)
            return Ipv4Subnet(toInt(address) and maskFor(prefix), prefix)
        }

        fun parseAddress(text: String): ByteArray {
            val parts = text.split('.')
            require(parts.size == 4) { "Not an IPv4 address: $text" }
            return ByteArray(4) { index ->
                val octet = parts[index].toIntOrNull()
                require(octet != null && octet in 0..255) { "Not an IPv4 address: $text" }
                octet.toByte()
            }
        }

        fun formatAddress(address: Int): String =
            listOf(24, 16, 8, 0).joinToString(".") { shift -> ((address ushr shift) and 0xFF).toString() }

        private fun toInt(address: ByteArray): Int =
            address.fold(0) { accumulated, octet -> (accumulated shl 8) or (octet.toInt() and 0xFF) }

        private fun maskFor(prefix: Int): Int = if (prefix == 0) 0 else -1 shl (MAX_PREFIX - prefix)
    }
}
