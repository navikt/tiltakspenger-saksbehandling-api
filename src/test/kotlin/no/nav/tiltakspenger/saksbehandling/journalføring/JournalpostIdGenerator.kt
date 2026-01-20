package no.nav.tiltakspenger.saksbehandling.journalføring

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

interface JournalpostIdGenerator {
    fun neste(): JournalpostId
}

/**
 * Trådsikker. Dersom tester deler database, bør de bruke en felles statisk versjon av denne.
 */
class JournalpostIdGeneratorSerial(
    første: Long = 1,
) : JournalpostIdGenerator {
    private val neste = AtomicLong(første)

    override fun neste(): JournalpostId = JournalpostId(neste.getAndIncrement().toString())
}

/**
 * Random id'er for lokal kjøring for å hindre kollisjoner i lokal db
 * */
class JournalpostIdGeneratorRandom : JournalpostIdGenerator {
    override fun neste(): JournalpostId = JournalpostId(UUID.randomUUID().toString())
}
