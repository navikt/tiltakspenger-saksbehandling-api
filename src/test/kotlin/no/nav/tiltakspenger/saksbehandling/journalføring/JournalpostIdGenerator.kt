package no.nav.tiltakspenger.saksbehandling.journalføring

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

interface JournalpostIdGenerator {
    fun generer(): JournalpostId
}

/**
 * Trådsikker. Dersom tester deler database, bør de bruke en felles statisk versjon av denne.
 */
class JournalpostIdGeneratorSerial(
    første: Long = 1,
) : JournalpostIdGenerator {
    private val neste = AtomicLong(første)

    override fun generer(): JournalpostId = JournalpostId(neste.getAndIncrement().toString())
}

/**
 * Random id'er for lokal kjøring for å hindre kollisjoner i lokal db
 * */
class JournalpostIdGeneratorRandom : JournalpostIdGenerator {
    override fun generer(): JournalpostId = JournalpostId(UUID.randomUUID().toString())
}
