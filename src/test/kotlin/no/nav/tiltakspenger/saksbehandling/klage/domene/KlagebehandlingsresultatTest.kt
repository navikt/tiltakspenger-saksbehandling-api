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
    fun `AVVIST skal ikke være knyttet til rammebehandling`() {
        val resultat = ObjectMother.klagebehandlingresultatAvvist()

        resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
    }

    @Test
    fun `OMGJØR skal alltid være knyttet til rammebehandling`() {
        val resultat = ObjectMother.klagebehandlingresultatOmgjør()

        resultat.`kanVæreKnyttetTilRammebehandling` shouldBe true
    }

    @Test
    fun `visse avsluttet utfall skal knyttes til rammebehandling`() {
        KlagehendelseKlagebehandlingAvsluttetUtfall.entries.forEach { entry ->
            val resultat = ObjectMother.klagebehandlingresultatOpprettholdt(
                klageinstanshendelser = Klageinstanshendelser(listOf(ObjectMother.klageinstanshendelseAvsluttet(utfall = entry))),
            )

            when (entry) {
                KlagehendelseKlagebehandlingAvsluttetUtfall.TRUKKET -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                KlagehendelseKlagebehandlingAvsluttetUtfall.RETUR -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                KlagehendelseKlagebehandlingAvsluttetUtfall.OPPHEVET -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe true
                KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe true
                KlagehendelseKlagebehandlingAvsluttetUtfall.DELVIS_MEDHOLD -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe true
                KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe true
                KlagehendelseKlagebehandlingAvsluttetUtfall.AVVIST -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                KlagehendelseKlagebehandlingAvsluttetUtfall.HENLAGT -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
            }
        }
    }

    @Test
    fun `visse omgjørings utfall skal knyttes til rammebehandling`() {
        OmgjøringskravbehandlingAvsluttetUtfall.entries.forEach { entry ->
            val resultat = ObjectMother.klagebehandlingresultatOpprettholdt(
                klageinstanshendelser = Klageinstanshendelser(
                    listOf(ObjectMother.klageinstanshendelseOmgjøringskrav(utfall = entry)),
                ),
            )

            when (entry) {
                OmgjøringskravbehandlingAvsluttetUtfall.MEDHOLD_ETTER_FVL_35 -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                OmgjøringskravbehandlingAvsluttetUtfall.UGUNST -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe true
            }
        }
    }

    @Test
    fun `feilregistrert typer skal ikke knyttes til rammebehandling`() {
        KlagehendelseFeilregistrertType.entries.forEach { entry ->
            val resultat = ObjectMother.klagebehandlingresultatOpprettholdt(
                klageinstanshendelser = Klageinstanshendelser(listOf(klageinstanshendelseFeilregistrert(type = entry))),
            )

            when (entry) {
                KlagehendelseFeilregistrertType.KLAGE -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                KlagehendelseFeilregistrertType.ANKE -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                KlagehendelseFeilregistrertType.ANKE_I_TRYGDERETTEN -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                KlagehendelseFeilregistrertType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
                KlagehendelseFeilregistrertType.OMGJOERINGSKRAV -> resultat.`kanVæreKnyttetTilRammebehandling` shouldBe false
            }
        }
    }
}
