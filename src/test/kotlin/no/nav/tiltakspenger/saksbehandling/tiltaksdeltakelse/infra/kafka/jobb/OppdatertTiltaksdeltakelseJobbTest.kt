package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.gyldigFnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.oppgave.infra.OppgaveFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadPåSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFakeKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.IngenLesbarNåtilstand
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.IngenRelevantEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.OppgaveOpprettet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakelseEndringBehandlet.RevurderingOpprettet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import kotlin.reflect.KClass

class OppdatertTiltaksdeltakelseJobbTest {

    private suspend fun TestApplicationContextMedPostgres.registrerEndringOgBehandle(
        sak: Sak,
        tiltaksdeltakelse: TiltaksdeltakelseIntern,
        nåtilstand: TiltaksdeltakelseIntern? = tiltaksdeltakelse,
        forventetOppgavetekst: String? = null,
        forventetResultat: KClass<out TiltaksdeltakelseEndringBehandlet>,
    ): Sak {
        oppdaterTiltaksdeltakelse(sak.fnr, nåtilstand)
        tiltakContext.tiltaksdeltakerRepo.registrerUbehandletEndring(
            id = tiltaksdeltakelse.internDeltakelseId,
            sakId = sak.id,
            tidspunkt = nå(clock).minusMinutes(20),
        )
        val deltaker = tiltakContext.tiltaksdeltakerRepo
            .hentTiltaksdeltaker(tiltaksdeltakelse.eksternDeltakelseId).shouldNotBeNull()

        // Alle endringene i denne fila skal ferdigbehandles, slik at markøren kan nullstilles.
        val resultat = oppdatertTiltaksdeltakelseJobb.behandleDeltaker(deltaker).getOrFail()
        resultat::class shouldBe forventetResultat

        val oppgaver = oppgaveKlient.shouldBeInstanceOf<OppgaveFakeKlient>()
        val referanser = eksternOppgaveRepo.hentForSakId(sak.id)
        if (forventetOppgavetekst == null) {
            oppgaver.opprettedeOppgaverUtenDuplikatkontroll.shouldBeEmpty()
            referanser.shouldBeEmpty()
        } else {
            oppgaver.opprettedeOppgaverUtenDuplikatkontroll shouldBe listOf(sak.fnr to Oppgavebehov.ENDRET_TILTAKDELTAKER)
            oppgaver.opprettedeOppgavetekster shouldBe listOf(forventetOppgavetekst)
            referanser.map { it.oppgaveId } shouldBe oppgaver.opprettedeOppgaveIder
            referanser.map { it.tilleggstekst } shouldBe listOf(forventetOppgavetekst)
            referanser.single().grunnlag.shouldContainJsonKeyValue("$.verdi.eksternDeltakelseId", tiltaksdeltakelse.eksternDeltakelseId)
            resultat.shouldBeInstanceOf<OppgaveOpprettet>().oppgaveId shouldBe referanser.single().oppgaveId
        }

        val oppdatertSak = sakContext.sakRepo.hentForSakId(sak.id)!!
        if (resultat is RevurderingOpprettet) {
            resultat.revurderingId shouldBe oppdatertSak.rammebehandlinger.last().id
        }
        return oppdatertSak
    }

