package no.nav.tiltakspenger.common

import no.nav.tiltakspenger.vedtak.felles.journalføring.JournalpostId
import java.util.concurrent.atomic.AtomicLong

/**
 * Trådsikker. Dersom tester deler database, bør de bruke en felles statisk versjon av denne.
 */
class JournalpostIdGenerator(
    første: Long = 1,
) {
    private val neste = AtomicLong(første)

    fun neste(): JournalpostId = JournalpostId(neste.getAndIncrement().toString())
}
