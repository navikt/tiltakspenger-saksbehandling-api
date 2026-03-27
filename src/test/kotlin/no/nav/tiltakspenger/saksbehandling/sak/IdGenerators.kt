package no.nav.tiltakspenger.saksbehandling.sak

import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonIdGenerator
import no.nav.tiltakspenger.saksbehandling.journalføring.DokumentInfoIdGeneratorSerial
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGeneratorSerial

data class IdGenerators(
    val saksnummerGenerator: SaksnummerGeneratorForTest = SaksnummerGeneratorForTest(),
    val fnrGenerator: FnrGenerator = FnrGenerator(),
    val distribusjonIdGenerator: DistribusjonIdGenerator = DistribusjonIdGenerator(),
    val journalpostIdGenerator: JournalpostIdGeneratorSerial = JournalpostIdGeneratorSerial(),
    val dokumentInfoIdGeneratorSerial: DokumentInfoIdGeneratorSerial = DokumentInfoIdGeneratorSerial(),
    val søknadstiltakIdGenerator: SøknadstiltakIdGenerator = SøknadstiltakIdGenerator(),
)
