package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO.MeldekortvedtakDTO.Reduksjon
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status
import no.nav.tiltakspenger.saksbehandling.beregning.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.brukersMeldekort
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.meldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySakMedVedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tilMeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.time.Clock

/**
 * Verifiserer mappingen via det offentlige inngangspunktet [Sak.tilMeldekortApiDTO] heller enn
 * å teste private hjelpere direkte. Dekker uttømmende alle [InnmeldtStatus]- og
 * [ReduksjonAvYtelsePåGrunnAvFravær]-verdier slik at en ny dag-/reduksjon-type ikke kan legges
 * til uten at den blir mappet riktig over kontrakten mot meldekort-api.
 */
class SakTilMeldekortApiDTOMapperTest {

    @TestFactory
    fun `Sak#tilMeldekortApiDTO mapper hver dag-status til riktig Status og Reduksjon i DTO`(): List<DynamicTest> {
        // Dager uten rett mappes alltid til IKKE_RETT_TIL_TILTAKSPENGER, uavhengig av brukers status.
        // IKKE_RETT_TIL_TILTAKSPENGER kan ikke settes av bruker på en dag med rett, så den dekkes via
        // ikke-rett dagene i meldeperioden (se sjekken nederst i testen).
        //
        // For ikke-syk dag-typer er reduksjonen konstant per type, og verifiseres her. De to syk-
        // tilfellene (FRAVÆR_SYK, FRAVÆR_SYKT_BARN) kan ha alle tre reduksjonsverdier avhengig av
        // arbeidsgiverperioden, men her bygger vi beregningen med default IngenReduksjon — den
        // fulle variasjonen dekkes av den andre testen under.
        val cases: List<Triple<InnmeldtStatus, Status, Reduksjon>> = listOf(
            Triple(InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET, Status.DELTATT_UTEN_LØNN_I_TILTAKET, Reduksjon.INGEN_REDUKSJON),
            Triple(InnmeldtStatus.DELTATT_MED_LØNN_I_TILTAKET, Status.DELTATT_MED_LØNN_I_TILTAKET, Reduksjon.YTELSEN_FALLER_BORT),
            Triple(InnmeldtStatus.FRAVÆR_SYK, Status.FRAVÆR_SYK, Reduksjon.INGEN_REDUKSJON),
            Triple(InnmeldtStatus.FRAVÆR_SYKT_BARN, Status.FRAVÆR_SYKT_BARN, Reduksjon.INGEN_REDUKSJON),
            Triple(InnmeldtStatus.FRAVÆR_GODKJENT_AV_NAV, Status.FRAVÆR_GODKJENT_AV_NAV, Reduksjon.INGEN_REDUKSJON),
            Triple(
                InnmeldtStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
                Status.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
                Reduksjon.INGEN_REDUKSJON,
            ),
            Triple(InnmeldtStatus.FRAVÆR_ANNET, Status.FRAVÆR_ANNET, Reduksjon.YTELSEN_FALLER_BORT),
            Triple(InnmeldtStatus.IKKE_TILTAKSDAG, Status.IKKE_TILTAKSDAG, Reduksjon.YTELSEN_FALLER_BORT),
            Triple(InnmeldtStatus.IKKE_BESVART, Status.IKKE_BESVART, Reduksjon.YTELSEN_FALLER_BORT),
        )

        // Sanity-sjekk: kombinert med IKKE_RETT_TIL_TILTAKSPENGER fra ikke-rett dagene dekker vi alle
        // Status-verdiene i DTO-kontrakten.
        (cases.map { it.second } + Status.IKKE_RETT_TIL_TILTAKSPENGER).toSet() shouldBe Status.entries.toSet()

        return cases.map { (innmeldtStatus, forventetStatus, forventetReduksjon) ->
            DynamicTest.dynamicTest("$innmeldtStatus -> $forventetStatus / $forventetReduksjon") {
                val (sak, meldeperiode) = sakMedFørsteMeldeperiode()

                val sakMedVedtak = sak.medMeldekortvedtak(
                    meldeperiode = meldeperiode,
                    statusPåRettDager = innmeldtStatus,
                )

                val dagerDto = sakMedVedtak.tilMeldekortApiDTO()
                    .meldekortvedtak.single()
                    .meldeperiodebehandlinger.single()
                    .dager

                dagerDto.size shouldBe meldeperiode.periode.antallDager.toInt()
                dagerDto.forEach { dag ->
                    val harRett = meldeperiode.girRett[dag.dato] == true
                    if (harRett) {
                        dag.status shouldBe forventetStatus
                        dag.reduksjon shouldBe forventetReduksjon
                    } else {
                        // Ikke-rett dager blir alltid IkkeRettTilTiltakspenger med YtelsenFallerBort.
                        dag.status shouldBe Status.IKKE_RETT_TIL_TILTAKSPENGER
                        dag.reduksjon shouldBe Reduksjon.YTELSEN_FALLER_BORT
                    }
                }
            }
        }
    }

