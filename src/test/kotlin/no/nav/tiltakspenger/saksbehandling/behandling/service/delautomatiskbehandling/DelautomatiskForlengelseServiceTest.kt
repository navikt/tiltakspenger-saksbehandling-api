package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdaterMeldekortdagDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggMedIdent
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggUtenIdent
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.toggleKanSendeHelgForMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DelautomatiskForlengelseServiceTest {

    /**
     * Kjører [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb], som oppretter en forlengelse under automatisk behandling.
     */
    private suspend fun TestApplicationContextMedPostgres.opprettAutomatiskForlengelse(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): Revurdering {
        behandleEndring(sak, tiltaksdeltakelse, nåtilstand).shouldBeInstanceOf<RevurderingOpprettet>()

        return sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>().also {
            it.resultat shouldBe Revurderingsresultat.Innvilgelse.empty
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

    private fun Revurdering.skalVæreOverlattTilSaksbehandler(grunn: ManueltBehandlesGrunn) {
        status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        saksbehandler.shouldBeNull()
        manueltBehandlesGrunner shouldBe listOf(grunn)
        resultat shouldBe Revurderingsresultat.Innvilgelse.empty
    }

    @Test
    fun `deltakelsen er forlenget - fylles ut og sendes til beslutning av tp-sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                it.beslutter.shouldBeNull()
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldBe
                    "Automatisk behandlet.\nTiltaksdeltakelsen er forlenget til 30. juni 2025."
                it.skalSendeVedtaksbrev shouldBe true
                it.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
                it.innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (6.mai(2025) til 30.juni(2025))
                it.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct() shouldBe
                    vedtak.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct()
                it.barnetillegg.shouldNotBeNull().also { barnetillegg ->
                    barnetillegg.periodisering.verdier.distinct() shouldBe listOf(AntallBarn.ZERO)
                    barnetillegg.begrunnelse.shouldBeNull()
                }
            }
        }
    }

    /**
     * Iverksetter en innvilgelse av deltakelsen, oppretter en forlengelse til 30. juni og registrerer [nåtilstand] før forlengelsen behandles.
     * Gir forlengelsen slik den er lagret etter den automatiske behandlingen.
     *
     * @param tidligereTilstand Deltakelsen slik den var da søknaden ble innvilget.
     * @param nåtilstand Deltakelsen slik den er registrert når forlengelsen behandles, eller null om den ikke lenger finnes.
     * @param innvilgetTilOgMed Siste dag i innvilgelsen, som ellers følger deltakelsen.
     * @param barnFraSøknad Barna i søknaden som innvilges.
     * @param antallBarnIVedtaket Antall barn det innvilges barnetillegg for i hele vedtaksperioden.
     * @param etterInnvilgelse Kjøres etter at søknaden er innvilget, før forlengelsen opprettes.
     * @param førBehandling Kjøres etter at forlengelsen er opprettet, før den behandles.
     */
    private suspend fun ApplicationTestBuilder.forlengOgBehandleAutomatisk(
        tac: TestApplicationContextMedPostgres,
        tidligereTilstand: (TiltaksdeltakelseIntern) -> TiltaksdeltakelseIntern = { it },
        nåtilstand: (TiltaksdeltakelseIntern) -> TiltaksdeltakelseIntern? = { it },
        innvilgetTilOgMed: LocalDate = 5.mai(2025),
        barnFraSøknad: List<BarnetilleggFraSøknad> = emptyList(),
        antallBarnIVedtaket: AntallBarn = AntallBarn(barnFraSøknad.size),
        kanSendeHelg: Boolean = false,
        etterInnvilgelse: suspend ApplicationTestBuilder.(Sak) -> Unit = {},
        førBehandling: suspend ApplicationTestBuilder.(Sak) -> Unit = {},
    ): Revurdering {
        val deltakelse = tidligereTilstand(tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025)))
        val innvilgelsesperiode = deltakelse.deltakelseFraOgMed!! til innvilgetTilOgMed
        val (sak) = iverksettSøknadsbehandling(
            tac = tac,
            tiltaksdeltakelse = deltakelse,
            innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, deltakelse),
            barnetillegg = barnetillegg(periode = innvilgelsesperiode, antallBarn = antallBarnIVedtaket),
            barnFraSøknad = barnFraSøknad,
        )
        if (kanSendeHelg) {
            toggleKanSendeHelgForMeldekort(tac, sak.id, kanSendeHelg = true)
        }
        etterInnvilgelse(sak)
        val forlenget = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025))
        val revurdering = tac.opprettAutomatiskForlengelse(sak = sak, tiltaksdeltakelse = deltakelse, nåtilstand = forlenget)
        førBehandling(sak)
        tac.oppdaterTiltaksdeltakelse(sak.fnr, nåtilstand(forlenget))

        tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

        return tac.hentRevurdering(sak, revurdering)
    }

    private fun Revurdering.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode: Int, antallBarn: AntallBarn = AntallBarn.ZERO) {
        status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
        manueltBehandlesGrunner.shouldBeEmpty()
        innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (6.mai(2025) til 30.juni(2025))
        this.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct() shouldBe listOf(AntallDagerForMeldeperiode(antallDagerPerMeldeperiode))
        barnetillegg.shouldNotBeNull().periodisering.also {
            it.totalPeriode shouldBe (6.mai(2025) til 30.juni(2025))
            it.verdier.distinct() shouldBe listOf(antallBarn)
        }
    }

    @Test
    fun `deltakelsesmengden er endret til færre dager per uke - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = 2.5F, deltakelseProsent = 50F) })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE)
        }
    }

    @Test
    fun `kun prosenten er endret - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = 5F, deltakelseProsent = 80F) })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE)
        }
    }

    @Test
    fun `deltakelsesmengden var ikke oppgitt i vedtaket og er oppgitt nå - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                tidligereTilstand = { it.copy(antallDagerPerUke = null, deltakelseProsent = null) },
                nåtilstand = { it.copy(antallDagerPerUke = null, deltakelseProsent = 80F) },
            ).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE)
        }
    }

    @Test
    fun `deltakelsesmengden er ikke lenger oppgitt - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = null, deltakelseProsent = null) })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE)
        }
    }

    @Test
    fun `deltakelsesmengden er endret og bruker kan melde i helg - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = 2F) }, kanSendeHelg = true)
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_DELTAKELSESMENGDE)
        }
    }

    /**
     * Fulltid regnes som samme deltakelsesmengde uansett om den er oppgitt som dager per uke eller prosent.
     */
    @Test
    fun `fulltid er oppgitt som prosent i stedet for dager per uke - regnes som uendret og sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(antallDagerPerUke = null, deltakelseProsent = 100F) })
                .skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10)
        }
    }

    /**
     * Antall dager per meldeperiode videreføres fra vedtaket, også når saksbehandler har satt et annet antall enn deltakelsesmengden tilsier.
     */
    @Test
    fun `uendret deltid - antall dager per meldeperiode videreføres fra vedtaket`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltid: (TiltaksdeltakelseIntern) -> TiltaksdeltakelseIntern = { it.copy(antallDagerPerUke = 3F, deltakelseProsent = 60F) }
            forlengOgBehandleAutomatisk(tac, tidligereTilstand = deltid, nåtilstand = deltid).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10)
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldBe
                    "Automatisk behandlet.\nTiltaksdeltakelsen er forlenget til 30. juni 2025."
            }
        }
    }

    @Test
    fun `startdatoen er endret - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(deltakelseFraOgMed = 6.januar(2025)) })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ENDRET_STARTDATO)
        }
    }

    @Test
    fun `sluttdatoen er satt tilbake til siste innvilgede dag før behandlingen - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(deltakelseTilOgMed = 5.mai(2025)) })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_SLUTTDATO_ER_IKKE_ETTER_INNVILGELSEN)
        }
    }

    @Test
    fun `sluttdatoen er fjernet før behandlingen - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(deltakelseTilOgMed = null) })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSEN_MANGLER_SLUTTDATO)
        }
    }

    @Test
    fun `deltakelsen er ikke aktuell før behandlingen - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { it.copy(deltakelseStatus = TiltakDeltakerstatus.IkkeAktuell) })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_DELTAKELSEN_HAR_UGYLDIG_STATUS)
        }
    }

    @Test
    fun `deltakelsen er borte fra registeret før behandlingen - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, nåtilstand = { null })
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_FANT_IKKE_TILTAKSDELTAKELSE)
        }
    }

    @Test
    fun `innvilgelsen sluttet før deltakelsen - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, innvilgetTilOgMed = 30.april(2025))
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_INNVILGELSEN_SLUTTER_FOR_DELTAKELSEN)
        }
    }

    @Test
    fun `bruker har andre ytelser - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                etterInnvilgelse = { sak ->
                    tac.leggTilYtelserFraUtbetaldata(
                        fnr = sak.fnr,
                        ytelser = listOf(Ytelse(ytelsetype = Ytelsetype.DAGPENGER, perioder = listOf(1.juni(2025) til 30.juni(2025)))),
                    )
                },
            ).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_ANDRE_YTELSER)
        }
    }

    @Test
    fun `bruker har bare tiltakspenger fra utbetaldata - sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                etterInnvilgelse = { sak ->
                    tac.leggTilYtelserFraUtbetaldata(
                        fnr = sak.fnr,
                        ytelser = listOf(Ytelse(ytelsetype = Ytelsetype.TILTAKSPENGER, perioder = listOf(5.januar(2025) til 30.april(2025)))),
                    )
                },
            ).skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10)
        }
    }

    @Test
    fun `bruker har tiltakspengevedtak i Arena - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                etterInnvilgelse = { sak ->
                    tac.leggTilTiltakspengevedtakFraArena(
                        fnr = sak.fnr,
                        vedtak = listOf(
                            ArenaTPVedtak(
                                fraOgMed = 1.juni(2025),
                                tilOgMed = 30.juni(2025),
                                rettighet = ArenaTPVedtak.Rettighet.TILTAKSPENGER,
                                vedtakId = 1,
                            ),
                        ),
                    )
                },
            ).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_VEDTAK_I_ARENA)
        }
    }

    /**
     * Et vedtak som overlapper forlengelsen før den opprettes, gir oppgave i stedet for forlengelse.
     * Her iverksettes vedtaket mens forlengelsen venter på automatisk behandling.
     */
    @Test
    fun `en annen deltakelse innvilges i forlengelsen før den behandles - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                førBehandling = { sak ->
                    val annenDeltakelse = tac.tiltaksdeltakelse(2.juni(2025) til 31.august(2025))
                    iverksettSøknadsbehandling(
                        tac = tac,
                        sakId = sak.id,
                        fnr = sak.fnr,
                        tiltaksdeltakelse = annenDeltakelse,
                        innvilgelsesperioder = innvilgelsesperioder(annenDeltakelse.periode!!, annenDeltakelse),
                    )
                },
            ).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_OVERLAPPER_ANDRE_VEDTAK)
        }
    }

    @Test
    fun `bruker har søkt for en annen deltakelse som overlapper forlengelsen - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                etterInnvilgelse = { sak ->
                    val annenDeltakelse = tac.tiltaksdeltakelse(2.juni(2025) til 31.august(2025))
                    iverksettSøknadsbehandling(
                        tac = tac,
                        sakId = sak.id,
                        fnr = sak.fnr,
                        resultat = SøknadsbehandlingsresultatType.AVSLAG,
                        tiltaksdeltakelse = annenDeltakelse,
                        innvilgelsesperioder = innvilgelsesperioder(annenDeltakelse.periode!!, annenDeltakelse),
                    )
                },
            ).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_OVERLAPPENDE_TILTAK)
        }
    }

    @Test
    fun `bruker kan melde i helg med uendret deltakelsesmengde - fylles ut og sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, kanSendeHelg = true).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10)
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldNotContain "dager per meldeperiode"
            }
        }
    }

    @Test
    fun `vedtaket har barnetillegg for barna i søknaden - barnetillegget videreføres`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015)), barnetilleggUtenIdent(fødselsdato = 1.januar(2018))),
            ).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10, antallBarn = AntallBarn(2))
                it.barnetillegg.shouldNotBeNull().begrunnelse.shouldNotBeNull().verdi shouldBe
                    "Barnetillegg for 2 barn videreføres fra det gjeldende vedtaket."
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldNotContain "arnetillegg"
            }
        }
    }

    @Test
    fun `et barn fyller 16 i forlengelsen - barnetillegget reduseres fra 16-årsdagen`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015)), barnetilleggMedIdent(fødselsdato = 1.juni(2009))),
            ).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.barnetillegg.shouldNotBeNull().periodisering.perioderMedVerdi.toList() shouldBe listOf(
                    PeriodeMedVerdi(AntallBarn(2), 6.mai(2025) til 31.mai(2025)),
                    PeriodeMedVerdi(AntallBarn(1), 1.juni(2025) til 30.juni(2025)),
                )
                it.barnetillegg.shouldNotBeNull().begrunnelse.shouldNotBeNull().verdi shouldBe
                    "Barnetillegget for 2 barn i det gjeldende vedtaket reduseres når barn fyller 16 år:\n" +
                    "2 barn fra og med 6. mai 2025 til og med 31. mai 2025.\n" +
                    "1 barn fra og med 1. juni 2025 til og med 30. juni 2025."
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldNotContain "arnetillegg"
            }
        }
    }

    @Test
    fun `forlengelse der et barn fyller 16 iverksettes - vedtaket får periodisert barnetillegg`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
                barnetillegg = barnetillegg(periode = deltakelse.periode!!, antallBarn = AntallBarn(2)),
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015)), barnetilleggMedIdent(fødselsdato = 1.juni(2009))),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammevedtaksliste.also { vedtaksliste ->
                vedtaksliste.last().rammebehandling.id shouldBe revurdering.id
                vedtaksliste.barnetilleggsperioder.perioderMedVerdi.toList() shouldBe listOf(
                    PeriodeMedVerdi(AntallBarn(2), 5.januar(2025) til 31.mai(2025)),
                    PeriodeMedVerdi(AntallBarn(1), 1.juni(2025) til 30.juni(2025)),
                )
                vedtaksliste.antallDagerPerMeldeperiode.perioderMedVerdi.toList() shouldBe listOf(
                    PeriodeMedVerdi(AntallDagerForMeldeperiode(10), 5.januar(2025) til 30.juni(2025)),
                )
            }
        }
    }

    @Test
    fun `det eneste barnet fyller 16 dagen etter vedtaket - forlengelsen får ikke barnetillegg`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(tac, barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 6.mai(2009)))).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10, antallBarn = AntallBarn.ZERO)
                it.barnetillegg.shouldNotBeNull().begrunnelse.shouldBeNull()
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldBe
                    "Automatisk behandlet.\n" +
                    "Tiltaksdeltakelsen er forlenget til 30. juni 2025.\n" +
                    "Barnetillegget for 1 barn i det gjeldende vedtaket faller bort, fordi barna har fylt 16 år."
            }
        }
    }

    @Test
    fun `vedtaket har ikke barnetillegg selv om søknaden har barn - forlengelsen får ikke barnetillegg`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015))),
                antallBarnIVedtaket = AntallBarn.ZERO,
            ).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10, antallBarn = AntallBarn.ZERO)
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldNotContain "arnetillegg"
                it.barnetillegg.shouldNotBeNull().begrunnelse.shouldBeNull()
            }
        }
    }

    @Test
    fun `vedtaket har barnetillegg for flere barn enn søknaden - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015))),
                antallBarnIVedtaket = AntallBarn(2),
            ).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG)
        }
    }

    @Test
    fun `vedtaket har barnetillegg for et barn utenfor EØS - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015), oppholderSegIEØS = Søknad.JaNeiSpm.Nei)),
                antallBarnIVedtaket = AntallBarn(1),
            ).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG)
        }
    }

    @Test
    fun `søknaden har et barn som blir født i forlengelsen - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            forlengOgBehandleAutomatisk(
                tac,
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015)), barnetilleggMedIdent(fødselsdato = 1.juni(2025))),
                antallBarnIVedtaket = AntallBarn(1),
            ).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_KAN_IKKE_UTLEDE_BARNETILLEGG)
        }
    }

    @Test
    fun `vedtaket er stanset før forlengelsen behandles - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            iverksettRevurderingStans(tac = tac, sakId = sak.id, stansFraOgMed = 2.mai(2025))

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_VEDTAKET_ER_STANSET_ELLER_OPPHORT)
        }
    }

    @Test
    fun `hele vedtaket er stanset før forlengelsen behandles - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            iverksettRevurderingStans(tac = tac, sakId = sak.id)

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.FORLENGELSE_FANT_IKKE_GJELDENDE_VEDTAK)
        }
    }

    /**
     * Endringer som kommer før forlengelsen er behandlet, fanges opp når forlengelsen henter nå-tilstanden på nytt.
     * Etterpå sammenlignes de med forlengelsen, først som åpen behandling og så som gjeldende vedtak.
     * Ingen av dem skal gi nye revurderinger eller oppgaver.
     */
    @Test
    fun `flere forlengelser før og etter automatisk forlengelse gir verken ny revurdering eller oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            val forlengetIgjen = deltakelse.copy(deltakelseTilOgMed = 31.juli(2025))

            tac.behandleEndring(sak, deltakelse, forlengetIgjen).shouldBeInstanceOf<IngenRelevantEndring>()

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (6.mai(2025) til 31.juli(2025))
            }
            tac.behandleEndring(sak, deltakelse, forlengetIgjen).shouldBeInstanceOf<IngenRelevantEndring>()

            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.behandleEndring(sak, deltakelse, forlengetIgjen).shouldBeInstanceOf<IngenRelevantEndring>()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id } shouldBe
                listOf(sak.rammebehandlinger.single().id, revurdering.id)
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    /**
     * Meldekortet for meldeperioden der deltakelsen forlenges, er behandlet før forlengelsen.
     * Forlengelsen beregnes og simuleres derfor på nytt, uten at de utbetalte beløpene endres.
     * Dagene etter den tidligere sluttdatoen står fortsatt uten rett, til et nytt meldekort korrigerer dem.
     */
    @Test
    fun `meldekortet for meldeperioden som forlenges er behandlet - beregnes, simuleres og iverksettes`() {
        withTestApplicationContextAndPostgres(clock = TikkendeKlokke(fixedClockAt(1.mai(2025)))) { tac ->
            val deltakelse = tac.tiltaksdeltakelse(28.april(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            tac.clock.spolTil(12.mai(2025).atStartOfDay())
            val meldeperiode = sak.meldeperiodeKjeder.single().siste
            meldeperiode.periode shouldBe (28.april(2025) til 11.mai(2025))
            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = meldeperiode.kjedeId,
                meldeperioder = listOf(
                    OppdatertMeldeperiodeDTO(
                        kjedeId = meldeperiode.kjedeId.verdi,
                        dager = meldeperiode.periode.tilDager().map {
                            OppdaterMeldekortdagDTO(
                                dato = it,
                                status = if (deltakelse.periode!!.inneholder(it)) {
                                    MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
                                } else {
                                    MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
                                },
                            )
                        },
                    ),
                ),
            ).shouldNotBeNull()

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.skalVæreSendtTilBeslutningMed(antallDagerPerMeldeperiode = 10)
                val utbetaling = it.utbetaling.shouldNotBeNull()
                utbetaling.simulering.shouldBeInstanceOf<Simulering.IngenEndring>()
                utbetaling.beregning.totalBeløp shouldBe 8 * 298
                utbetaling.beregning.beregninger.single().dager
                    .filter { dag -> dag.dato.isAfter(5.mai(2025)) }
                    .map { dag -> dag.beløp }.distinct() shouldBe listOf(0)
                it.utbetalingskontroll.shouldNotBeNull()
            }

            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammevedtaksliste.innvilgetTidslinje.totalPeriode shouldBe
                (28.april(2025) til 30.juni(2025))
        }
    }

    @Test
    fun `annen åpen behandling på saken - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val revurdering = tac.opprettAutomatiskForlengelse(
                sak = sak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.copy(deltakelseTilOgMed = 30.juni(2025)),
            )
            startRevurderingInnvilgelse(tac, sak.id).shouldNotBeNull()

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING)
        }
    }

    @Test
    fun `manuelt opprettet innvilgelse - behandles ikke automatisk`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val (_, revurdering) = startRevurderingInnvilgelse(tac, sak.id)!!

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering) shouldBe revurdering
        }
    }
}
