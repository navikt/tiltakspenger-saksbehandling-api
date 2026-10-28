package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import arrow.core.nonEmptySetOf
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdaterMeldekortdagDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.OppgaveOpprettet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.medPeriode
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test

class DelautomatiskStansServiceTest {

    /**
     * Kjører [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb], som oppretter en stans under automatisk behandling.
     */
    private suspend fun TestApplicationContextMedPostgres.opprettAutomatiskStans(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): Revurdering {
        behandleEndring(sak, tiltaksdeltakelse, nåtilstand).shouldBeInstanceOf<RevurderingOpprettet>()

        return sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>().also {
            it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
            it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
            it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
        }
    }

    private suspend fun TestApplicationContextMedPostgres.behandleEndring(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): TiltaksdeltakelseEndringBehandlet {
        oppdaterTiltaksdeltakelse(sak.fnr, nåtilstand)
        tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
            id = tiltaksdeltakelse.internDeltakelseId,
            sakId = sak.id,
            tidspunkt = nå(clock).minusMinutes(20),
        )
        val deltaker = tiltakContext.tiltaksdeltakerRepo
            .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()
        return oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker).getOrFail()
    }

    private fun TestApplicationContextMedPostgres.hentRevurdering(sak: Sak, revurdering: Revurdering): Revurdering =
        sakContext.sakRepo.hentForSakId(sak.id)!!.hentRammebehandling(revurdering.id).shouldBeInstanceOf<Revurdering>()

    private fun Revurdering.skalVæreOverlattTilSaksbehandler(vararg grunner: ManueltBehandlesGrunn) {
        status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        saksbehandler.shouldBeNull()
        manueltBehandlesGrunner shouldBe grunner.toList()
        resultat shouldBe Revurderingsresultat.Stans.empty
    }

    /**
     * Innvilger hele deltakelsen og utbetaler første meldeperiode, 6.–17. januar 2025.
     * Gir saken slik den var før meldekortet, og søknadsvedtaket.
     */
    private suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingMedUtbetaling(
        tac: TestApplicationContextMedPostgres,
        deltakelse: TiltaksdeltakelseIntern,
    ): Pair<Sak, Rammevedtak> {
        val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
            tac = tac,
            tiltaksdeltakelse = deltakelse,
            innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
        )
        val (sakMedUtbetaling) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
        )!!
        sakMedUtbetaling.meldeperiodeBeregninger.sisteDagMedUtbetaling shouldBe 17.januar(2025)
        return sak to søknadsvedtak
    }

    @Test
    fun `avbrutt deltakelse med sluttdato i fortiden - fylles ut og sendes til beslutning av tp-sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 20.april(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                it.beslutter.shouldBeNull()
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.begrunnelseVilkårsvurdering.shouldNotBeNull()
                it.skalSendeVedtaksbrev shouldBe true
                val stans = it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
                stans.valgtHjemmel shouldBe nonEmptySetOf(HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak)
                stans.harValgtStansFraFørsteDagSomGirRett shouldBe false
                stans.stansperiode shouldBe (21.april(2025) til 5.mai(2025))
            }
        }
    }

    /**
     * Stans støtter ikke feilutbetaling, så dager som oppdragssystemet allerede har utbetalt etter sluttdatoen skal ikke stanses.
     * Simuleringen fra sluttdatoen viser feilutbetaling for 13.–17. januar, og stansen flyttes til dagen etter siste dag med feilutbetaling.
     * Fem utbetalte dager er det meste som stanses, flere gir opphør.
     */
    @Test
    fun `utbetalt fem dager etter sluttdato - stanses fra dagen etter siste dag med feilutbetaling`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandlingMedUtbetaling(tac, deltakelse)
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 10.januar(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>().stansperiode shouldBe (18.januar(2025) til 5.mai(2025))
                it.begrunnelseVilkårsvurdering!!.verdi shouldContain "Oppdragssystemet har allerede utbetalt tiltakspenger til og med 17. januar 2025"
                it.utbetaling.shouldNotBeNull().simulering.shouldNotBeNull().harFeilutbetaling shouldBe false
                it.prioritering.shouldBeNull()
            }
        }
    }

    @Test
    fun `avbrutt tilbake i tid i en innvilgelse som er passert - fylles ut og sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 31.mars(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 20.februar(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>().stansperiode shouldBe (21.februar(2025) til 31.mars(2025))
            }
        }
    }

    /**
     * Simuleringen fra sluttdatoen viser feilutbetaling for seks utbetalingsdager, 10. og 13.–17. januar.
     * Da opprettes et utfylt og prioritert opphør fra dagen etter sluttdatoen, som overlates til en saksbehandler uten å behandles automatisk.
     * Innvilgelsen er passert, så opphøret gjelder i sin helhet tilbake i tid.
     */
    @Test
    fun `utbetalt mer enn fem dager etter sluttdato - opprettes opphør som overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 31.mars(2025))
            val (sak) = iverksettSøknadsbehandlingMedUtbetaling(tac, deltakelse)

            tac.behandleEndring(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 9.januar(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            ).shouldBeInstanceOf<RevurderingOpprettet>()

            val opphør = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(opphør.id)

            tac.hentRevurdering(sak, opphør).also {
                it shouldBe opphør
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                it.saksbehandler.shouldBeNull()
                it.erUnderAutomatiskBehandling shouldBe false
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.automatiskOpprettetGrunn.shouldNotBeNull().tiltaksdeltakerId shouldBe deltakelse.internDeltakelseId
                it.skalSendeVedtaksbrev shouldBe true
                val resultat = it.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringOpphør>()
                resultat.vedtaksperiode shouldBe (10.januar(2025) til 31.mars(2025))
                resultat.valgteHjemler shouldBe nonEmptySetOf(HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak)
                resultat.omgjørRammevedtak.rammevedtakIDer shouldBe listOf(sak.rammevedtaksliste.single().id)
                it.begrunnelseVilkårsvurdering!!.verdi shouldBe
                    "Automatisk opprettet. Tiltaksdeltakelsen er avbrutt, og tiltakspengene opphøres fra og med 10. januar 2025 fordi bruker ikke lenger deltar på arbeidsmarkedstiltak. " +
                    "Oppdragssystemet har allerede utbetalt tiltakspenger for 6 dager fra og med 10. januar 2025, og utbetalingen for disse dagene blir feilutbetaling."
                it.prioritering shouldBe Prioritering.create(
                    "Opphør må behandles omgående for å begrense feilutbetaling. " +
                        "Oppdragssystemet har allerede utbetalt tiltakspenger for 6 dager etter at deltakelsen ble avbrutt.",
                )
                it.utbetaling.shouldNotBeNull().simulering.shouldNotBeNull().harFeilutbetaling shouldBe true
            }
        }
    }

    /**
     * Et opphør over 10. januar–5. mai ville omgjort både søknadsvedtaket og omgjøringen fra 1. februar.
     */
    @Test
    fun `utbetalt mer enn fem dager og opphøret ville omgjort flere vedtak - oppretter oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 5.mai(2025))
            val (sak, søknadsvedtak) = iverksettSøknadsbehandlingMedUtbetaling(tac, deltakelse)
            iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadsvedtak.id,
                vedtaksperiode = 1.februar(2025) til 5.mai(2025),
                innvilgelsesperioder = innvilgelsesperioder(1.februar(2025) til 5.mai(2025), deltakelse),
            )

            tac.behandleEndring(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 9.januar(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            ).shouldBeInstanceOf<OppgaveOpprettet>()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.filterIsInstance<Revurdering>()
                .filter { it.automatiskOpprettetGrunn != null }.shouldBeEmpty()
        }
    }

    /**
     * Fram til oppdragssystemet har utbetalt, viser ikke simuleringen feilutbetaling, og stansen gjelder fra dagen etter sluttdatoen.
     * Den prioriteres, siden den må iverksettes før oppdragssystemet utbetaler.
     */
    @Test
    fun `sendt utbetaling som oppdragssystemet ikke har utbetalt - stanses fra dagen etter sluttdato og prioriteres`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandlingMedUtbetaling(tac, deltakelse)
            tac.utbetalingFakeKlient.sakUtbetaltAvOppdrag = sak
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 7.januar(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>().stansperiode shouldBe (8.januar(2025) til 5.mai(2025))
                it.begrunnelseVilkårsvurdering!!.verdi shouldNotContain "Oppdragssystemet har allerede utbetalt"
                it.prioritering shouldBe Prioritering.create(
                    "Stans må behandles omgående for å hindre feilutbetaling. " +
                        "Det er sendt utbetaling av tiltakspenger til og med 17. januar 2025 som oppdragssystemet ikke har utbetalt ennå.",
                )
            }
        }
    }

    /**
     * Stansen opprettes mens innvilgelsen fortsatt løper, men meldekortet behandles før stansen behandles automatisk.
     */
    @Test
    fun `alle innvilgede dager etter sluttdato er utbetalt - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres(clock = TikkendeKlokke(fixedClockAt(8.januar(2025)))) { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 9.januar(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 7.januar(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )

            tac.clock.spolTil(1.mai(2025).atStartOfDay())
            val meldeperiode = sak.meldeperiodeKjeder.first().siste
            val (sakMedUtbetaling) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = meldeperiode.kjedeId,
                meldeperioder = listOf(
                    OppdatertMeldeperiodeDTO(
                        kjedeId = meldeperiode.kjedeId.verdi,
                        dager = meldeperiode.periode.tilDager().map {
                            OppdaterMeldekortdagDTO(
                                dato = it,
                                status = if (deltakelse.lukketPeriode!!.inneholder(it)) {
                                    MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
                                } else {
                                    MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
                                },
                            )
                        },
                    ),
                ),
            )!!
            sakMedUtbetaling.meldeperiodeBeregninger.sisteDagMedUtbetaling shouldBe 9.januar(2025)

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering)
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.STANS_INGEN_INNVILGEDE_DAGER_ETTER_SISTE_UTBETALING)
        }
    }

    @Test
    fun `avbrutt deltakelse med sluttdato i fremtiden - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.STANS_SLUTTDATO_ER_IKKE_PASSERT)
        }
    }

    @Test
    fun `deltakelsen er tatt opp igjen før stansen behandles - overlates til saksbehandler med begge grunnene`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 20.april(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse)

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(
                ManueltBehandlesGrunn.STANS_DELTAKELSEN_ER_IKKE_AVSLUTTET,
                ManueltBehandlesGrunn.STANS_SLUTTDATO_ER_IKKE_PASSERT,
            )
        }
    }

    /**
     * Endringer som kommer før stansen er behandlet, fanges opp når stansen henter nå-tilstanden på nytt.
     * Etterpå sammenlignes de med stansen, først som åpen behandling og så som gjeldende vedtak.
     * Ingen av dem skal gi nye revurderinger eller oppgaver.
     */
    @Test
    fun `flere endringer før og etter automatisk stans gir verken ny revurdering eller oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val avbrutt = deltakelse.medPeriode(tilOgMed = 20.april(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt)
            val revurdering = tac.opprettAutomatiskStans(sak = sak, tiltaksdeltakelse = deltakelse, nåtilstand = avbrutt)
            val avbruttTidligere = avbrutt.medPeriode(tilOgMed = 15.april(2025))

            tac.behandleEndring(sak, deltakelse, avbruttTidligere).shouldBeInstanceOf<IngenRelevantEndring>()

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>().stansperiode shouldBe (16.april(2025) til 5.mai(2025))
            }
            tac.behandleEndring(sak, deltakelse, avbruttTidligere).shouldBeInstanceOf<IngenRelevantEndring>()

            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.behandleEndring(sak, deltakelse, avbruttTidligere).shouldBeInstanceOf<IngenRelevantEndring>()
            tac.behandleEndring(sak, deltakelse, avbrutt).shouldBeInstanceOf<IngenRelevantEndring>()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe
                listOf(sak.rammebehandlinger.single().id, revurdering.id)
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    @Test
    fun `annen åpen behandling på saken - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 20.april(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )
            startRevurderingInnvilgelse(tac, sak.id).shouldNotBeNull()

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING)
        }
    }

    @Test
    fun `annen åpen behandling og sluttdato i fremtiden - overlates til saksbehandler med begge grunnene`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskStans(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 20.april(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
            )
            startRevurderingInnvilgelse(tac, sak.id).shouldNotBeNull()
            tac.oppdaterTiltaksdeltakelse(sak.fnr, deltakelse.medPeriode(tilOgMed = 2.mai(2025)).copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt))

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(
                ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING,
                ManueltBehandlesGrunn.STANS_SLUTTDATO_ER_IKKE_PASSERT,
            )
        }
    }

    @Test
    fun `manuelt opprettet stans - behandles ikke automatisk`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val (_, revurdering) = startRevurderingStans(tac, sak.id)!!

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering) shouldBe revurdering
        }
    }
}