    @TestFactory
    fun `Sak#tilMeldekortApiDTO mapper Reduksjon for syk bruker og sykt barn til riktig Reduksjon i DTO`(): List<DynamicTest> {
        // De to syk-tilfellene (FRAVÆR_SYK = bruker selv, FRAVÆR_SYKT_BARN = sykt barn / barnepasser)
        // kan i prinsippet føre til alle tre reduksjonsverdier. Typisk forløp:
        //   - INGEN_REDUKSJON de første 3 dagene (egenmelding)
        //   - REDUKSJON i de neste 13 dagene
        //   - YTELSEN_FALLER_BORT når arbeidsgiverperioden er over
        // Domenemodellen passer på dette regelverket; selve DTO-en er "dum" og skal bare propagere
        // verdien som beregningen har satt på dagen. Her verifiserer vi nettopp at DTO-mappingen er
        // tro mot beregningens reduksjon, krysset over begge syk-tilfeller × alle reduksjonsverdier.
        val sykTilfeller = listOf(InnmeldtStatus.FRAVÆR_SYK, InnmeldtStatus.FRAVÆR_SYKT_BARN)
        val reduksjonsverdier: List<Pair<ReduksjonAvYtelsePåGrunnAvFravær, Reduksjon>> = listOf(
            ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon to Reduksjon.INGEN_REDUKSJON,
            ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon to Reduksjon.REDUKSJON,
            ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort to Reduksjon.YTELSEN_FALLER_BORT,
        )

        // Sanity-sjekk: alle Reduksjon-verdiene i DTO-kontrakten dekkes
        reduksjonsverdier.map { it.second }.toSet() shouldBe Reduksjon.entries.toSet()

        return sykTilfeller.flatMap { sykStatus ->
            reduksjonsverdier.map { (reduksjon, forventet) ->
                DynamicTest.dynamicTest("$sykStatus + $reduksjon -> $forventet") {
                    val (sak, meldeperiode) = sakMedFørsteMeldeperiode()

                    val sakMedVedtak = sak.medMeldekortvedtak(
                        meldeperiode = meldeperiode,
                        statusPåRettDager = sykStatus,
                        reduksjon = reduksjon,
                    )

                    val dagerDto = sakMedVedtak.tilMeldekortApiDTO()
                        .meldekortvedtak.single()
                        .meldeperiodebehandlinger.single()
                        .dager

                    dagerDto.forEach { dag ->
                        val harRett = meldeperiode.girRett[dag.dato] == true
                        // Kun rett-dagene har syk-status; no-rett-dager blir IkkeRettTilTiltakspenger
                        // som alltid har IngenReduksjon.
                        if (harRett) {
                            dag.reduksjon shouldBe forventet
                        }
                    }
                }
            }
        }
    }

    private fun sakMedFørsteMeldeperiode(): Pair<Sak, Meldeperiode> {
        val (sakMedMeldeperioder, meldeperioder) = nySakMedVedtak().first.genererMeldeperioder(fixedClock)
        return sakMedMeldeperioder to meldeperioder.first()
    }

    /**
     * Bygger en [Sak] med ett iverksatt meldekortvedtak hvor alle dager med rett har [statusPåRettDager]
     * og hvor reduksjonen i beregningen er [reduksjon]. Bygger via de samme domene-byggerne som
     * produksjonsflyten, slik at testen verifiserer hele kjeden fram til [Sak.tilMeldekortApiDTO].
     */
    private fun Sak.medMeldekortvedtak(
        meldeperiode: Meldeperiode,
        statusPåRettDager: InnmeldtStatus,
        reduksjon: ReduksjonAvYtelsePåGrunnAvFravær = ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon,
    ): Sak {
        // Forhåndsallokerer meldekort-id'en, ellers vil beregningen og meldekortbehandlingen havne
        // på hver sin id og bryte invariansen i Meldekortvedtak.
        val meldekortId = MeldekortId.random()
        // Behandlinger og vedtak må ha distinkte opprettet-tidspunkt, så vi tikker klokka forbi
        // rammebehandlingens / rammevedtakets tidspunkt.
        val senereKlokke: Clock = Clock.fixed(fixedClock.instant().plusSeconds(2), fixedClock.zone)
        val brukersMk = brukersMeldekort(
            sakId = id,
            meldeperiode = meldeperiode,
            behandlesAutomatisk = true,
            dager = meldeperiode.girRett.entries.map { (dato, harRett) ->
                BrukersMeldekortDag(
                    dato = dato,
                    status = if (harRett) statusPåRettDager else InnmeldtStatus.IKKE_BESVART,
                )
            },
        )
        val mkb = meldekortBehandletAutomatisk(
            id = meldekortId,
            sakId = id,
            saksnummer = saksnummer,
            fnr = fnr,
            // Må være etter rammebehandlingens opprettet-tidspunkt for å tilfredsstille invariansen
            // i Behandlinger om at to behandlinger ikke kan ha samme opprettet-tidspunkt.
            opprettet = nå(senereKlokke),
            meldeperiode = meldeperiode,
            brukersMeldekort = brukersMk,
            beregning = brukersMk.tilMeldekortBeregning(
                meldekortbehandlingId = meldekortId,
                reduksjon = reduksjon,
            ),
        )
        val vedtak = mkb.opprettVedtak(forrigeUtbetaling = utbetalinger.lastOrNull(), clock = senereKlokke)
        return copy(
            behandlinger = behandlinger.leggTilMeldekortBehandletAutomatisk(mkb),
            vedtaksliste = vedtaksliste.leggTilMeldekortvedtak(vedtak),
        )
    }
}