    /**
     * Stansen eller opphøret dekker slutten av deltakelsen i gjeldende vedtak, så saken har allerede tatt stilling til slutten.
     * En forlengelse er da ikke relevant, uavhengig av om det er rett igjen før stansen eller opphøret.
     */
    @ParameterizedTest
    @CsvSource(
        "false, 2025-03-31, 2025-03-01",
        "true, 2025-03-31, 2025-03-01",
        "false, 2025-05-05, 2025-04-01",
        "true, 2025-05-05, 2025-04-01",
        "false, 2025-05-05, 2025-05-01",
        "true, 2025-05-05, 2025-05-01",
        "false, 2025-05-05, 2025-05-02",
        "true, 2025-05-05, 2025-05-02",
        "false, 2025-12-31, 2025-07-01",
        "true, 2025-12-31, 2025-07-01",
    )
    fun `forlengelse etter stans eller opphør av slutten gir ingen revurdering`(
        opphør: Boolean,
        opprinneligSluttdato: LocalDate,
        bortfallFraOgMed: LocalDate,
    ) {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til opprinneligSluttdato)
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
            )
            if (opphør) {
                iverksettOmgjøringOpphør(
                    tac = tac,
                    sakId = sak.id,
                    rammevedtakIdSomOmgjøres = vedtak.id,
                    vedtaksperiode = bortfallFraOgMed til opprinneligSluttdato,
                )
            } else {
                iverksettRevurderingStans(tac = tac, sakId = sak.id, stansFraOgMed = bortfallFraOgMed)
            }
            val behandlingerFør = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger.map { it.id }

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseTilOgMed = opprinneligSluttdato.plusMonths(1)),
                forventetResultat = IngenRelevantEndring::class,
            )

            oppdatertSak.rammebehandlinger.map { it.id } shouldBe behandlingerFør
        }
    }

    @Test
    fun `utløpt innvilgelse uten stans eller opphør kan fortsatt forlenges`() {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 31.januar(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
            )

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseTilOgMed = 5.juni(2025)),
                forventetResultat = RevurderingOpprettet::class,
            )

            oppdatertSak.rammebehandlinger shouldHaveSize 2
            val revurdering = oppdatertSak.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
            revurdering.automatiskOpprettetGrunn.shouldNotBeNull().endring shouldBe
                TiltaksdeltakerEndring.Forlengelse(5.juni(2025))
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `endring vekker bare automatisk behandling på vent for den aktuelle deltakelsen`(nåtilstandMangler: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val tiltaksdeltakelse = tac.tiltaksdeltakelse(
                periode = 5.mai(2025) til 5.juni(2025),
                status = TiltakDeltakerstatus.VenterPåOppstart,
            )
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            val (_, _, annenBehandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                tac = tac,
                sakId = sak.id,
                tiltaksdeltakelse = tac.tiltaksdeltakelse(
                    periode = 6.mai(2025) til 6.juni(2025),
                    status = TiltakDeltakerstatus.VenterPåOppstart,
                ),
            )
            listOf(behandling, annenBehandling).forEach {
                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(it, CorrelationId.generate())
            }
            val repo = tac.behandlingContext.rammebehandlingRepo
            repo.hent(behandling.id).venterTil.shouldNotBeNull().toLocalDate() shouldBe 5.mai(2025)
            val annenVenterTil = repo.hent(annenBehandling.id).venterTil.shouldNotBeNull()
            val tidligsteVenterTil = nå(tac.clock).withNano(0).plusMinutes(OppdatertTiltaksdeltakelseJobb.MINUTTER_FORSINKELSE)

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = if (nåtilstandMangler) {
                    null
                } else {
                    tiltaksdeltakelse.copy(
                        deltakelseFraOgMed = 1.mai(2025),
                        deltakelseStatus = TiltakDeltakerstatus.Deltar,
                    )
                },
                forventetResultat = if (nåtilstandMangler) IngenLesbarNåtilstand::class else IngenRelevantEndring::class,
            )

            val senesteVenterTil = nå(tac.clock).plusSeconds(1).withNano(0)
                .plusMinutes(OppdatertTiltaksdeltakelseJobb.MINUTTER_FORSINKELSE)
            val oppdatertBehandling = repo.hent(behandling.id)
            val venterTil = oppdatertBehandling.venterTil.shouldNotBeNull()
            (venterTil in tidligsteVenterTil..senesteVenterTil) shouldBe true
            oppdatertBehandling.erUnderAutomatiskBehandling shouldBe true
            oppdatertBehandling.ventestatus.erSattPåVent shouldBe true
            repo.hent(annenBehandling.id).venterTil shouldBe annenVenterTil
            oppdatertSak.rammebehandlinger shouldHaveSize 2
        }
    }

    @Test
    fun `ingen endring i nå-tilstanden - endringen ferdigbehandles uten revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(sak, tiltaksdeltakelse, forventetResultat = IngenRelevantEndring::class)

            // Endringen tolkes ikke — den trigger kun et ferskt oppslag mot tiltakshistorikk på nåværende ekstern id.
            val fakeKlient = tac.tiltakContext.tiltaksdeltakelseKlient as TiltaksdeltakelseFakeKlient
            fakeKlient.hentTiltaksdeltakelseKall.get()
                .shouldContainExactly(fnr to tiltaksdeltakelse.eksternDeltakelseId)

            oppdatertSak.rammebehandlinger shouldHaveSize 1
        }
    }

    @Test
    fun `deltakelse avsluttet som forventet - ingen revurdering eller oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 30.april(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )

            val oppdatert = tac.registrerEndringOgBehandle(
                sak,
                deltakelse,
                deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Fullført),
                forventetResultat = TiltaksdeltakelseEndringBehandlet.AvsluttetSomForventet::class,
            )

            oppdatert.rammebehandlinger.map { it.id } shouldBe sak.rammebehandlinger.map { it.id }
        }
    }

    @Test
    fun `avbrutt deltakelse - oppretter stans-revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
                forventetResultat = RevurderingOpprettet::class,
            )

            oppdatertSak.rammebehandlinger shouldHaveSize 2

            val revurdering = oppdatertSak.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
            revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
            grunn.endring shouldBe TiltaksdeltakerEndring.AvbruttDeltakelse
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `forlenget deltakelse - oppretter innvilgelse-revurdering`(endretDeltakelsesmengde: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(
                    deltakelseFraOgMed = 5.januar(2025),
                    deltakelseTilOgMed = 5.juni(2025),
                    antallDagerPerUke = if (endretDeltakelsesmengde) 3F else tiltaksdeltakelse.antallDagerPerUke,
                ),
                forventetResultat = RevurderingOpprettet::class,
            )

            oppdatertSak.rammebehandlinger shouldHaveSize 2

            val revurdering = oppdatertSak.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
            val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
            grunn.endring shouldBe TiltaksdeltakerEndring.Forlengelse(
                nySluttdato = 5.juni(2025),
                endretDeltakelsesmengde = if (endretDeltakelsesmengde) {
                    TiltaksdeltakerEndring.EndretDeltakelsesmengde(tiltaksdeltakelse.deltakelseProsent, 3F)
                } else {
                    null
                },
            )
        }
    }

    @Test
    fun `endret startdato - oppretter omgjøring`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(
                    deltakelseFraOgMed = 6.januar(2025),
                ),
                forventetResultat = RevurderingOpprettet::class,
            )

            oppdatertSak.rammebehandlinger shouldHaveSize 2

            val revurdering = oppdatertSak.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            val grunn = revurdering.automatiskOpprettetGrunn.shouldNotBeNull()
            revurdering.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
                .omgjørRammevedtak.rammevedtakIDer shouldBe listOf(sak.rammevedtaksliste.single().id)
            grunn.endring shouldBe TiltaksdeltakerEndring.AndreEndringer(
                endretStartdato = TiltaksdeltakerEndring.EndretStartdato(6.januar(2025)),
            )
        }
    }

    @ParameterizedTest
    @CsvSource(
        "Venteliste, Endret status.",
        "IkkeAktuell, Deltakelsen er ikke aktuell.",
    )
    fun `endring som ikke gir automatisk revurdering oppretter Gosys-oppgave`(
        status: TiltakDeltakerstatus,
        oppgavetekst: String,
    ) {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                periode = 5.januar(2025) til 5.mai(2025),
                internDeltakelseId = TiltaksdeltakerId.random(),
            )
            val sak = iverksettSøknadsbehandling(
                tac = tac,
                fnr = fnr,
                innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.periode!!, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            ).first

            val oppdatertSak = tac.registrerEndringOgBehandle(
                sak,
                tiltaksdeltakelse,
                nåtilstand = tiltaksdeltakelse.copy(deltakelseStatus = status),
                forventetOppgavetekst = oppgavetekst,
                forventetResultat = OppgaveOpprettet::class,
            )

            oppdatertSak.rammebehandlinger shouldHaveSize 1
        }
    }

    @Test
    fun `flere endringer i åpen manuell behandling samles i én Gosys-oppgave`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
            )

            val oppdatert = tac.registrerEndringOgBehandle(
                sak,
                deltakelse,
                nåtilstand = deltakelse.copy(deltakelseFraOgMed = 6.januar(2025), antallDagerPerUke = 3F),
                forventetOppgavetekst = "- Endret deltakelsesmengde\n- Endret startdato",
                forventetResultat = OppgaveOpprettet::class,
            )

            oppdatert.rammebehandlinger.map { it.id } shouldBe listOf(behandling.id)
        }
    }

    @Test
    fun `søknad uten behandling - endringen kvitteres uten å opprette behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = opprettSakOgSøknad(tac, fnr = gyldigFnr(), tiltaksdeltakelse = deltakelse)

            val oppdatert = tac.registrerEndringOgBehandle(sak, deltakelse, deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)), forventetResultat = IngenRelevantEndring::class)

            oppdatert.rammebehandlinger.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `åpen behandling uten førstegangsvedtak - ingen revurdering eller endring av ventestatus`(automatisk: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak, _, behandling) = if (automatisk) {
                opprettSøknadsbehandlingUnderAutomatiskBehandling(tac, fnr = gyldigFnr(), tiltaksdeltakelse = deltakelse)
            } else {
                opprettSøknadsbehandlingUnderBehandling(tac, fnr = gyldigFnr(), tiltaksdeltakelse = deltakelse)
            }

            val oppdatert = tac.registrerEndringOgBehandle(
                sak,
                deltakelse,
                deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)),
                forventetOppgavetekst = if (automatisk) null else "Deltakelsen har blitt forlenget.",
                forventetResultat = if (automatisk) IngenRelevantEndring::class else OppgaveOpprettet::class,
            )

            oppdatert.rammebehandlinger.map { it.id } shouldBe listOf(behandling.id)
            oppdatert.rammebehandlinger.single().ventestatus shouldBe behandling.ventestatus
            oppdatert.rammebehandlinger.single().venterTil.shouldBeNull()
        }
    }

    @Test
    fun `åpen manuell revurdering på vent blokkerer ny revurdering og vekkes ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val (_, revurdering) = startRevurderingStans(tac, sak.id)!!
            settRammebehandlingPåVent(tac, sak.id, revurdering.id, frist = 5.juni(2025))
            val før = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)

            val oppdatert = tac.registrerEndringOgBehandle(
                sak,
                deltakelse,
                deltakelse.copy(deltakelseTilOgMed = 5.juni(2025)),
                forventetOppgavetekst = "Deltakelsen har blitt forlenget.",
                forventetResultat = OppgaveOpprettet::class,
            )

            oppdatert.rammebehandlinger shouldHaveSize 2
            tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id) shouldBe før
        }
    }

    @Test
    fun `vedtak for annen deltakelse - endringen ignoreres`() {
        withTestApplicationContextAndPostgres { tac ->
            val innvilget = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val annen = tac.tiltaksdeltakelse(6.mai(2025) til 5.juni(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = innvilget,
                innvilgelsesperioder = innvilgelsesperioder(innvilget.periode!!, innvilget),
            )
            opprettSøknadPåSakId(tac, sakId = sak.id, tiltaksdeltakelse = annen)

            val oppdatert = tac.registrerEndringOgBehandle(sak, annen, annen.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt), forventetResultat = IngenRelevantEndring::class)

            oppdatert.rammebehandlinger.map { it.id } shouldBe sak.rammebehandlinger.map { it.id }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `avslag for en annen deltakelse hindrer ikke stans av innvilgelsen`(avslagFørst: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val innvilget = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val avslått = tac.tiltaksdeltakelse(6.mai(2025) til 5.juni(2025))
            val første = if (avslagFørst) avslått else innvilget
            val andre = if (avslagFørst) innvilget else avslått
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = første,
                innvilgelsesperioder = innvilgelsesperioder(første.periode!!, første),
                resultat = if (avslagFørst) SøknadsbehandlingsresultatType.AVSLAG else SøknadsbehandlingsresultatType.INNVILGELSE,
            )
            val (sakMedBeggeVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                tiltaksdeltakelse = andre,
                innvilgelsesperioder = innvilgelsesperioder(andre.periode!!, andre),
                resultat = if (avslagFørst) SøknadsbehandlingsresultatType.INNVILGELSE else SøknadsbehandlingsresultatType.AVSLAG,
            )

            val etterAvslag = tac.registrerEndringOgBehandle(
                sakMedBeggeVedtak,
                avslått,
                avslått.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
                forventetResultat = IngenRelevantEndring::class,
            )
            etterAvslag.rammebehandlinger shouldHaveSize 2
            val etterInnvilgelse = tac.registrerEndringOgBehandle(
                etterAvslag,
                innvilget,
                innvilget.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
                forventetResultat = RevurderingOpprettet::class,
            )

            etterInnvilgelse.rammebehandlinger shouldHaveSize 3
            etterInnvilgelse.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                .resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()
        }
    }

    @Test
    fun `avbrudd når andre deltakelser gir rett fremover oppretter omgjøring av riktig vedtak`() {
        withTestApplicationContextAndPostgres { tac ->
            val første = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val andre = tac.tiltaksdeltakelse(6.mai(2025) til 5.juni(2025))
            val (sak, _, førsteVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = første,
                innvilgelsesperioder = innvilgelsesperioder(første.periode!!, første),
            )
            iverksettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                tiltaksdeltakelse = andre,
                innvilgelsesperioder = innvilgelsesperioder(andre.periode!!, andre),
            )

            val oppdatert = tac.registrerEndringOgBehandle(sak, første, første.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt), forventetResultat = RevurderingOpprettet::class)

            oppdatert.rammebehandlinger shouldHaveSize 3
            val revurdering = oppdatert.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            revurdering.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
                .omgjørRammevedtak.rammevedtakIDer shouldBe listOf(førsteVedtak.id)
            revurdering.automatiskOpprettetGrunn.shouldNotBeNull().endring shouldBe TiltaksdeltakerEndring.AvbruttDeltakelse
        }
    }

    @Test
    fun `avbrudd på utløpt innvilgelse uten rett fremover gir ingen revurdering`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 31.januar(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )

            val oppdatert = tac.registrerEndringOgBehandle(
                sak,
                deltakelse,
                deltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt),
                forventetOppgavetekst = "Deltakelsen er avbrutt.",
                forventetResultat = OppgaveOpprettet::class,
            )

            oppdatert.rammebehandlinger shouldHaveSize 1
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `allerede iverksatt forlengelse gir ingen duplikat og flere vedtak omgjøres ikke automatisk`(endreMengde: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )
            val forlenget = deltakelse.copy(deltakelseTilOgMed = 5.juni(2025))
            tac.oppdaterTiltaksdeltakelse(sak.fnr, forlenget)
            val (sakMedForlengelse) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(6.mai(2025) til 5.juni(2025), forlenget),
            )

            val oppdatert = tac.registrerEndringOgBehandle(
                sakMedForlengelse,
                deltakelse,
                if (endreMengde) forlenget.copy(antallDagerPerUke = 3F) else forlenget,
                forventetOppgavetekst = if (endreMengde) "Endret deltakelsesmengde." else null,
                forventetResultat = if (endreMengde) OppgaveOpprettet::class else IngenRelevantEndring::class,
            )

            oppdatert.rammebehandlinger.map { it.id } shouldBe sakMedForlengelse.rammebehandlinger.map { it.id }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `forlengelse innenfor sakens siste rett kan fortsatt kreve omgjøring ved endret mengde`(endreMengde: Boolean) {
        withTestApplicationContextAndPostgres { tac ->
            val første = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val andre = tac.tiltaksdeltakelse(10.mai(2025) til 5.juni(2025))
            val (sak, _, førsteVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = første,
                innvilgelsesperioder = innvilgelsesperioder(første.periode!!, første),
            )
            val (sakMedBeggeVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                tiltaksdeltakelse = andre,
                innvilgelsesperioder = innvilgelsesperioder(andre.periode!!, andre),
            )
            val oppdatert = tac.registrerEndringOgBehandle(
                sakMedBeggeVedtak,
                første,
                første.copy(deltakelseTilOgMed = 9.mai(2025), antallDagerPerUke = if (endreMengde) 3F else første.antallDagerPerUke),
                forventetOppgavetekst = if (endreMengde) null else "Deltakelsen har blitt forlenget.",
                forventetResultat = if (endreMengde) RevurderingOpprettet::class else OppgaveOpprettet::class,
            )

            if (endreMengde) {
                oppdatert.rammebehandlinger shouldHaveSize 3
                val revurdering = oppdatert.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
                revurdering.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
                    .omgjørRammevedtak.rammevedtakIDer shouldBe listOf(førsteVedtak.id)
                revurdering.automatiskOpprettetGrunn.shouldNotBeNull().endring shouldBe TiltaksdeltakerEndring.Forlengelse(
                    nySluttdato = 9.mai(2025),
                    endretDeltakelsesmengde = TiltaksdeltakerEndring.EndretDeltakelsesmengde(første.deltakelseProsent, 3F),
                )
            } else {
                oppdatert.rammebehandlinger.map { it.id } shouldBe sakMedBeggeVedtak.rammebehandlinger.map { it.id }
            }
        }
    }

    @Test
    fun `avkortet sluttdato i fremtiden oppretter omgjøring og ikke stans`() {
        withTestApplicationContextAndPostgres { tac ->
            val deltakelse = tac.tiltaksdeltakelse(5.januar(2025) til 5.mai(2025))
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(deltakelse.periode!!, deltakelse),
            )

            val oppdatert = tac.registrerEndringOgBehandle(sak, deltakelse, deltakelse.copy(deltakelseTilOgMed = 3.mai(2025)), forventetResultat = RevurderingOpprettet::class)

            oppdatert.rammebehandlinger shouldHaveSize 2
            val revurdering = oppdatert.rammebehandlinger.last().shouldBeInstanceOf<Revurdering>()
            revurdering.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
                .omgjørRammevedtak.rammevedtakIDer shouldBe listOf(vedtak.id)
            revurdering.automatiskOpprettetGrunn.shouldNotBeNull().endring shouldBe TiltaksdeltakerEndring.AndreEndringer(
                endretSluttdato = TiltaksdeltakerEndring.EndretSluttdato(3.mai(2025)),
            )
        }
    }
}
