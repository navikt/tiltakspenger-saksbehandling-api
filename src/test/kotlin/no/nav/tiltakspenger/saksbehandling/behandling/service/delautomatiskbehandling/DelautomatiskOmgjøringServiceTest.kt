package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.DevSimuleringsscenario
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.medPeriode
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test

/**
 * Sluttdatoen for deltakelsen avkortes, mens den fortsatt har status deltar.
 * Klokka står på 1. mai 2025.
 */
class DelautomatiskOmgjøringServiceTest {

    /**
     * Kjører [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb], som oppretter en omgjøring under automatisk behandling.
     */
    private suspend fun TestApplicationContextMedPostgres.opprettAutomatiskOmgjøring(
        sak: Sak,
        vedtak: Rammevedtak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern,
    ): Revurdering {
        behandleEndring(sak, tiltaksdeltakelse, nåtilstand).shouldBeInstanceOf<RevurderingOpprettet>()

        return sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>().also {
            it.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>().omgjørRammevedtak.rammevedtakIDer shouldBe
                listOf(vedtak.id)
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

    /**
     * Utbetaler første meldeperiode, 6.–17. januar 2025.
     */
    private suspend fun ApplicationTestBuilder.utbetalFørsteMeldeperiode(tac: TestApplicationContextMedPostgres, sak: Sak) {
        val (sakMedUtbetaling) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
        )!!
        sakMedUtbetaling.meldeperiodeBeregninger.sisteDagMedUtbetaling shouldBe 17.januar(2025)
    }

    private fun Revurdering.skalVæreOverlattTilSaksbehandler(vararg grunner: ManueltBehandlesGrunn) {
        status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
        saksbehandler.shouldBeNull()
        manueltBehandlesGrunner shouldBe grunner.toList()
        resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
    }

    @Test
    fun `sluttdato avkortet med status deltar - omgjøres, sendes til beslutning og kan iverksettes`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 30.juni(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val avkortet = deltakelse.medPeriode(tilOgMed = 15.mai(2025))
            val revurdering = tac.opprettAutomatiskOmgjøring(sak, vedtak, deltakelse, avkortet)

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                it.beslutter.shouldBeNull()
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.begrunnelseVilkårsvurdering.shouldNotBeNull().verdi shouldBe
                    "Automatisk behandlet.\n" +
                    "Sluttdatoen for tiltaksdeltakelsen er endret fra 30. juni 2025 til 15. mai 2025, og deltakelsen har fortsatt status deltar.\n" +
                    "Vedtaket omgjøres slik at tiltakspenger innvilges til og med ny sluttdato."
                it.skalSendeVedtaksbrev shouldBe true
                it.fritekstTilVedtaksbrev.shouldNotBeNull().verdi shouldBe
                    "Vi har fått opplysninger om at sluttdato for tiltaket er endret. Du er innvilget tiltakspenger ut perioden du deltar i tiltaket."
                val omgjøring = it.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringInnvilgelse>()
                omgjøring.omgjørRammevedtak.rammevedtakIDer shouldBe listOf(vedtak.id)
                omgjøring.vedtaksperiode shouldBe (5.januar(2025) til 30.juni(2025))
                omgjøring.innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (5.januar(2025) til 15.mai(2025))
                it.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct() shouldBe
                    vedtak.antallDagerPerMeldeperiode.shouldNotBeNull().verdier.distinct()
                it.barnetillegg.shouldNotBeNull().periodisering.totalPeriode shouldBe (5.januar(2025) til 15.mai(2025))
            }

            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.also {
                it.førsteDagSomGirRett shouldBe 5.januar(2025)
                it.sisteDagSomGirRett shouldBe 15.mai(2025)
            }
            tac.behandleEndring(sak, deltakelse, avkortet).shouldBeInstanceOf<IngenRelevantEndring>()
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    @Test
    fun `deltakelsen er avbrutt før omgjøringen behandles - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 30.juni(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val avkortet = deltakelse.medPeriode(tilOgMed = 15.mai(2025))
            val revurdering = tac.opprettAutomatiskOmgjøring(sak, vedtak, deltakelse, avkortet)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, avkortet.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt))

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering)
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.OMGJORING_DELTAKELSEN_HAR_IKKE_STATUS_DELTAR)
        }
    }

    @Test
    fun `startdato og deltakelsesmengde er endret før omgjøringen behandles - overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 30.juni(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            val avkortet = deltakelse.medPeriode(tilOgMed = 15.mai(2025))
            val revurdering = tac.opprettAutomatiskOmgjøring(sak, vedtak, deltakelse, avkortet)
            tac.oppdaterTiltaksdeltakelse(
                sak.fnr,
                avkortet.medPeriode(fraOgMed = 13.januar(2025)).copy(antallDagerPerUke = 2F, deltakelseProsent = 40F),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).skalVæreOverlattTilSaksbehandler(
                ManueltBehandlesGrunn.OMGJORING_ENDRET_STARTDATO,
                ManueltBehandlesGrunn.OMGJORING_ENDRET_DELTAKELSESMENGDE,
                ManueltBehandlesGrunn.OMGJORING_ANDRE_ENDRINGER_I_TILTAKSDELTAKELSENE,
            )
        }
    }

