package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkAntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingResultat
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSortering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringRetning
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadstype
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingKilde
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentBenkKommando
import no.nav.tiltakspenger.saksbehandling.benk.domene.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort.Companion.MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettKlagebehandlingTilVurdering
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgMottaOppretholdtKlagebehandlingFraKa
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgSettPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedAvslag
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilGodkjenning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settMeldekortbehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tildelTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjenn
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.konsumerTilbakekrevingshendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Aggregat-test for benk v2, jf. testtaksonomien i `AGENTS.md`.
 *
 * Benken er nettopp en spørring på tvers av alle saker, så totalantallet er featuren og ikke en krykke.
 * Derfor kjører hver test isolert, og derfor kalles `hent*(limit)` direkte.
 *
 * Tilstanden bygges gjennom prodstiene: routene, konsumentene og jobbene, slik de kjører i nais.
 */
class BenkAggregatTest {

    private fun <F : BenkFiltrering, K : BenkSorteringKolonne> command(
        filtrering: F,
        kolonne: K,
        retning: BenkSorteringRetning = BenkSorteringRetning.ASC,
    ) = HentBenkKommando(
        filtrering = filtrering,
        sortering = BenkSortering(kolonne, retning),
        saksbehandler = ObjectMother.saksbehandler(),
        correlationId = CorrelationId.generate(),
    )

    private fun søknaderCommand(
        status: BenkBehandlingsstatus? = null,
        søknadstype: BenkSøknadstype? = null,
        resultat: BenkSøknadsbehandlingResultat? = null,
        saksbehandler: String? = null,
        kolonne: BenkSøknaderKolonne = BenkSøknaderKolonne.KRAVTIDSPUNKT,
        retning: BenkSorteringRetning = BenkSorteringRetning.ASC,
        skjulPåVent: Boolean = false,
    ) = command(BenkSøknaderFiltrering(status, søknadstype, resultat, saksbehandler, skjulPåVent), kolonne, retning)

    private fun revurderingerCommand(
        status: BenkBehandlingsstatus? = null,
        resultat: BenkRevurderingResultat? = null,
        saksbehandler: String? = null,
        skjulPåVent: Boolean = false,
    ) = command(BenkRevurderingerFiltrering(status, resultat, saksbehandler, skjulPåVent), BenkRevurderingerKolonne.STARTET)

    private fun meldekortCommand(
        status: BenkBehandlingsstatus? = null,
        type: BenkMeldekortType? = null,
        saksbehandler: String? = null,
        skjulPåVent: Boolean = false,
    ) = command(BenkMeldekortFiltrering(status, type, saksbehandler, skjulPåVent), BenkMeldekortKolonne.PERIODE)

    private fun klageCommand(
        status: BenkBehandlingsstatus? = null,
        resultat: BenkKlagebehandlingResultat? = null,
        saksbehandler: String? = null,
        skjulPåVent: Boolean = false,
    ) = command(BenkKlageFiltrering(status, resultat, saksbehandler, skjulPåVent), BenkKlageKolonne.KRAVTIDSPUNKT)

    private fun tilbakekrevingCommand(
        status: BenkTilbakekrevingStatus? = null,
        kilde: BenkTilbakekrevingKilde? = null,
        saksbehandler: String? = null,
        minstebeløp: Long = 0,
        skjulPåVent: Boolean = false,
        kolonne: BenkTilbakekrevingKolonne = BenkTilbakekrevingKolonne.STARTET,
    ) = command(
        BenkTilbakekrevingFiltrering(status, kilde, saksbehandler, minstebeløp, skjulPåVent),
        kolonne,
    )

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen viser åpne søknadsbehandlinger, men ikke søknader uten behandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            // Søknader som ingen har tatt tak i, er ikke behandlinger og skal ikke ligge på benken.
            opprettSakOgSøknad(tac = tac)
            val (sakUnderBehandling, søknad, underBehandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac)
            // Iverksatte behandlinger er ferdige, og skal ikke ligge på benken.
            iverksettSøknadsbehandling(tac = tac)

            val oversikt = tac.benkContext.benkRepo.hentSøknader(søknaderCommand())

            oversikt.totalAntall shouldBe 1
            oversikt.totalAntallUfiltrert shouldBe 1

