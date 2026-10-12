package no.nav.tiltakspenger.saksbehandling.lokal.journalføring

import no.nav.tiltakspenger.saksbehandling.journalføring.DokumentInfoIdGeneratorGenerator
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import java.util.UUID

/**
 * Random id'er for lokal kjøring for å hindre kollisjoner i lokal db
 * */
class DokumentInfoIdGeneratorRandom : DokumentInfoIdGeneratorGenerator {
    override fun generer(): DokumentInfoId = DokumentInfoId(UUID.randomUUID().toString())
}