    /**
     * Første meldeperiode, 6.–17. januar, er utbetalt, og ny sluttdato er 10. januar.
     * Omgjøringen gir da feilutbetaling, som ingen saksbehandler har vurdert.
     */
    @Test
    fun `utbetalt etter ny sluttdato - feilutbetaling overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 31.mai(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            utbetalFørsteMeldeperiode(tac, sak)
            val revurdering = tac.opprettAutomatiskOmgjøring(
                sak = sak,
                vedtak = vedtak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 10.januar(2025)),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering)
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.OMGJORING_UTBETALING_STOTTER_IKKE_FEILUTBETALING)
        }
    }

    /**
     * Som over, men oppdrag motregner reduksjonen mot meldeperioder utenfor omgjøringen og svarer med justering i stedet for feilutbetaling.
     * Lokalt bestilles det med fødselsnummeret til [DevSimuleringsscenario.UBALANSERT_JUSTERING].
     */
    @Test
    fun `utbetalt etter ny sluttdato med justering - justeringen overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 31.mai(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                fnr = Fnr.fromString(DevSimuleringsscenario.UBALANSERT_JUSTERING.fnr),
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.lukketPeriode!!, deltakelse),
            )
            utbetalFørsteMeldeperiode(tac, sak)
            val revurdering = tac.opprettAutomatiskOmgjøring(
                sak = sak,
                vedtak = vedtak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 10.januar(2025)),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering)
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.OMGJORING_UTBETALING_STOTTER_IKKE_JUSTERING)
        }
    }

    /**
     * Som over, men revurderingen innvilger færre dager per meldeperiode, slik at innvilgelsesperiodene ikke slås sammen.
     * Innvilgelsesperioden fra revurderingen starter da etter ny sluttdato, og opphøres i sin helhet.
     */
    @Test
    fun `ny sluttdato i et tidligere vedtak med annet antall dager - innvilgelsesperioden etter ny sluttdato opphøres`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 30.juni(2025))
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(5.januar(2025) til 31.mars(2025), deltakelse),
            )
            val (_, revurderingsvedtak) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 30.juni(2025), deltakelse, AntallDagerForMeldeperiode(6)),
            )
            val revurdering = tac.opprettAutomatiskOmgjøring(
                sak = sak,
                vedtak = søknadsvedtak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 15.mars(2025)),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.manueltBehandlesGrunner.shouldBeEmpty()
                val omgjøring = it.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringInnvilgelse>()
                omgjøring.omgjørRammevedtak.rammevedtakIDer shouldBe listOf(søknadsvedtak.id, revurderingsvedtak.id)
                omgjøring.vedtaksperiode shouldBe (5.januar(2025) til 30.juni(2025))
                omgjøring.innvilgelsesperioder.shouldNotBeNull().perioder shouldBe listOf(5.januar(2025) til 15.mars(2025))
            }
        }
    }

    /**
     * Søknadsbehandlingen innvilger januar–mars, og en revurdering innvilger april–mai.
     * Første meldeperiode, 6.–17. januar, er utbetalt, og ny sluttdato, 10. januar, er i søknadsbehandlingen.
     * Omgjøringen av begge vedtakene gir feilutbetaling, som ingen saksbehandler har vurdert.
     */
    @Test
    fun `ny sluttdato i et tidligere vedtak og utbetalt etter ny sluttdato - feilutbetaling overlates til saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 31.mai(2025))
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(6.januar(2025) til 31.mars(2025), deltakelse),
            )
            iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025), deltakelse),
            )
            utbetalFørsteMeldeperiode(tac, sak)
            val revurdering = tac.opprettAutomatiskOmgjøring(
                sak = sak,
                vedtak = søknadsvedtak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 10.januar(2025)),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering)
                .skalVæreOverlattTilSaksbehandler(ManueltBehandlesGrunn.OMGJORING_UTBETALING_STOTTER_IKKE_FEILUTBETALING)
        }
    }

    /**
     * Søknadsbehandlingen innvilger januar–mars, og en revurdering innvilger april–mai.
     * Første meldeperiode, 6.–17. januar, er utbetalt, men ny sluttdato, 15. mars, er etter de utbetalte dagene.
     * Utbetalingen endres ikke, så omgjøringen av begge vedtakene sendes til beslutning.
     */
    @Test
    fun `ny sluttdato i et tidligere vedtak og utbetalt bare før ny sluttdato - sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(6.januar(2025) til 31.mai(2025))
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(6.januar(2025) til 31.mars(2025), deltakelse),
            )
            val (_, revurderingsvedtak) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025), deltakelse),
            )
            utbetalFørsteMeldeperiode(tac, sak)
            val revurdering = tac.opprettAutomatiskOmgjøring(
                sak = sak,
                vedtak = søknadsvedtak,
                tiltaksdeltakelse = deltakelse,
                nåtilstand = deltakelse.medPeriode(tilOgMed = 15.mars(2025)),
            )

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.manueltBehandlesGrunner.shouldBeEmpty()
                it.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringInnvilgelse>()
                    .omgjørRammevedtak.rammevedtakIDer shouldBe listOf(søknadsvedtak.id, revurderingsvedtak.id)
            }
            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.also {
                it.sisteDagSomGirRett shouldBe 15.mars(2025)
                it.meldeperiodeBeregninger.sisteDagMedUtbetaling shouldBe 17.januar(2025)
            }
        }
    }

    /**
     * Søknadsbehandlingen innvilger januar–mars, og en revurdering innvilger resten av deltakelsen, april–juni.
     * Ny sluttdato, 15. mars, er innvilget i søknadsbehandlingen, mens de avkortede dagene også er innvilget i revurderingen.
     * Omgjøringen startes fra søknadsbehandlingen, men vedtaksperioden dekker begge vedtakene.
     */
    @Test
    fun `ny sluttdato i et tidligere vedtak - omgjør både det tidligere og det senere vedtaket`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 30.juni(2025))
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(5.januar(2025) til 31.mars(2025), deltakelse),
            )
            val (_, revurderingsvedtak) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 30.juni(2025), deltakelse),
            )
            val avkortet = deltakelse.medPeriode(tilOgMed = 15.mars(2025))
            val revurdering = tac.opprettAutomatiskOmgjøring(sak, søknadsvedtak, deltakelse, avkortet)

            tac.delautomatiskRevurderingJobb.automatiskBehandleRevurdering(revurdering.id)

            tac.hentRevurdering(sak, revurdering).also {
                it.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                it.manueltBehandlesGrunner.shouldBeEmpty()
                val omgjøring = it.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringInnvilgelse>()
                omgjøring.omgjørRammevedtak.rammevedtakIDer shouldBe listOf(søknadsvedtak.id, revurderingsvedtak.id)
                omgjøring.vedtaksperiode shouldBe (5.januar(2025) til 30.juni(2025))
                omgjøring.innvilgelsesperioder.shouldNotBeNull().perioder shouldBe listOf(5.januar(2025) til 15.mars(2025))
                it.barnetillegg.shouldNotBeNull().periodisering.totalPeriode shouldBe (5.januar(2025) til 15.mars(2025))
            }

            taRammebehandlinger(tac, listOf(sak.id to revurdering.id), saksbehandler = ObjectMother.beslutter()).shouldNotBeNull()
            iverksettForBehandlingId(tac, sak.id, revurdering.id).shouldNotBeNull()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.also {
                it.førsteDagSomGirRett shouldBe 5.januar(2025)
                it.sisteDagSomGirRett shouldBe 15.mars(2025)
                it.rammevedtaksliste.innvilgetTidslinje.perioder shouldBe listOf(5.januar(2025) til 15.mars(2025))
            }
            tac.behandleEndring(sak, deltakelse, avkortet).shouldBeInstanceOf<IngenRelevantEndring>()
            tac.oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>().opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
        }
    }

    @Test
    fun `sluttdato før innvilgelsen starter gir stans som før`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 31.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(3.mars(2025) til 31.mai(2025), deltakelse),
            )

            tac.behandleEndring(sak, deltakelse, deltakelse.medPeriode(tilOgMed = 20.februar(2025)))
                .shouldBeInstanceOf<RevurderingOpprettet>()

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                .resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
        }
    }
}