            val rad = oversikt.behandlinger.single()
            rad.id shouldBe underBehandling.id
            rad.felles.sakId shouldBe sakUnderBehandling.id
            rad.felles.fnr shouldBe søknad.fnr
            rad.felles.saksnummer shouldBe søknad.saksnummer
            rad.felles.startet shouldBe underBehandling.opprettet
            rad.felles.sistEndret shouldBe underBehandling.sistEndret
            rad.felles.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
            rad.felles.beslutter shouldBe null
            rad.felles.erUnderkjent shouldBe false
            rad.felles.ventestatus.erSattPåVent shouldBe false
            rad.felles.ventestatus.begrunnelse shouldBe null
            rad.felles.ventestatus.frist shouldBe null
            rad.status shouldBe BenkBehandlingsstatus.UNDER_BEHANDLING
            rad.søknadstype shouldBe BenkSøknadstype.DIGITAL
            rad.kravtidspunkt shouldBe søknad.opprettet
            rad.resultat shouldBe BenkSøknadsbehandlingResultat.INNVILGELSE
            // Kommandoene på raden er de samme reglene som på selve behandlingen — dette pinner speilingen.
            rad.finnGyldigeKommandoer(ObjectMother.saksbehandler()) shouldBe
                underBehandling.finnGyldigeKommandoer(ObjectMother.saksbehandler())
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen viser underkjent og ventestatus`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUnderkjent) = underkjenn(tac = tac)
            val (sakPåVent) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            val oversikt = tac.benkContext.benkRepo.hentSøknader(søknaderCommand())

            oversikt.behandlinger.single { it.felles.sakId == sakUnderkjent.id }.felles.erUnderkjent shouldBe true
            oversikt.behandlinger.single { it.felles.sakId == sakPåVent.id }.felles.ventestatus.let {
                it.erSattPåVent shouldBe true
                (it.begrunnelse != null) shouldBe true
                (it.frist != null) shouldBe true
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `skjulPåVent tar bort behandlingene som er satt på vent`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac)
            val (sakPåVent) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            val repo = tac.benkContext.benkRepo

            repo.hentSøknader(søknaderCommand()).totalAntall shouldBe 2

            val filtrert = repo.hentSøknader(søknaderCommand(skjulPåVent = true))

            filtrert.totalAntall shouldBe 1
            filtrert.totalAntallUfiltrert shouldBe 2
            filtrert.behandlinger.none { it.felles.sakId == sakPåVent.id } shouldBe true
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen filtrerer på status, søknadstype, resultat og saksbehandler`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakKlarTilBehandling) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            val (sakUnderBehandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac)
            val repo = tac.benkContext.benkRepo

            repo.hentSøknader(søknaderCommand(status = BenkBehandlingsstatus.UNDER_BEHANDLING)).let {
                it.totalAntall shouldBe 1
                it.totalAntallUfiltrert shouldBe 2
                it.behandlinger.single().felles.sakId shouldBe sakUnderBehandling.id
            }
            repo.hentSøknader(søknaderCommand(søknadstype = BenkSøknadstype.DIGITAL)).totalAntall shouldBe 2
            repo.hentSøknader(søknaderCommand(søknadstype = BenkSøknadstype.PAPIR_SKJEMA)).let {
                it.totalAntall shouldBe 0
                it.totalAntallUfiltrert shouldBe 2
                it.behandlinger shouldBe emptyList()
            }
            repo.hentSøknader(søknaderCommand(resultat = BenkSøknadsbehandlingResultat.INNVILGELSE)).let {
                it.totalAntall shouldBe 1
                it.behandlinger.single().felles.sakId shouldBe sakUnderBehandling.id
            }
            repo.hentSøknader(søknaderCommand(resultat = BenkSøknadsbehandlingResultat.AVSLAG)).totalAntall shouldBe 0
            repo.hentSøknader(søknaderCommand(resultat = BenkSøknadsbehandlingResultat.IKKE_VALGT)).let {
                it.totalAntall shouldBe 1
                it.behandlinger.single().felles.sakId shouldBe sakKlarTilBehandling.id
            }
            repo.hentSøknader(søknaderCommand(saksbehandler = ObjectMother.saksbehandler().navIdent)).let {
                it.totalAntall shouldBe 1
                it.behandlinger.single().felles.sakId shouldBe sakUnderBehandling.id
            }
            repo.hentSøknader(søknaderCommand(saksbehandler = BenkFiltrering.IKKE_TILDELT)).let {
                it.totalAntall shouldBe 1
                it.behandlinger.single().felles.sakId shouldBe sakKlarTilBehandling.id
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen filtrerer på identen som beslutter også`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val saksbehandlerOgBeslutter = ObjectMother.saksbehandlerOgBeslutter("Z999999")
            // Identen er saksbehandler på den første behandlingen og beslutter på den andre — filteret treffer begge.
            opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac, saksbehandler = saksbehandlerOgBeslutter)
            val (sakUnderBeslutning, _, underBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taBehandling(tac, sakUnderBeslutning.id, underBeslutningId, saksbehandlerOgBeslutter)

            val oversikt = tac.benkContext.benkRepo.hentSøknader(
                søknaderCommand(saksbehandler = saksbehandlerOgBeslutter.navIdent),
            )

            oversikt.totalAntall shouldBe 2
            oversikt.behandlinger.single { it.felles.sakId == sakUnderBeslutning.id }.let {
                it.felles.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                it.felles.beslutter shouldBe saksbehandlerOgBeslutter.navIdent
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen sorterer stigende og synkende, og respekterer limit`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            // Behandlingene opprettes med samme tidsstempel, så fnr er den eneste kolonnen sorteringen kan observeres på her.
            val fnrEn = Fnr.fromString("01019012345")
            val fnrTo = Fnr.fromString("02019012345")
            opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac, fnr = fnrTo)
            opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac, fnr = fnrEn)
            val repo = tac.benkContext.benkRepo

            fun kommando(retning: BenkSorteringRetning) =
                søknaderCommand(kolonne = BenkSøknaderKolonne.FNR, retning = retning)

            repo.hentSøknader(kommando(BenkSorteringRetning.ASC)).behandlinger.map { it.felles.fnr } shouldBe
                listOf(fnrEn, fnrTo)
            repo.hentSøknader(kommando(BenkSorteringRetning.DESC)).behandlinger.map { it.felles.fnr } shouldBe
                listOf(fnrTo, fnrEn)
            repo.hentSøknader(kommando(BenkSorteringRetning.ASC), limit = 1).let {
                // Limit kutter radene, men ikke tellingene: benken skal kunne si at den viser én av to.
                it.behandlinger.size shouldBe 1
                it.totalAntall shouldBe 2
            }
            repo.hentSøknader(søknaderCommand()).behandlinger.size shouldBe 2
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen sorterer på ventestatusfrist, med rader uten frist sist uansett retning`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakSent) = opprettSøknadsbehandlingOgSettPåVent(tac = tac, frist = 10.januar(2026))!!
            val (sakTidlig) = opprettSøknadsbehandlingOgSettPåVent(tac = tac, frist = 5.januar(2026))!!
            val (sakIkkePåVent) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            val repo = tac.benkContext.benkRepo

            fun sorterteSaker(retning: BenkSorteringRetning) =
                repo.hentSøknader(søknaderCommand(kolonne = BenkSøknaderKolonne.VENTESTATUS_FRIST, retning = retning))
                    .behandlinger.map { it.felles.sakId }

            sorterteSaker(BenkSorteringRetning.ASC) shouldBe listOf(sakTidlig.id, sakSent.id, sakIkkePåVent.id)
            sorterteSaker(BenkSorteringRetning.DESC) shouldBe listOf(sakSent.id, sakTidlig.id, sakIkkePåVent.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen sorterer på sist endret`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            // Den tikkende klokka gjør at den andre saken får et senere sist_endret enn den første.
            val (sakFørst) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!
            val (sakSist) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!
            val repo = tac.benkContext.benkRepo

            fun sorterteSaker(retning: BenkSorteringRetning) =
                repo.hentSøknader(søknaderCommand(kolonne = BenkSøknaderKolonne.SIST_ENDRET, retning = retning))
                    .behandlinger.map { it.felles.sakId }

            sorterteSaker(BenkSorteringRetning.ASC) shouldBe listOf(sakFørst.id, sakSist.id)
            sorterteSaker(BenkSorteringRetning.DESC) shouldBe listOf(sakSist.id, sakFørst.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknadsfanen sorterer på resultat, der ikke-valgt er en vanlig verdi`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakAvslag) = opprettSøknadsbehandlingUnderBehandlingMedAvslag(tac = tac)
            val (sakInnvilgelse) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac)
            val (sakIkkeValgt) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            val repo = tac.benkContext.benkRepo

            fun sorterteSaker(retning: BenkSorteringRetning) =
                repo.hentSøknader(søknaderCommand(kolonne = BenkSøknaderKolonne.RESULTAT, retning = retning))
                    .behandlinger.map { it.felles.sakId }

            sorterteSaker(BenkSorteringRetning.ASC) shouldBe listOf(sakAvslag.id, sakIkkeValgt.id, sakInnvilgelse.id)
            sorterteSaker(BenkSorteringRetning.DESC) shouldBe listOf(sakInnvilgelse.id, sakIkkeValgt.id, sakAvslag.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `revurderingsfanen viser åpne revurderinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            oppdaterRevurderingStans(tac = tac, sakId = sak.id, behandlingId = revurdering.id)
            val repo = tac.benkContext.benkRepo

            val oversikt = repo.hentRevurderinger(revurderingerCommand())

            oversikt.totalAntall shouldBe 1
            oversikt.behandlinger.single().let {
                it.felles.sakId shouldBe sak.id
                it.felles.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                it.id shouldBe revurdering.id
                it.status shouldBe BenkBehandlingsstatus.UNDER_BEHANDLING
                it.resultat shouldBe BenkRevurderingResultat.STANS
                it.finnGyldigeKommandoer(ObjectMother.saksbehandler()) shouldBe
                    revurdering.finnGyldigeKommandoer(ObjectMother.saksbehandler())
            }

            repo.hentRevurderinger(revurderingerCommand(resultat = BenkRevurderingResultat.STANS)).totalAntall shouldBe 1
            repo.hentRevurderinger(
                revurderingerCommand(resultat = BenkRevurderingResultat.OMGJØRING),
            ).totalAntall shouldBe 0
            repo.hentRevurderinger(
                revurderingerCommand(status = BenkBehandlingsstatus.KLAR_TIL_BESLUTNING),
            ).totalAntall shouldBe 0
            repo.hentRevurderinger(
                revurderingerCommand(saksbehandler = BenkFiltrering.IKKE_TILDELT),
            ).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser meldekortbehandlinger med beregnet beløp og periode`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUtenBeregning, _, _, meldekortbehandling) =
                iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            val (sakMedBeregning) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac = tac)!!
            val repo = tac.benkContext.benkRepo

            val oversikt = repo.hentMeldekort(meldekortCommand())

            oversikt.totalAntall shouldBe 2
            oversikt.behandlinger.single { it.felles.sakId == sakUtenBeregning.id }.let {
                it.id shouldBe meldekortbehandling.id
                it.type shouldBe BenkMeldekortType.MELDEKORTBEHANDLING
                it.status shouldBe BenkBehandlingsstatus.UNDER_BEHANDLING
                it.meldeperioder shouldBe listOf(meldekortbehandling.periode)
                it.beløp shouldBe null
                it.felles.saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                it.finnGyldigeKommandoer(ObjectMother.saksbehandler()) shouldBe
                    meldekortbehandling.finnGyldigeKommandoer(ObjectMother.saksbehandler())
            }
            oversikt.behandlinger.single { it.felles.sakId == sakMedBeregning.id }.let {
                it.status shouldBe BenkBehandlingsstatus.KLAR_TIL_BESLUTNING
                (it.beløp!! > 0) shouldBe true
            }

            repo.hentMeldekort(meldekortCommand(type = BenkMeldekortType.MELDEKORTBEHANDLING)).totalAntall shouldBe 2
            repo.hentMeldekort(meldekortCommand(type = BenkMeldekortType.INNSENDT_MELDEKORT)).totalAntall shouldBe 0
            repo.hentMeldekort(
                meldekortCommand(status = BenkBehandlingsstatus.KLAR_TIL_BESLUTNING),
            ).totalAntall shouldBe 1
            repo.hentMeldekort(meldekortCommand(saksbehandler = BenkFiltrering.IKKE_TILDELT)).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser alle meldeperiodene en behandling dekker, i kronologisk rekkefølge`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val kjeder = sak.meldeperiodeKjeder.take(2)

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                // Sender i omvendt rekkefølge for å verifisere at benken sorterer dem.
                kjedeIder = kjeder.reversed().map { it.kjedeId },
            )!!

            val oversikt = tac.benkContext.benkRepo.hentMeldekort(meldekortCommand())

            oversikt.behandlinger.single().meldeperioder shouldBe kjeder.map { it.periode }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser innsendte og korrigerte meldekort som venter på behandling`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            // Første kort i kjeden behandles automatisk; korrigeringen etterpå faller til manuell behandling og havner på benken.
            mottaAutomatiskMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()
            val (_, korrigering) = mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            val (_, innsendt) = mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 1)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            val repo = tac.benkContext.benkRepo

            val oversikt = repo.hentMeldekort(meldekortCommand())

            oversikt.behandlinger.map { it.type } shouldBe listOf(
                BenkMeldekortType.KORRIGERT_MELDEKORT,
                BenkMeldekortType.INNSENDT_MELDEKORT,
            )
            oversikt.behandlinger.first().let {
                it.id shouldBe korrigering.id
                it.felles.startet shouldBe korrigering.mottatt
                it.felles.sistEndret shouldBe korrigering.mottatt
                it.meldeperioder shouldBe listOf(sak.meldeperiodeKjeder[0].periode)
                it.beløp shouldBe null
                it.status shouldBe BenkBehandlingsstatus.KLAR_TIL_BEHANDLING
                it.felles.saksbehandler shouldBe null
            }
            oversikt.behandlinger.last().let {
                it.felles.sistEndret shouldBe innsendt.mottatt
                it.meldeperioder shouldBe listOf(sak.meldeperiodeKjeder[1].periode)
            }
            // Meldekort som venter på at noen starter en behandling, har ingen behandling å utføre kommandoer på.
            oversikt.behandlinger.forEach {
                it.finnGyldigeKommandoer(ObjectMother.saksbehandler()) shouldBe emptyList()
            }

            repo.hentMeldekort(meldekortCommand(type = BenkMeldekortType.KORRIGERT_MELDEKORT)).totalAntall shouldBe 1
            repo.hentMeldekort(meldekortCommand(saksbehandler = BenkFiltrering.IKKE_TILDELT)).totalAntall shouldBe 2
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser ventestatus, og skjulPåVent tar bort raden`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val frist = LocalDate.now(tac.clock).plusWeeks(1)
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = ObjectMother.saksbehandler(),
                begrunnelse = "Venter på dokumentasjon",
                frist = frist,
            )
            val repo = tac.benkContext.benkRepo

            repo.hentMeldekort(meldekortCommand()).behandlinger.single().felles.ventestatus.let {
                it.erSattPåVent shouldBe true
                it.begrunnelse shouldBe "Venter på dokumentasjon"
                it.frist shouldBe frist
            }

            repo.hentMeldekort(meldekortCommand(skjulPåVent = true)).let {
                it.totalAntall shouldBe 0
                it.totalAntallUfiltrert shouldBe 1
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser ikke innsendt meldekort når en nyere behandling finnes for kjeden`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val (_, innsendt) = mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            val repo = tac.benkContext.benkRepo

            repo.hentMeldekort(meldekortCommand()).behandlinger.single().type shouldBe BenkMeldekortType.INNSENDT_MELDEKORT

            // Behandlingen er nyere enn innsendingen, så meldekortet er tatt stilling til og faller bort fra benken.
            opprettOgIverksettMeldekortbehandling(tac = tac, sakId = sak.id, kjedeId = innsendt.kjedeId)!!

            repo.hentMeldekort(meldekortCommand()).totalAntall shouldBe 0

            // Korrigeringen er mottatt etter iverksettingen, og er derfor ikke dekket av behandlingen.
            mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            repo.hentMeldekort(meldekortCommand()).behandlinger.single().type shouldBe BenkMeldekortType.KORRIGERT_MELDEKORT

            opprettOgIverksettMeldekortbehandling(tac = tac, sakId = sak.id, kjedeId = innsendt.kjedeId)!!

            repo.hentMeldekort(meldekortCommand()).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser ikke meldekort der behandlingen er avbrutt i ettertid`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val (_, innsendt) = mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            val repo = tac.benkContext.benkRepo

            repo.hentMeldekort(meldekortCommand()).behandlinger.single().type shouldBe BenkMeldekortType.INNSENDT_MELDEKORT

            val (_, behandling, _) = opprettMeldekortbehandlingForSakId(tac = tac, sakId = sak.id, kjedeId = innsendt.kjedeId)!!

            repo.hentMeldekort(meldekortCommand()).behandlinger.single().type shouldBe BenkMeldekortType.MELDEKORTBEHANDLING

            // Den avbrutte behandlingen er fortsatt nyere enn innsendingen, så meldekortet kommer ikke tilbake på benken.
            avbrytMeldekortbehandling(tac = tac, sakId = sak.id, meldekortId = behandling.id)!!

            repo.hentMeldekort(meldekortCommand()).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `meldekortfanen viser korrigering inntil den åpne behandlingen er oppdatert`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val (_, innsendt) = mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            val (_, behandling, _) = opprettMeldekortbehandlingForSakId(tac = tac, sakId = sak.id, kjedeId = innsendt.kjedeId)!!
            val repo = tac.benkContext.benkRepo

            repo.hentMeldekort(meldekortCommand()).behandlinger.single().type shouldBe BenkMeldekortType.MELDEKORTBEHANDLING

            // Korrigeringen er mottatt etter at behandlingen sist ble endret, så begge radene vises.
            mottaManueltMeldekortForKjede(tac, sak, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            // Rekkefølgen mellom de to radene er udefinert — begge dekker samme kjede, og sekundærsorteringen på sak skiller dem ikke.
            repo.hentMeldekort(meldekortCommand()).let {
                it.totalAntall shouldBe 2
                it.behandlinger.map { rad -> rad.type }.toSet() shouldBe
                    setOf(BenkMeldekortType.MELDEKORTBEHANDLING, BenkMeldekortType.KORRIGERT_MELDEKORT)
            }

            // Når behandlingen oppdateres er den nyere enn korrigeringen, og korrigeringen faller bort.
            oppdaterMeldekortbehandling(tac = tac, sakId = sak.id, meldekortId = behandling.id)!!

            repo.hentMeldekort(meldekortCommand()).behandlinger.single().type shouldBe BenkMeldekortType.MELDEKORTBEHANDLING
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `klagefanen viser åpne klagebehandlinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sakAvvist, klageAvvist) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!
            val (sakMedResultat, _, klageMedResultat) = opprettSakOgKlagebehandlingTilOpprettholdelse(tac = tac)!!
            val (sakTilVurdering, _, _, klageTilVurdering) =
                iverksettSøknadsbehandlingOgOpprettKlagebehandlingTilVurdering(tac = tac)!!
            val repo = tac.benkContext.benkRepo

            val oversikt = repo.hentKlager(klageCommand())

            oversikt.totalAntall shouldBe 3
            oversikt.behandlinger.single { it.felles.sakId == sakAvvist.id }.let {
                it.id shouldBe klageAvvist.id
                it.felles.saksbehandler shouldBe saksbehandler.navIdent
                it.felles.beslutter shouldBe null
                it.felles.erUnderkjent shouldBe false
                it.status shouldBe BenkBehandlingsstatus.UNDER_BEHANDLING
                it.resultat shouldBe BenkKlagebehandlingResultat.AVVIST
                it.felles.startet shouldBe klageAvvist.opprettet
            }
            oversikt.behandlinger.single { it.felles.sakId == sakMedResultat.id }.let {
                it.resultat shouldBe BenkKlagebehandlingResultat.OPPRETTHOLDT
                it.felles.sistEndret shouldBe klageMedResultat.sistEndret
            }
            // En klage med oppfylte formkrav som ikke er vurdert ennå, har ikke noe resultat.
            oversikt.behandlinger.single { it.felles.sakId == sakTilVurdering.id }.let {
                it.id shouldBe klageTilVurdering.id
                it.resultat shouldBe null
            }

            repo.hentKlager(klageCommand(resultat = BenkKlagebehandlingResultat.OPPRETTHOLDT)).totalAntall shouldBe 1
            repo.hentKlager(klageCommand(resultat = BenkKlagebehandlingResultat.OMGJØR)).totalAntall shouldBe 0
            repo.hentKlager(klageCommand(status = BenkBehandlingsstatus.UNDER_BEHANDLING)).totalAntall shouldBe 3
            repo.hentKlager(klageCommand(saksbehandler = saksbehandler.navIdent)).totalAntall shouldBe 3
            repo.hentKlager(klageCommand(saksbehandler = BenkFiltrering.IKKE_TILDELT)).totalAntall shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `klagefanen viser ventestatus, og skjulPåVent tar bort raden`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klage) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!
            settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klage.id,
                begrunnelse = "Venter på svar fra bruker",
                frist = 13.februar(2026),
            )!!
            val repo = tac.benkContext.benkRepo

            repo.hentKlager(klageCommand()).behandlinger.single().felles.ventestatus.let {
                it.erSattPåVent shouldBe true
                it.begrunnelse shouldBe "Venter på svar fra bruker"
                it.frist shouldBe 13.februar(2026)
            }

            repo.hentKlager(klageCommand(skjulPåVent = true)).let {
                it.totalAntall shouldBe 0
                it.totalAntallUfiltrert shouldBe 1
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `klagefanen viser klage mottatt fra klageinstans som klar til ferdigstilling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, mottattFraKa, _) = opprettSakOgMottaOppretholdtKlagebehandlingFraKa(tac = tac)!!

            val oversikt = tac.benkContext.benkRepo.hentKlager(klageCommand())

            oversikt.behandlinger.single().let {
                it.felles.sakId shouldBe sak.id
                it.id shouldBe mottattFraKa.id
                it.status shouldBe BenkBehandlingsstatus.KLAR_TIL_FERDIGSTILLING
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `tilbakekrevingsfanen viser åpne tilbakekrevinger med utledet status`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakTilBehandling, tilBehandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val (sakTilGodkjenning) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)
            val repo = tac.benkContext.benkRepo

            val oversikt = repo.hentTilbakekrevinger(tilbakekrevingCommand())

            oversikt.totalAntall shouldBe 2
            oversikt.behandlinger.single { it.felles.sakId == sakTilBehandling.id }.let {
                it.id shouldBe tilBehandling.id
                it.status shouldBe BenkTilbakekrevingStatus.TIL_BEHANDLING
                it.kilde shouldBe BenkTilbakekrevingKilde.MELDEKORT
                it.beløp shouldBe tilBehandling.totaltFeilutbetaltBeløp
                it.kravgrunnlagPeriode shouldBe tilBehandling.kravgrunnlagTotalPeriode
                it.felles.saksbehandler shouldBe null
                it.felles.beslutter shouldBe null
                it.felles.erUnderkjent shouldBe false
                it.felles.ventestatus.erSattPåVent shouldBe false
                it.finnGyldigeKommandoer(ObjectMother.saksbehandler()) shouldBe
                    tilBehandling.gyldigeKommandoer(ObjectMother.saksbehandler())
            }
            oversikt.behandlinger.single { it.felles.sakId == sakTilGodkjenning.id }.status shouldBe
                BenkTilbakekrevingStatus.TIL_GODKJENNING

            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(status = BenkTilbakekrevingStatus.TIL_BEHANDLING),
            ).totalAntall shouldBe 1
            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(kilde = BenkTilbakekrevingKilde.MELDEKORT),
            ).totalAntall shouldBe 2
            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(kilde = BenkTilbakekrevingKilde.RAMMEVEDTAK),
            ).totalAntall shouldBe 0
            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(saksbehandler = BenkFiltrering.IKKE_TILDELT),
            ).totalAntall shouldBe 2
            // Minstebeløpsfilteret er benkens eneste tallfilter, og skal kutte begge veier.
            repo.hentTilbakekrevinger(tilbakekrevingCommand(minstebeløp = 1)).totalAntall shouldBe 2
            repo.hentTilbakekrevinger(tilbakekrevingCommand(minstebeløp = 1_000_000)).totalAntall shouldBe 0
            // Begge behandlingene får samme klokke-styrte kravgrunnlagperiode, så rekkefølgen er ikke observerbar — spørringen skal bare gå gjennom.
            repo.hentTilbakekrevinger(
                tilbakekrevingCommand(kolonne = BenkTilbakekrevingKolonne.KRAVGRUNNLAG_PERIODE),
            ).totalAntall shouldBe 2
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `tilbakekrevingsfanen utleder under-statuser fra hvem som har tatt behandlingen`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakTilBehandling, tilBehandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val (sakTilGodkjenning, tilGodkjenning) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)
            tildelTilbakekrevingBehandling(tac = tac, sakId = sakTilBehandling.id, tilbakekrevingId = tilBehandling.id)!!
            // Tildelingen går beslutter-veien når behandlingen er til godkjenning.
            tildelTilbakekrevingBehandling(
                tac = tac,
                sakId = sakTilGodkjenning.id,
                tilbakekrevingId = tilGodkjenning.id,
                saksbehandler = ObjectMother.beslutter("beslutterSomTar"),
            )!!

            val oversikt = tac.benkContext.benkRepo.hentTilbakekrevinger(tilbakekrevingCommand())

            oversikt.totalAntall shouldBe 2
            oversikt.behandlinger.single { it.felles.sakId == sakTilBehandling.id }.let {
                it.status shouldBe BenkTilbakekrevingStatus.UNDER_BEHANDLING
                it.felles.saksbehandler shouldBe "saksbehandlerSomTar"
            }
            oversikt.behandlinger.single { it.felles.sakId == sakTilGodkjenning.id }.let {
                it.status shouldBe BenkTilbakekrevingStatus.UNDER_GODKJENNING
                it.felles.beslutter shouldBe "beslutterSomTar"
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `tilbakekrevingsfanen viser venter-status, og skjulPåVent tar bort raden`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, tilBehandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            sendTilbakekrevingHendelseOgKjørJobb(
                tac = tac,
                sak = sak,
                tilbakeBehandlingId = tilBehandling.tilbakeBehandlingId,
                behandlingsstatus = "TIL_BEHANDLING",
                forrigeBehandlingsstatus = "TIL_BEHANDLING",
                venterGjenopptas = 28.februar(2026),
            )
            val repo = tac.benkContext.benkRepo

            repo.hentTilbakekrevinger(tilbakekrevingCommand()).behandlinger.single().felles.ventestatus.let {
                it.erSattPåVent shouldBe true
                it.begrunnelse shouldBe "AVVENTER_BRUKERUTTALELSE"
                it.frist shouldBe 28.februar(2026)
            }

            repo.hentTilbakekrevinger(tilbakekrevingCommand(skjulPåVent = true)).let {
                it.totalAntall shouldBe 0
                it.totalAntallUfiltrert shouldBe 1
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `tilbakekrevingsfanen viser opprettede behandlinger, men ikke avsluttede`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            // Legacy-benken viste ikke OPPRETTET; v2 viser den bevisst, slik at saksbehandler ser behandlingen før noen har tatt den.
            val (sakKunOpprettet) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!
            sendTilbakekrevingHendelseOgKjørJobb(
                tac = tac,
                sak = tac.sakContext.sakRepo.hentForSakId(sakKunOpprettet.id)!!,
                tilbakeBehandlingId = "tilbake-kun-opprettet",
                behandlingsstatus = "OPPRETTET",
                forrigeBehandlingsstatus = null,
            )
            val (sakAvsluttet, tilbakekrevingSomAvsluttes) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            sendTilbakekrevingHendelseOgKjørJobb(
                tac = tac,
                sak = sakAvsluttet,
                tilbakeBehandlingId = tilbakekrevingSomAvsluttes.tilbakeBehandlingId,
                behandlingsstatus = "AVSLUTTET",
                forrigeBehandlingsstatus = "TIL_BEHANDLING",
            )

            val oversikt = tac.benkContext.benkRepo.hentTilbakekrevinger(tilbakekrevingCommand())

            oversikt.totalAntall shouldBe 1
            oversikt.behandlinger.single().let {
                it.felles.sakId shouldBe sakKunOpprettet.id
                it.status shouldBe BenkTilbakekrevingStatus.OPPRETTET
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `antall per fane telles uten filter`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac)
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            oppdaterRevurderingStans(tac = tac, sakId = sak.id, behandlingId = revurdering.id)
            iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)
            opprettSakOgKlagebehandlingTilAvvisning(tac = tac)
            opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            tac.benkContext.benkRepo.hentAntallPerFane() shouldBe
                BenkAntallPerFane(
                    søknader = 1,
                    revurderinger = 1,
                    meldekort = 1,
                    klage = 1,
                    tilbakekreving = 1,
                )
        }
    }

    /**
     * Sender en behandling_endret-hendelse fra tilbakekrevingskomponenten og kjører hendelsejobben, slik hendelsene flyter i prod.
     * Dekker tilstandene tilbakekreving-builderne ikke har egne funksjoner for: venter, OPPRETTET uten videre flyt og AVSLUTTET.
     */
    private suspend fun sendTilbakekrevingHendelseOgKjørJobb(
        tac: TestApplicationContextMedPostgres,
        sak: Sak,
        tilbakeBehandlingId: String,
        behandlingsstatus: String,
        forrigeBehandlingsstatus: String?,
        totaltFeilutbetaltBeløp: BigDecimal = BigDecimal("1000.00"),
        venterGjenopptas: LocalDate? = null,
    ) {
        val venterJson = venterGjenopptas?.let { """{"grunn": "AVVENTER_BRUKERUTTALELSE", "gjenopptas": "$it"}""" } ?: "null"
        val forrigeJson = forrigeBehandlingsstatus?.let { "\"$it\"" } ?: "null"

        @Language("JSON")
        val hendelseJson = """
            {
                "hendelsestype": "behandling_endret",
                "versjon": 1,
                "eksternFagsakId": "${sak.saksnummer.verdi}",
                "hendelseOpprettet": "${nå(tac.clock).plusSeconds(30)}",
                "eksternBehandlingId": "${sak.utbetalinger.first().id.uuidPart()}",
                "tilbakekreving": {
                    "behandlingId": "$tilbakeBehandlingId",
                    "sakOpprettet": "${nå(tac.clock)}",
                    "varselSendt": null,
                    "behandlingsstatus": "$behandlingsstatus",
                    "forrigeBehandlingsstatus": $forrigeJson,
                    "totaltFeilutbetaltBeløp": ${totaltFeilutbetaltBeløp.toPlainString()},
                    "venter": $venterJson,
                    "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/$tilbakeBehandlingId",
                    "fullstendigPeriode": {
                        "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                        "tom": "${LocalDate.now(tac.clock)}"
                    }
                }
            }
        """.trimIndent()

        val hendelseId = konsumerTilbakekrevingshendelse(
            key = sak.fnr.verdi,
            value = hendelseJson,
            tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
            clock = tac.clock,
        )!!
        // Jobbens kø-spørring går på tvers av saker, så vi kjører kun vår egen hendelse.
        tac.behandleTilbakekrevingHendelserJobb.håndterHendelse(hendelseId)
    }

    /** Sender inn et rent brukers meldekort via motta-ruta, klart for automatisk behandling. */
    private suspend fun ApplicationTestBuilder.mottaAutomatiskMeldekortForKjede(
        tac: TestApplicationContextMedPostgres,
        sak: Sak,
        kjedeIndeks: Int = 0,
    ): Pair<Sak, BrukersMeldekort> {
        val meldeperiode = sak.meldeperiodeKjeder[kjedeIndeks].hentSisteMeldeperiode()
        val (oppdatertSak, brukersMeldekort, _) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            dager = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort),
            journalpostId = UUID.randomUUID().toString(),
        )
        return oppdatertSak to brukersMeldekort!!
    }

    /**
     * Sender inn brukers meldekort med for mye sammenhengende godkjent fravær via motta-ruta.
     * Ruta setter `behandlesAutomatisk = true` på alle kort, så prodstien til benken går gjennom den automatiske jobben: den gir opp kortet og markerer det for manuell behandling.
     */
    private suspend fun ApplicationTestBuilder.mottaManueltMeldekortForKjede(
        tac: TestApplicationContextMedPostgres,
        sak: Sak,
        kjedeIndeks: Int = 0,
    ): Pair<Sak, BrukersMeldekort> {
        val meldeperiode = sak.meldeperiodeKjeder[kjedeIndeks].hentSisteMeldeperiode()
        val utfylt = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort)
        val fraværsdager = utfylt.keys
            .filter { utfylt.getValue(it) != BrukerutfyltMeldekortDTO.Status.IKKE_BESVART }
            .sorted()
            .windowed(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER + 1)
            .first { vindu -> vindu.zipWithNext().all { (a, b) -> b == a.plusDays(1) } }
            .toSet()
        val dagerMedFravær = utfylt.mapValues { (dato, status) ->
            if (dato in fraværsdager) BrukerutfyltMeldekortDTO.Status.FRAVÆR_GODKJENT_AV_NAV else status
        }
        val (oppdatertSak, brukersMeldekort, _) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            dager = dagerMedFravær,
            journalpostId = UUID.randomUUID().toString(),
        )
        return oppdatertSak to brukersMeldekort!!
    }
}
