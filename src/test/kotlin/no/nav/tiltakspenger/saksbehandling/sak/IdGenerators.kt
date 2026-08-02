package no.nav.tiltakspenger.saksbehandling.sak

import no.nav.tiltakspenger.libs.common.SaksnummerGeneratorForTest
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonIdGenerator
import no.nav.tiltakspenger.saksbehandling.journalføring.DokumentInfoIdGeneratorSerial
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGeneratorSerial

/**
 * Delt instans slik at alle saksnummer er unike på tvers av hele testkjøringen; saksnummer har unik indeks i sak-tabellen.
 * Skal ikke ha flere instanser; to generatorer med samme startverdi gir samme sekvens og dermed kollisjoner.
 */
val delteSaksnummerGenerator = SaksnummerGeneratorForTest()

data class IdGenerators(
    val saksnummerGenerator: SaksnummerGeneratorForTest = delteSaksnummerGenerator,
    val distribusjonIdGenerator: DistribusjonIdGenerator = DistribusjonIdGenerator(),
    val journalpostIdGenerator: JournalpostIdGeneratorSerial = JournalpostIdGeneratorSerial(),
    val dokumentInfoIdGeneratorSerial: DokumentInfoIdGeneratorSerial = DokumentInfoIdGeneratorSerial(),
    val søknadstiltakIdGenerator: SøknadstiltakIdGenerator = SøknadstiltakIdGenerator(),
)
