package no.nav.tiltakspenger.saksbehandling.lokal.journalføring

import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import java.util.UUID

/**
 * Random id'er for lokal kjøring for å hindre kollisjoner i lokal db
 * */
class JournalpostIdGeneratorRandom : JournalpostIdGenerator {
    override fun generer(): JournalpostId = JournalpostId(UUID.randomUUID().toString())
}
