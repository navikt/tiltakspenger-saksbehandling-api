package no.nav.tiltakspenger.saksbehandling.klage.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.klageinstanshendelseFeilregistrert
import org.junit.jupiter.api.Test

/**
 * Vi har IT tester for klage.
 * Denne er for å dekke de tilfellene som ikke dekkes av IT testene, som vi har lyst på mer kontroll på
 */
class KlagebehandlingsresultatTest {
    @Test
    fun `visse avsluttet utfall skal knyttes til rammebehandling`() {
        KlagehendelseKlagebehandlingAvsluttetUtfall.entries.forEach { entry ->
            val resultat = ObjectMother.klagebehandlingresultatOpprettholdt(
                klageinstanshendelser = Klageinstanshendelser(listOf(ObjectMother.klageinstanshendelseAvsluttet(utfall = entry))),
            )

            when (entry) {
                KlagehendelseKlagebehandlingAvsluttetUtfall.TRUKKET -> resultat.skalVæreKnyttetTilRammebehandling shouldBe false
                KlagehendelseKlagebehandlingAvsluttetUtfall.RETUR -> resultat.skalVæreKnyttetTilRammebehandling shouldBe false
                KlagehendelseKlagebehandlingAvsluttetUtfall.OPPHEVET -> resultat.skalVæreKnyttetTilRammebehandling shouldBe true
                KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD -> resultat.skalVæreKnyttetTilRammebehandling shouldBe true
                KlagehendelseKlagebehandlingAvsluttetUtfall.DELVIS_MEDHOLD -> resultat.skalVæreKnyttetTilRammebehandling shouldBe true
                KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE -> resultat.skalVæreKnyttetTilRammebehandling shouldBe false
                KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST -> resultat.skalVæreKnyttetTilRammebehandling shouldBe true
                KlagehendelseKlagebehandlingAvsluttetUtfall.AVVIST -> resultat.skalVæreKnyttetTilRammebehandling shouldBe false
                KlagehendelseKlagebehandlingAvsluttetUtfall.HENLAGT -> resultat.skalVæreKnyttetTilRammebehandling shouldBe false
            }
        }
    }

    @Test
    fun `omgjøring skal aldri være knyttet til rammebehandling`() {
        OmgjøringskravbehandlingAvsluttetUtfall.entries.forEach { entry ->
            val resultat = ObjectMother.klagebehandlingresultatOpprettholdt(
                klageinstanshendelser = Klageinstanshendelser(
                    listOf(ObjectMother.klageinstanshendelseOmgjøringskrav(utfall = entry)),
                ),
            )

            resultat.skalVæreKnyttetTilRammebehandling shouldBe false
        }
    }

    @Test
    fun `feilregistrert skal aldri være knyttet til rammebehandling`() {
        KlagehendelseFeilregistrertType.entries.forEach { entry ->
            val resultat = ObjectMother.klagebehandlingresultatOpprettholdt(
                klageinstanshendelser = Klageinstanshendelser(listOf(klageinstanshendelseFeilregistrert(type = entry))),
            )

            resultat.skalVæreKnyttetTilRammebehandling shouldBe false
        }
    }
}
