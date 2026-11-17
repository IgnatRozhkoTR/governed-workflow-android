package com.ignatrozhko.governedworkflow.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ipv4SubnetTest {

    @Test
    fun scanRangeFor_shouldCover254Hosts_whenSlash24() {
        val subnet = Ipv4Subnet.scanRangeFor(Ipv4Subnet.parseAddress("192.168.1.129"), 24)

        val hosts = subnet.hostAddresses()

        assertEquals("192.168.1.0", Ipv4Subnet.formatAddress(subnet.networkAddress))
        assertEquals(254, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
    }

    @Test
    fun scanRangeFor_shouldNarrowToContainingSlash24_whenNetworkIsWider() {
        val subnet = Ipv4Subnet.scanRangeFor(Ipv4Subnet.parseAddress("10.20.30.40"), 16)

        assertEquals(24, subnet.prefixLength)
        assertEquals("10.20.30.0", Ipv4Subnet.formatAddress(subnet.networkAddress))
    }

    @Test
    fun scanRangeFor_shouldKeepNarrowerPrefix_whenSlash28() {
        val hosts = Ipv4Subnet.scanRangeFor(Ipv4Subnet.parseAddress("172.16.5.21"), 28).hostAddresses()

        assertEquals((17..30).map { "172.16.5.$it" }, hosts)
    }

    @Test
    fun hostAddresses_shouldHandleHighFirstOctet_withoutSignOverflow() {
        val hosts = Ipv4Subnet.scanRangeFor(Ipv4Subnet.parseAddress("255.255.255.10"), 30).hostAddresses()

        assertEquals(listOf("255.255.255.9", "255.255.255.10"), hosts)
    }

    @Test
    fun hostAddresses_shouldExcludeNetworkAndBroadcast() {
        val hosts = Ipv4Subnet.scanRangeFor(Ipv4Subnet.parseAddress("192.168.0.7"), 24).hostAddresses()

        assertFalse("192.168.0.0" in hosts)
        assertFalse("192.168.0.255" in hosts)
        assertTrue("192.168.0.7" in hosts)
    }

    @Test(expected = IllegalArgumentException::class)
    fun scanRangeFor_shouldReject_whenAddressIsIpv6() {
        Ipv4Subnet.scanRangeFor(ByteArray(16), 64)
    }
}
