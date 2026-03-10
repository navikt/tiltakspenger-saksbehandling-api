package no.nav.tiltakspenger.saksbehandling.journalføring

import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

interface DokumentInfoIdGeneratorGenerator {
    fun neste(): DokumentInfoId
}

/**
 * Trådsikker. Dersom tester deler database, bør de bruke en felles statisk versjon av denne.
 */
class DokumentInfoIdGeneratorSerial(
    første: Long = 1,
) : DokumentInfoIdGeneratorGenerator {
    private val neste = AtomicLong(første)

    override fun neste(): DokumentInfoId = DokumentInfoId(neste.getAndIncrement().toString())
}

/**
 * Random id'er for lokal kjøring for å hindre kollisjoner i lokal db
 * */
class DokumentInfoIdGeneratorRandom : DokumentInfoIdGeneratorGenerator {
    override fun neste(): DokumentInfoId = DokumentInfoId(UUID.randomUUID().toString())
}
