package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragBenktype
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragStatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BehandlingssammendragType
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSortering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand
import no.nav.tiltakspenger.saksbehandling.benk.domene.SorteringRetning
import no.nav.tiltakspenger.saksbehandling.benk.domene.TilbakekrevingKilde
import no.nav.tiltakspenger.saksbehandling.benk.domene.ÅpneBehandlingerFiltrering
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort.Companion.MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingType
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstiltOpprettholdtKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgMottaOppretholdtKlagebehandlingFraKa
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgSettPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettTilbakekrevingBehandlingTilGodkjenning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settMeldekortbehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.konsumerTilbakekrevingshendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Aggregat-test for benkoversikten, jf. testtaksonomien i `AGENTS.md`.
 *
 * Totalantallet på tvers av saker er selve featuren her, ikke en krykke: benken viser alle åpne behandlinger i hele skjemaet.
 * Derfor kjører alle testene isolert, og derfor kaller de `hentÅpneBehandlinger(limit)` direkte.
 *
 * Tilstanden bygges gjennom prodstiene: routene, consumerne og jobbene, slik de kjører i nais.
 */
class BenkOversiktAggregatTest {
    private fun newCommand(
        benktype: List<BehandlingssammendragBenktype>? = null,
        behandlingstype: List<BehandlingssammendragType>? = null,
        status: List<BehandlingssammendragStatus>? = null,
        saksbehandlere: List<String>? = null,
        tilbakekrevingMinstebeløp: Long = 0,
        sortering: BenkSortering = BenkSortering(BenkSorteringKolonne.STARTET, SorteringRetning.ASC),
    ): HentÅpneBehandlingerCommand {
        return HentÅpneBehandlingerCommand(
            åpneBehandlingerFiltrering = ÅpneBehandlingerFiltrering(
                benktype = benktype,
                behandlingstype = behandlingstype,
                status = status,
                identer = saksbehandlere,
                tilbakekrevingMinBeløp = tilbakekrevingMinstebeløp,
            ),
            sortering = sortering,
            saksbehandler = ObjectMother.saksbehandler(),
            correlationId = CorrelationId.generate(),
        )
    }

    /** Iverksetter en søknadsbehandling og driver en stans-revurdering til beslutning via routene. */
    private suspend fun ApplicationTestBuilder.revurderingStansTilBeslutning(
        tac: TestApplicationContextMedPostgres,
    ): Pair<Sak, RammebehandlingId> {
        val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
        oppdaterRevurderingStans(tac = tac, sakId = sak.id, behandlingId = revurdering.id)
        sendRevurderingTilBeslutningForBehandlingId(tac, sak.id, revurdering.id)
        return sak to revurdering.id
    }

    /**
     * Sender en behandling_endret-hendelse fra tilbakekrevingskomponenten og kjører hendelsejobben, slik hendelsene flyter i prod.
     * Dekker tilstandene tilbakekreving-builderne ikke har egne funksjoner for: venter, styrt beløp, OPPRETTET uten videre flyt og AVSLUTTET.
     */
    private fun sendTilbakekrevingHendelseOgKjørJobb(
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
            // Journalpost-IDen er unik per meldekort.
            journalpostId = UUID.randomUUID().toString(),
        )
        return oppdatertSak to brukersMeldekort!!
    }

    /**
     * Sender inn brukers meldekort med for mye sammenhengende godkjent fravær via motta-ruta.
     * Ruta setter `behandlesAutomatisk = true` på alle kort, så prodstien til benken går gjennom den automatiske jobben: den gir opp kortet og markerer det for manuell behandling.
     * Kalleren kjører `automatiskMeldekortbehandlingJobb` når alle kortene er mottatt, og klokka må stå på en hverdag innenfor økonomisystemets åpningstider.
     */
    private suspend fun ApplicationTestBuilder.mottaManueltMeldekortForKjede(
        tac: TestApplicationContextMedPostgres,
        sak: Sak,
        kjedeIndeks: Int = 0,
    ): Pair<Sak, BrukersMeldekort> {
        val meldeperiode = sak.meldeperiodeKjeder[kjedeIndeks].hentSisteMeldeperiode()
        val utfylt = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort)
        // Bruker kan bare registrere på dager med rett, så fraværet må legges på dager hen faktisk kan fylle ut.
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
            // Journalpost-IDen er unik per meldekort.
            journalpostId = UUID.randomUUID().toString(),
        )
        return oppdatertSak to brukersMeldekort!!
    }

    /** Tar en eksisterende meldekortbehandling gjennom hele saksbehandlingsflyten via rutene, fram til iverksettelse. */
    private suspend fun ApplicationTestBuilder.iverksettEksisterendeMeldekortbehandling(
        tac: TestApplicationContextMedPostgres,
        sakId: SakId,
        meldekortId: MeldekortId,
    ) {
        val saksbehandler = ObjectMother.saksbehandler()
        val beslutter = ObjectMother.beslutter()
        taMeldekortbehanding(tac = tac, sakId = sakId, meldekortId = meldekortId, saksbehandlerEllerBeslutter = saksbehandler)
        oppdaterMeldekortbehandling(tac = tac, sakId = sakId, meldekortId = meldekortId, saksbehandler = saksbehandler)
        sendMeldekortbehandlingTilBeslutning(tac = tac, sakId = sakId, meldekortId = meldekortId, saksbehandler = saksbehandler)
        taMeldekortbehanding(tac = tac, sakId = sakId, meldekortId = meldekortId, saksbehandlerEllerBeslutter = beslutter)
        iverksettMeldekortbehandling(tac = tac, sakId = sakId, meldekortId = meldekortId, beslutter = beslutter)
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter åpne søknader uten behandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, søknad) = opprettSakOgSøknad(tac = tac)

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 1
            totalAntallUfiltrert shouldBe 1
            actual.size shouldBe 1
            actual.first() shouldBe Behandlingssammendrag(
                sakId = sak.id,
                fnr = søknad.fnr,
                saksnummer = søknad.saksnummer,
                startet = søknad.opprettet,
                kravtidspunkt = søknad.opprettet,
                behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = null,
                beslutter = null,
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = null,
                resultat = null,
                erUnderkjent = false,
                beløp = null,
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter åpne søknadsbehandlinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUnderBehandling, _, opprettetBehandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(tac = tac)
            val (sakKlarTilBeslutning, _, klarTilBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            val (sakUnderBeslutning, _, underBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taBehandling(tac, sakUnderBeslutning.id, underBeslutningId, ObjectMother.beslutter())
            iverksettSøknadsbehandling(tac = tac)
            iverksettSøknadsbehandling(tac = tac, resultat = SøknadsbehandlingsresultatType.AVSLAG)
            opprettSøknadsbehandlingOgAvbryt(tac = tac)
            val underBehandling = tac.behandlingContext.rammebehandlingRepo.hent(opprettetBehandling.id)
            val klarTilBeslutning = tac.behandlingContext.rammebehandlingRepo.hent(klarTilBeslutningId)
            val underBeslutning = tac.behandlingContext.rammebehandlingRepo.hent(underBeslutningId)

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 3
            totalAntallUfiltrert shouldBe 3
            actual.size shouldBe 3
            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    sakId = sakUnderBehandling.id,
                    fnr = underBehandling.fnr,
                    saksnummer = underBehandling.saksnummer,
                    startet = underBehandling.opprettet,
                    kravtidspunkt = underBehandling.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                    sistEndret = underBehandling.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
                    erUnderkjent = underBehandling.erUnderkjent,
                    beløp = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    sakId = sakKlarTilBeslutning.id,
                    fnr = klarTilBeslutning.fnr,
                    saksnummer = klarTilBeslutning.saksnummer,
                    startet = klarTilBeslutning.opprettet,
                    kravtidspunkt = klarTilBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                    sistEndret = klarTilBeslutning.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
                    erUnderkjent = klarTilBeslutning.erUnderkjent,
                    beløp = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    sakId = sakUnderBeslutning.id,
                    fnr = underBeslutning.fnr,
                    saksnummer = underBeslutning.saksnummer,
                    startet = underBeslutning.opprettet,
                    kravtidspunkt = underBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.SØKNADSBEHANDLING,
                    status = BehandlingssammendragStatus.UNDER_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = ObjectMother.beslutter().navIdent,
                    sistEndret = underBeslutning.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
                    erUnderkjent = underBeslutning.erUnderkjent,
                    beløp = null,
                )
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter åpne revurderinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakOpprettetRevurdering, _, _, opprettetRevurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            val (sakRevurderingTilBeslutning, revurderingTilBeslutningId) = revurderingStansTilBeslutning(tac)
            val (sakRevurderingUnderBeslutning, revurderingUnderBeslutningId) = revurderingStansTilBeslutning(tac)
            taBehandling(tac, sakRevurderingUnderBeslutning.id, revurderingUnderBeslutningId, ObjectMother.beslutter())
            iverksettSøknadsbehandlingOgRevurderingStans(tac = tac)
            val (sakMedAvbruttRevurdering, _, _, revurderingSomAvbrytes) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            avbrytRammebehandling(
                tac = tac,
                saksnummer = sakMedAvbruttRevurdering.saksnummer,
                sakId = sakMedAvbruttRevurdering.id,
                rammebehandlingId = revurderingSomAvbrytes.id,
            )
            val revurderingTilBeslutning = tac.behandlingContext.rammebehandlingRepo.hent(revurderingTilBeslutningId)
            val revurderingUnderBeslutning = tac.behandlingContext.rammebehandlingRepo.hent(revurderingUnderBeslutningId)

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 3
            totalAntallUfiltrert shouldBe 3
            actual.size shouldBe 3
            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    sakId = sakOpprettetRevurdering.id,
                    fnr = opprettetRevurdering.fnr,
                    saksnummer = opprettetRevurdering.saksnummer,
                    startet = opprettetRevurdering.opprettet,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                    kravtidspunkt = null,
                    sistEndret = opprettetRevurdering.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = RammebehandlingResultatTypeDTO.STANS,
                    erUnderkjent = opprettetRevurdering.erUnderkjent,
                    beløp = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    sakId = sakRevurderingTilBeslutning.id,
                    fnr = revurderingTilBeslutning.fnr,
                    saksnummer = revurderingTilBeslutning.saksnummer,
                    startet = revurderingTilBeslutning.opprettet,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                    kravtidspunkt = null,
                    sistEndret = revurderingTilBeslutning.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = RammebehandlingResultatTypeDTO.STANS,
                    erUnderkjent = revurderingTilBeslutning.erUnderkjent,
                    beløp = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    sakId = sakRevurderingUnderBeslutning.id,
                    fnr = revurderingUnderBeslutning.fnr,
                    saksnummer = revurderingUnderBeslutning.saksnummer,
                    startet = revurderingUnderBeslutning.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.REVURDERING,
                    status = BehandlingssammendragStatus.UNDER_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = ObjectMother.beslutter().navIdent,
                    sistEndret = revurderingUnderBeslutning.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = RammebehandlingResultatTypeDTO.STANS,
                    erUnderkjent = revurderingUnderBeslutning.erUnderkjent,
                    beløp = null,
                )
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter meldekort som er klar til behandling`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak1Iverksatt) = iverksettSøknadsbehandling(tac = tac)
            // Første kort i kjeden behandles automatisk; korrigeringen etterpå faller til manuell behandling og havner på benken.
            mottaAutomatiskMeldekortForKjede(tac, sak1Iverksatt, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            // Utbetalingen fra den automatiske behandlingen må bekreftes før jobben tar flere kort på saken.
            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()
            // Korrigeringen på kjede én og kortet på kjede to faller begge til manuell behandling.
            mottaManueltMeldekortForKjede(tac, sak1Iverksatt, kjedeIndeks = 0)
            mottaManueltMeldekortForKjede(tac, sak1Iverksatt, kjedeIndeks = 1)
            val (sak2Iverksatt) = iverksettSøknadsbehandling(tac = tac)
            mottaManueltMeldekortForKjede(tac, sak2Iverksatt, kjedeIndeks = 0)
            // Jobben tar ett kort per sak per kjøring, så den kjøres til køen er drenert — som cron-intervallet i prod.
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            // Meldekortene jobben ga opp er akkumulert inn i én åpen meldekortbehandling per sak, og det er behandlingene som ligger på benken.
            val akkumulertBehandlingSak1 = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak1Iverksatt.id)!!
                .åpneMeldekortbehandlinger.single()
            val akkumulertBehandlingSak2 = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak2Iverksatt.id)!!
                .åpneMeldekortbehandlinger.single()

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 2
            totalAntallUfiltrert shouldBe 2
            actual.size shouldBe 2

            actual shouldBe listOf(akkumulertBehandlingSak1, akkumulertBehandlingSak2)
                .sortedBy { it.opprettet }
                .map { behandling ->
                    Behandlingssammendrag(
                        sakId = behandling.sakId,
                        fnr = behandling.fnr,
                        saksnummer = behandling.saksnummer,
                        startet = behandling.opprettet,
                        kravtidspunkt = null,
                        behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                        status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                        saksbehandler = null,
                        beslutter = null,
                        sistEndret = behandling.sistEndret,
                        erSattPåVent = false,
                        sattPåVentBegrunnelse = null,
                        sattPåVentFrist = null,
                        resultat = null,
                        erUnderkjent = false,
                        beløp = null,
                    )
                }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter åpne meldekortbehandlinger`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sakAIverksatt) = iverksettSøknadsbehandling(tac = tac)
            // Meldekortet jobben gir opp akkumuleres inn i en ny manuell behandling, og det er behandlingen som ligger på benken.
            val (sakMedInnsendtBrukersMeldekort) = mottaManueltMeldekortForKjede(tac, sakAIverksatt)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            val akkumulertBehandling = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sakAIverksatt.id)!!
                .åpneMeldekortbehandlinger.single()
            val (sakMedOpprettetMeldekortbehandling, _, _, opprettetMeldekortbehandling, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            val (sakMedMeldekortbehandlingTilBeslutning, _, _, meldekortbehandlingTilBeslutning, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac = tac)!!
            iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 3
            totalAntallUfiltrert shouldBe 3
            actual.size shouldBe 3
            tac.verifiserViHarNMeldekortbehandlinger(4)

            actual.let {
                it.first() shouldBe Behandlingssammendrag(
                    sakId = sakMedInnsendtBrukersMeldekort.id,
                    fnr = sakMedInnsendtBrukersMeldekort.fnr,
                    saksnummer = sakMedInnsendtBrukersMeldekort.saksnummer,
                    startet = akkumulertBehandling.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    sistEndret = akkumulertBehandling.sistEndret,
                    resultat = null,
                    erUnderkjent = false,
                    beløp = null,
                )
                it[1] shouldBe Behandlingssammendrag(
                    sakId = sakMedOpprettetMeldekortbehandling.id,
                    fnr = sakMedOpprettetMeldekortbehandling.fnr,
                    saksnummer = sakMedOpprettetMeldekortbehandling.saksnummer,
                    startet = opprettetMeldekortbehandling.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    sistEndret = opprettetMeldekortbehandling.sistEndret,
                    resultat = null,
                    erUnderkjent = opprettetMeldekortbehandling.erUnderkjent,
                    beløp = null,
                )
                it.last() shouldBe Behandlingssammendrag(
                    sakId = sakMedMeldekortbehandlingTilBeslutning.id,
                    fnr = sakMedMeldekortbehandlingTilBeslutning.fnr,
                    saksnummer = sakMedMeldekortbehandlingTilBeslutning.saksnummer,
                    startet = meldekortbehandlingTilBeslutning.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BESLUTNING,
                    saksbehandler = ObjectMother.saksbehandler().navIdent,
                    beslutter = null,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    sistEndret = meldekortbehandlingTilBeslutning.sistEndret,
                    resultat = null,
                    erUnderkjent = meldekortbehandlingTilBeslutning.erUnderkjent,
                    beløp = null,
                )
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter meldekortbehandling som er satt på vent`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val frist = LocalDate.now(tac.clock).plusWeeks(1)
            val (sak, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = ObjectMother.saksbehandler(),
                begrunnelse = "Venter på dokumentasjon",
                frist = frist,
            )
            val meldekortbehandlingPåVent = tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandling.id)!!

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(
                    benktype = listOf(BehandlingssammendragBenktype.VENTER),
                    behandlingstype = listOf(BehandlingssammendragType.MELDEKORTBEHANDLING),
                ),
            )

            totalAntall shouldBe 1
            totalAntallUfiltrert shouldBe 1
            actual shouldBe listOf(
                Behandlingssammendrag(
                    sakId = sak.id,
                    fnr = sak.fnr,
                    saksnummer = sak.saksnummer,
                    startet = meldekortbehandling.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    erSattPåVent = true,
                    sattPåVentBegrunnelse = "Venter på dokumentasjon",
                    sattPåVentFrist = frist,
                    sistEndret = meldekortbehandlingPåVent.sistEndret,
                    resultat = null,
                    erUnderkjent = meldekortbehandlingPåVent.erUnderkjent,
                    beløp = null,
                ),
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter ikke meldekort som har mottatt tidspunkt som er mindre enn siste meldekort behandling`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sakIverksatt) = iverksettSøknadsbehandling(tac = tac)
            mottaManueltMeldekortForKjede(tac, sakIverksatt)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            // Meldekortet er akkumulert inn i en ny behandling, og det er den som ligger på benken.
            val akkumulertBehandling = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sakIverksatt.id)!!
                .åpneMeldekortbehandlinger.single()

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 1
            totalAntallUfiltrert shouldBe 1
            actual shouldBe listOf(
                Behandlingssammendrag(
                    sakId = sakIverksatt.id,
                    fnr = sakIverksatt.fnr,
                    saksnummer = sakIverksatt.saksnummer,
                    startet = akkumulertBehandling.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    sistEndret = akkumulertBehandling.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = null,
                    erUnderkjent = false,
                    beløp = null,
                ),
            )

            // Når behandlingen er iverksatt er meldekortet tatt stilling til, og ingenting ligger igjen på benken.
            iverksettEksisterendeMeldekortbehandling(tac, sakIverksatt.id, akkumulertBehandling.id)

            val (actualEtterIverksetting, totalAntallEtterIverksetting, totalAntallUfiltrertEtterIverksetting) =
                tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntallEtterIverksetting shouldBe 0
            totalAntallUfiltrertEtterIverksetting shouldBe 0
            actualEtterIverksetting shouldBe emptyList()

            // En korrigering som jobben gir opp akkumuleres inn i en ny behandling, som igjen er den som ligger på benken.
            mottaManueltMeldekortForKjede(tac, sakIverksatt)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            val nyAkkumulertBehandling = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sakIverksatt.id)!!
                .åpneMeldekortbehandlinger.single()

            val (actualKorrigering, totalAntallKorrigering, totalAntallUfiltrertKorrigering) =
                tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntallKorrigering shouldBe 1
            totalAntallUfiltrertKorrigering shouldBe 1
            actualKorrigering.single().let {
                it.behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING
                it.startet shouldBe nyAkkumulertBehandling.opprettet
            }

            iverksettEksisterendeMeldekortbehandling(tac, sakIverksatt.id, nyAkkumulertBehandling.id)

            val (actualEtterIverksettingIgjen, totalAntallEtterIverksettingIgjen, totalAntallUfiltrertEtterIverksettingIgjen) =
                tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(newCommand())

            totalAntallEtterIverksettingIgjen shouldBe 0
            totalAntallUfiltrertEtterIverksettingIgjen shouldBe 0
            actualEtterIverksettingIgjen shouldBe emptyList()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter ikke meldekort der en behandling i ettertid har blitt avsluttet`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sakIverksatt) = iverksettSøknadsbehandling(tac = tac)
            mottaManueltMeldekortForKjede(tac, sakIverksatt)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            // Meldekortet er akkumulert inn i en ny behandling, og det er den som ligger på benken.
            val akkumulertBehandling = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sakIverksatt.id)!!
                .åpneMeldekortbehandlinger.single()

            val (actualFørBehandling, totalAntallFørBehandling, totalAntallUfiltrertFørBehandling) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )
            totalAntallFørBehandling shouldBe 1
            totalAntallUfiltrertFørBehandling shouldBe 1
            actualFørBehandling shouldBe listOf(
                Behandlingssammendrag(
                    sakId = sakIverksatt.id,
                    fnr = sakIverksatt.fnr,
                    saksnummer = sakIverksatt.saksnummer,
                    startet = akkumulertBehandling.opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.MELDEKORTBEHANDLING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    sistEndret = akkumulertBehandling.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = null,
                    erUnderkjent = false,
                    beløp = null,
                ),
            )

            // Behandlingen må være tatt av en saksbehandler før den kan avbrytes.
            taMeldekortbehanding(
                tac = tac,
                sakId = sakIverksatt.id,
                meldekortId = akkumulertBehandling.id,
                saksbehandlerEllerBeslutter = ObjectMother.saksbehandler(),
            )
            avbrytMeldekortbehandling(tac = tac, sakId = sakIverksatt.id, meldekortId = akkumulertBehandling.id)!!

            val (actualEtterAvbrytelse, totalAntallEtterAvbrytelse, totalAntallUfiltrertEtterAvbrytelse) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntallEtterAvbrytelse shouldBe 0
            totalAntallUfiltrertEtterAvbrytelse shouldBe 0
            actualEtterAvbrytelse shouldBe emptyList()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `korrigering på kjede med åpen behandling knyttes til behandlingen uten å havne på benken`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sakIverksatt) = iverksettSøknadsbehandling(tac = tac)
            val (_, førsteMeldekort) = mottaManueltMeldekortForKjede(tac, sakIverksatt)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            // Meldekortet er akkumulert inn i en ny behandling, og det er den som ligger på benken.
            val akkumulertBehandling = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sakIverksatt.id)!!
                .åpneMeldekortbehandlinger.single()

            val (actualMedNyBehandling, totalAntallMedNyBehandling, totalAntallUfiltrertMedNyBehandling) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntallMedNyBehandling shouldBe 1
            totalAntallUfiltrertMedNyBehandling shouldBe 1
            actualMedNyBehandling.single().behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING

            // Saksbehandler tar behandlingen.
            taMeldekortbehanding(
                tac = tac,
                sakId = sakIverksatt.id,
                meldekortId = akkumulertBehandling.id,
                saksbehandlerEllerBeslutter = ObjectMother.saksbehandler(),
            )

            // Bruker sender en korrigering på samme kjede.
            // Den kan ikke behandles automatisk mens behandlingen er åpen, og prøves på nytt en stund før den havner i behandlingen.
            val (_, korrigering) = mottaManueltMeldekortForKjede(tac, sakIverksatt)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            val (actualMedKorrigeringFraBruker, totalAntallMedNyKorrigeringFraBruker, totalAntallUfiltrertMedNyKorrigeringFraBruker) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            // Korrigeringen vises ikke på benken mens den prøves på nytt - kun behandlingen ligger der.
            totalAntallMedNyKorrigeringFraBruker shouldBe 1
            totalAntallUfiltrertMedNyKorrigeringFraBruker shouldBe 1
            actualMedKorrigeringFraBruker.single().behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING

            // Når jobben har gitt opp knyttes korrigeringen til behandlingen - den havner fortsatt ikke på benken.
            // Klokka må stå på en hverdag innenfor økonomisystemets åpningstider, ellers hopper jobben over alle meldekort.
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(fixedClockAt(5.mai(2025).atTime(12, 0)))

            tac.meldekortContext.meldekortbehandlingRepo.hent(akkumulertBehandling.id)!!.let { behandling ->
                behandling.meldeperioder.single().brukersMeldekort.map { it.id } shouldBe
                    listOf(førsteMeldekort.id, korrigering.id)
            }

            val (actualEtterKnytting, totalAntallEtterKnytting, totalAntallUfiltrertEtterKnytting) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntallEtterKnytting shouldBe 1
            totalAntallUfiltrertEtterKnytting shouldBe 1
            actualEtterKnytting.single().behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `første meldekort i en kjede er 'FØRSTE_BEHANDLING', deretter er det 'KORRIGERING' i den akkumulerte behandlingen`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sakIverksatt) = iverksettSøknadsbehandling(tac = tac)

            // Bruker sender første meldekort i kjeden, og det behandles automatisk uten å innom benken.
            // Et rent meldekort vises aldri på benken: ruta markerer det for automatisk behandling, og jobben tar det.
            mottaAutomatiskMeldekortForKjede(tac, sakIverksatt, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
            tac.utbetalingContext.oppdaterUtbetalingsstatusService.oppdaterUtbetalingsstatus()

            val (actualEtterAutomatiskBehandling, antallEtterAutomatiskBehandling) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            antallEtterAutomatiskBehandling shouldBe 0
            actualEtterAutomatiskBehandling shouldBe emptyList()

            // Bruker sender korrigering i samme kjede, som jobben gir opp - den akkumuleres inn i en ny behandling.
            mottaManueltMeldekortForKjede(tac, sakIverksatt, kjedeIndeks = 0)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            val akkumulertBehandling = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sakIverksatt.id)!!
                .åpneMeldekortbehandlinger.single()
            akkumulertBehandling.meldeperioder.single().type shouldBe MeldeperiodebehandlingType.KORRIGERING

            val (actualEtterKorrigering, antallEtterKorrigering) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            // Det er den akkumulerte behandlingen som ligger på benken, ikke selve korrigeringen.
            antallEtterKorrigering shouldBe 1
            actualEtterKorrigering.single().behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING

            // Første kort i en annen kjede som faller til manuell behandling, akkumuleres inn i samme behandling.
            mottaManueltMeldekortForKjede(tac, sakIverksatt, kjedeIndeks = 1)
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            tac.meldekortContext.meldekortbehandlingRepo.hent(akkumulertBehandling.id)!!.let { behandling ->
                behandling.meldeperioder.map { it.type } shouldBe listOf(
                    MeldeperiodebehandlingType.KORRIGERING,
                    MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                )
            }

            val (actualEtterFørsteIAnnenKjede, antallEtterFørsteIAnnenKjede) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            antallEtterFørsteIAnnenKjede shouldBe 1
            actualEtterFørsteIAnnenKjede.single().behandlingstype shouldBe BehandlingssammendragType.MELDEKORTBEHANDLING
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter åpne klagebehandlinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, klagebehandling, _) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!
            val (sakTilVurdering, _, klagebehandlingTilVurdering, _) = opprettSakOgKlagebehandlingTilOpprettholdelse(tac = tac)!!
            settKlagebehandlingPåVent(
                tac = tac,
                sakId = sakTilVurdering.id,
                klagebehandlingId = klagebehandlingTilVurdering.id,
                begrunnelse = "Venter på svar fra bruker",
                frist = 13.februar(2026),
            )!!
            opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val (_, mottattFraKa, _) = opprettSakOgMottaOppretholdtKlagebehandlingFraKa(tac = tac)!!
            ferdigstiltOpprettholdtKlagebehandling(tac = tac)!!
            val klagebehandlingPåVent = tac.klagebehandlingContext.klagebehandlingRepo.hentForKlagebehandlingId(klagebehandlingTilVurdering.id)!!
            val oversendtKlagebehandlingMedSvarFraKA = tac.klagebehandlingContext.klagebehandlingRepo.hentForKlagebehandlingId(mottattFraKa.id)!!

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 3
            totalAntallUfiltrert shouldBe 3
            actual.size shouldBe 3
            actual.first() shouldBe Behandlingssammendrag(
                sakId = klagebehandling.sakId,
                fnr = klagebehandling.fnr,
                saksnummer = klagebehandling.saksnummer,
                startet = klagebehandling.opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.KLAGEBEHANDLING,
                status = BehandlingssammendragStatus.UNDER_BEHANDLING,
                saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling").navIdent,
                beslutter = null,
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = klagebehandling.sistEndret,
                resultat = null,
                erUnderkjent = false,
                beløp = null,
            )
            actual[1] shouldBe Behandlingssammendrag(
                sakId = klagebehandlingPåVent.sakId,
                fnr = klagebehandlingPåVent.fnr,
                saksnummer = klagebehandlingPåVent.saksnummer,
                startet = klagebehandlingPåVent.opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.KLAGEBEHANDLING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = null,
                beslutter = null,
                erSattPåVent = true,
                sattPåVentBegrunnelse = "Venter på svar fra bruker",
                sattPåVentFrist = 13.februar(2026),
                sistEndret = klagebehandlingPåVent.sistEndret,
                resultat = null,
                erUnderkjent = false,
                beløp = null,
            )
            actual.last() shouldBe Behandlingssammendrag(
                sakId = oversendtKlagebehandlingMedSvarFraKA.sakId,
                fnr = oversendtKlagebehandlingMedSvarFraKA.fnr,
                saksnummer = oversendtKlagebehandlingMedSvarFraKA.saksnummer,
                startet = oversendtKlagebehandlingMedSvarFraKA.opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.KLAGEBEHANDLING,
                status = BehandlingssammendragStatus.KLAR_TIL_FERDIGSTILLING,
                saksbehandler = oversendtKlagebehandlingMedSvarFraKA.saksbehandler,
                beslutter = null,
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                sistEndret = oversendtKlagebehandlingMedSvarFraKA.sistEndret,
                resultat = null,
                erUnderkjent = false,
                beløp = null,
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter åpne tilbakekrevinger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakTilBehandling, tilBehandling) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            val (sakTilBehandlingVenter, tilBehandlingVenter) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            sendTilbakekrevingHendelseOgKjørJobb(
                tac = tac,
                sak = sakTilBehandlingVenter,
                tilbakeBehandlingId = tilBehandlingVenter.tilbakeBehandlingId,
                behandlingsstatus = "TIL_BEHANDLING",
                forrigeBehandlingsstatus = "TIL_BEHANDLING",
                venterGjenopptas = 28.februar(2026),
            )
            val (sakTilGodkjenning, tilGodkjenning) = opprettTilbakekrevingBehandlingTilGodkjenning(tac = tac)

            // Opprettet og avsluttet skal ikke dukke opp.
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
            val oppdatertTilBehandlingVenter = tac.sakContext.sakRepo.hentForSakId(sakTilBehandlingVenter.id)!!.tilbakekrevinger.single()

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(
                    sortering = BenkSortering(
                        BenkSorteringKolonne.STATUS,
                        SorteringRetning.ASC,
                    ),
                ),
            )

            totalAntall shouldBe 3
            totalAntallUfiltrert shouldBe 3
            actual.size shouldBe 3
            actual.map { it.status } shouldBe listOf(
                BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                BehandlingssammendragStatus.KLAR_TIL_BESLUTNING,
            )

            // De to KLAR_TIL_BEHANDLING-radene har lik sorteringsnøkkel, så de plukkes på innhold i stedet for posisjon.
            actual.single { it.sakId == sakTilBehandling.id } shouldBe Behandlingssammendrag(
                sakId = sakTilBehandling.id,
                fnr = sakTilBehandling.fnr,
                saksnummer = sakTilBehandling.saksnummer,
                startet = sakTilBehandling.utbetalinger.first().opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.TILBAKEKREVING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = null,
                beslutter = null,
                sistEndret = tilBehandling.sistEndret,
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                resultat = null,
                erUnderkjent = false,
                beløp = tilBehandling.totaltFeilutbetaltBeløp,
                tilbakekrevingKilde = TilbakekrevingKilde.MELDEKORT,
            )

            actual.single { it.sakId == sakTilBehandlingVenter.id } shouldBe Behandlingssammendrag(
                sakId = sakTilBehandlingVenter.id,
                fnr = sakTilBehandlingVenter.fnr,
                saksnummer = sakTilBehandlingVenter.saksnummer,
                startet = sakTilBehandlingVenter.utbetalinger.first().opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.TILBAKEKREVING,
                status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                saksbehandler = null,
                beslutter = null,
                sistEndret = oppdatertTilBehandlingVenter.sistEndret,
                erSattPåVent = true,
                sattPåVentBegrunnelse = "AVVENTER_BRUKERUTTALELSE",
                sattPåVentFrist = 28.februar(2026),
                resultat = null,
                erUnderkjent = false,
                beløp = oppdatertTilBehandlingVenter.totaltFeilutbetaltBeløp,
                tilbakekrevingKilde = TilbakekrevingKilde.MELDEKORT,
            )

            actual.single { it.sakId == sakTilGodkjenning.id } shouldBe Behandlingssammendrag(
                sakId = sakTilGodkjenning.id,
                fnr = sakTilGodkjenning.fnr,
                saksnummer = sakTilGodkjenning.saksnummer,
                startet = sakTilGodkjenning.utbetalinger.first().opprettet,
                kravtidspunkt = null,
                behandlingstype = BehandlingssammendragType.TILBAKEKREVING,
                status = BehandlingssammendragStatus.KLAR_TIL_BESLUTNING,
                saksbehandler = null,
                beslutter = null,
                sistEndret = tilGodkjenning.sistEndret,
                erSattPåVent = false,
                sattPåVentBegrunnelse = null,
                sattPåVentFrist = null,
                resultat = null,
                erUnderkjent = false,
                beløp = tilGodkjenning.totaltFeilutbetaltBeløp,
                tilbakekrevingKilde = TilbakekrevingKilde.MELDEKORT,
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kan filtrere tilbakekrevinger på minstebeløp`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUnderMinstebeløp, tilbakekrevingUnderMinstebeløp) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            sendTilbakekrevingHendelseOgKjørJobb(
                tac = tac,
                sak = sakUnderMinstebeløp,
                tilbakeBehandlingId = tilbakekrevingUnderMinstebeløp.tilbakeBehandlingId,
                behandlingsstatus = "TIL_BEHANDLING",
                forrigeBehandlingsstatus = "TIL_BEHANDLING",
                totaltFeilutbetaltBeløp = BigDecimal(TilbakekrevingBehandling.MINSTEBELØP_FOR_TILBAKEKREVING - 1),
            )
            val (sakOverMinstebeløp, tilbakekrevingOverMinstebeløp) = opprettTilbakekrevingBehandlingTilBehandling(tac = tac)
            sendTilbakekrevingHendelseOgKjørJobb(
                tac = tac,
                sak = sakOverMinstebeløp,
                tilbakeBehandlingId = tilbakekrevingOverMinstebeløp.tilbakeBehandlingId,
                behandlingsstatus = "TIL_BEHANDLING",
                forrigeBehandlingsstatus = "TIL_BEHANDLING",
                totaltFeilutbetaltBeløp = BigDecimal(TilbakekrevingBehandling.MINSTEBELØP_FOR_TILBAKEKREVING),
            )
            val oppdatertOverMinstebeløp = tac.sakContext.sakRepo.hentForSakId(sakOverMinstebeløp.id)!!.tilbakekrevinger.single()

            val (actualUtenFilter, totalAntallUtenFilter, totalAntallUfiltrertUtenFilter) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntallUtenFilter shouldBe 2
            totalAntallUfiltrertUtenFilter shouldBe 2
            actualUtenFilter.size shouldBe 2

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(tilbakekrevingMinstebeløp = TilbakekrevingBehandling.MINSTEBELØP_FOR_TILBAKEKREVING),
            )

            totalAntall shouldBe 1
            totalAntallUfiltrert shouldBe 2
            actual shouldBe listOf(
                Behandlingssammendrag(
                    sakId = sakOverMinstebeløp.id,
                    fnr = sakOverMinstebeløp.fnr,
                    saksnummer = sakOverMinstebeløp.saksnummer,
                    startet = sakOverMinstebeløp.utbetalinger.first().opprettet,
                    kravtidspunkt = null,
                    behandlingstype = BehandlingssammendragType.TILBAKEKREVING,
                    status = BehandlingssammendragStatus.KLAR_TIL_BEHANDLING,
                    saksbehandler = null,
                    beslutter = null,
                    sistEndret = oppdatertOverMinstebeløp.sistEndret,
                    erSattPåVent = false,
                    sattPåVentBegrunnelse = null,
                    sattPåVentFrist = null,
                    resultat = null,
                    erUnderkjent = false,
                    beløp = oppdatertOverMinstebeløp.totaltFeilutbetaltBeløp,
                    tilbakekrevingKilde = TilbakekrevingKilde.MELDEKORT,
                ),
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter mix av behandlingene`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSakOgSøknad(tac = tac)
            opprettSøknadsbehandlingUnderBehandling(tac = tac)
            iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!
            opprettTilbakekrevingBehandlingTilBehandling(tac = tac)

            val (actual, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 6
            totalAntallUfiltrert shouldBe 6
            actual.size shouldBe 6
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kan filtrere basert på behandlingstype`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSakOgSøknad(tac = tac)
            opprettSøknadsbehandlingUnderBehandling(tac = tac)
            iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!

            val (actualSøknadsbehandlinger, totalAntallSøknadbehandlinger, totalAntallUfiltrertSøknadsbehandlinger) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(behandlingstype = listOf(BehandlingssammendragType.SØKNADSBEHANDLING)),
            )
            val (actualRevurderinger, totalAntallRevurderinger, totalAntallUfiltrertRevurderinger) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(behandlingstype = listOf(BehandlingssammendragType.REVURDERING)),
            )
            val (actualMeldekortbehandlinger, totalAntallMeldekortbehandlinger, totalAntallUfiltrertMeldekortbehandlinger) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(behandlingstype = listOf(BehandlingssammendragType.MELDEKORTBEHANDLING)),
            )

            actualSøknadsbehandlinger.size shouldBe 2
            totalAntallSøknadbehandlinger shouldBe 2
            totalAntallUfiltrertSøknadsbehandlinger shouldBe 4
            actualRevurderinger.size shouldBe 1
            totalAntallRevurderinger shouldBe 1
            totalAntallUfiltrertRevurderinger shouldBe 4
            actualMeldekortbehandlinger.size shouldBe 1
            totalAntallMeldekortbehandlinger shouldBe 1
            totalAntallUfiltrertMeldekortbehandlinger shouldBe 4
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kan filtrere basert på status`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSakOgSøknad(tac = tac)
            opprettSøknadsbehandlingUnderBehandling(tac = tac)
            sendSøknadsbehandlingTilBeslutning(tac = tac)
            val (sakUnderBeslutning, _, underBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taBehandling(tac, sakUnderBeslutning.id, underBeslutningId, ObjectMother.beslutter())

            iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)
            revurderingStansTilBeslutning(tac)
            val (sakRevurderingUnderBeslutning, revurderingUnderBeslutningId) = revurderingStansTilBeslutning(tac)
            taBehandling(tac, sakRevurderingUnderBeslutning.id, revurderingUnderBeslutningId, ObjectMother.beslutter())

            iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac = tac)!!

            val (actualKlarTilBehandling, _, totalAntallUfiltrertKlarTilBehandling) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(status = listOf(BehandlingssammendragStatus.KLAR_TIL_BEHANDLING)),
            )

            val (actualUnderBehandling, _, totalAntallUfiltrertUnderBehandling) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(status = listOf(BehandlingssammendragStatus.UNDER_BEHANDLING)),
            )

            val (actualKlarTilBeslutning, _, totalAntallUfiltrertKlarTilBeslutning) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(status = listOf(BehandlingssammendragStatus.KLAR_TIL_BESLUTNING)),
            )

            val (actualUnderBeslutning, _, totalAntallUfiltrertUnderBeslutning) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(status = listOf(BehandlingssammendragStatus.UNDER_BESLUTNING)),
            )

            actualKlarTilBehandling.size shouldBe 1
            totalAntallUfiltrertKlarTilBehandling shouldBe 9
            actualUnderBehandling.size shouldBe 3
            totalAntallUfiltrertUnderBehandling shouldBe 9
            actualKlarTilBeslutning.size shouldBe 3
            totalAntallUfiltrertKlarTilBeslutning shouldBe 9
            actualUnderBeslutning.size shouldBe 2
            totalAntallUfiltrertUnderBeslutning shouldBe 9
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kan filtrere basert på saksbehandler og beslutter`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val saksbehandlerOgBeslutter = ObjectMother.saksbehandlerOgBeslutter("Z999999")
            opprettSøknadsbehandlingUnderBehandling(tac = tac, saksbehandler = saksbehandlerOgBeslutter)
            sendSøknadsbehandlingTilBeslutning(tac = tac)
            val (sakUnderBeslutning, _, underBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taBehandling(tac, sakUnderBeslutning.id, underBeslutningId, saksbehandlerOgBeslutter)

            val (behandlingssamendrag, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(saksbehandlere = listOf(saksbehandlerOgBeslutter.navIdent)),
            )

            totalAntall shouldBe 2
            totalAntallUfiltrert shouldBe 3
            behandlingssamendrag.size shouldBe 2
            behandlingssamendrag.let {
                it.first().saksbehandler shouldBe saksbehandlerOgBeslutter.navIdent
                it.first().beslutter shouldBe null

                it.last().saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                it.last().beslutter shouldBe saksbehandlerOgBeslutter.navIdent
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter behandlinger som har saksbehandler eller beslutter ikke tildelt`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val saksbehandlerOgBeslutter = ObjectMother.saksbehandlerOgBeslutter("Z999999")
            opprettSøknadsbehandlingUnderBehandling(tac = tac, saksbehandler = saksbehandlerOgBeslutter)
            sendSøknadsbehandlingTilBeslutning(tac = tac)
            val (sakUnderBeslutning, _, underBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taBehandling(tac, sakUnderBeslutning.id, underBeslutningId, saksbehandlerOgBeslutter)

            val (behandlingssamendrag, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(saksbehandlere = listOf("IKKE_TILDELT")),
            )

            totalAntall shouldBe 2
            totalAntallUfiltrert shouldBe 3
            behandlingssamendrag.size shouldBe 2
            behandlingssamendrag.let {
                it.first().saksbehandler shouldBe saksbehandlerOgBeslutter.navIdent
                it.first().beslutter shouldBe null

                it.last().saksbehandler shouldBe ObjectMother.saksbehandler().navIdent
                it.last().beslutter shouldBe null
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter både behandlinger som er klar og på vent`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUnderBeslutning, _, underBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taBehandling(tac, sakUnderBeslutning.id, underBeslutningId, ObjectMother.beslutter())
            opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            val (behandlingssamendrag, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(),
            )

            totalAntall shouldBe 2
            totalAntallUfiltrert shouldBe 2
            behandlingssamendrag.size shouldBe 2
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `henter behandlinger som er klar`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUnderBeslutning, _, underBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taBehandling(tac, sakUnderBeslutning.id, underBeslutningId, ObjectMother.beslutter())
            opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            val (behandlingssamendrag, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(benktype = listOf(BehandlingssammendragBenktype.KLAR)),
            )

            totalAntall shouldBe 1
            totalAntallUfiltrert shouldBe 2
            behandlingssamendrag.size shouldBe 1
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kan filtrere på behandlinger som er satt på vent`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakUnderBeslutning, _, underBeslutningId, _) = sendSøknadsbehandlingTilBeslutning(tac = tac)
            taBehandling(tac, sakUnderBeslutning.id, underBeslutningId, ObjectMother.beslutter())
            opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            val (behandlingssamendrag, totalAntall, totalAntallUfiltrert) = tac.benkOversiktContext.benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(benktype = listOf(BehandlingssammendragBenktype.VENTER)),
            )

            totalAntall shouldBe 1
            totalAntallUfiltrert shouldBe 2
            behandlingssamendrag.size shouldBe 1
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kan sortere på startet`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val benkOversiktRepo = tac.benkOversiktContext.benkOversiktRepo
            val (sak1, _, _, _) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!
            val (sak2, _, _, _) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            val (actualAsc, _) = benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(
                    sortering = BenkSortering(BenkSorteringKolonne.STARTET, SorteringRetning.ASC),
                ),
            )
            val (actualDesc, _) = benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(
                    sortering = BenkSortering(BenkSorteringKolonne.STARTET, SorteringRetning.DESC),
                ),
            )

            actualAsc.let {
                it.first().sakId shouldBe sak1.id
                it.last().sakId shouldBe sak2.id
            }

            actualDesc.let {
                it.first().sakId shouldBe sak2.id
                it.last().sakId shouldBe sak1.id
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kan sortere på sist endret`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val benkOversiktRepo = tac.benkOversiktContext.benkOversiktRepo
            val (sak1, _, _, _) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!
            val (sak2, _, _, _) = opprettSøknadsbehandlingOgSettPåVent(tac = tac)!!

            val (actualAsc, _) = benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(
                    sortering = BenkSortering(BenkSorteringKolonne.SIST_ENDRET, SorteringRetning.ASC),
                ),
            )
            val (actualDesc, _) = benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(
                    sortering = BenkSortering(BenkSorteringKolonne.SIST_ENDRET, SorteringRetning.DESC),
                ),
            )

            actualAsc.let {
                it.first().sakId shouldBe sak1.id
                it.last().sakId shouldBe sak2.id
            }

            actualDesc.let {
                it.first().sakId shouldBe sak2.id
                it.last().sakId shouldBe sak1.id
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `kan sortere på frist`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val benkOversiktRepo = tac.benkOversiktContext.benkOversiktRepo
            val dagensDato = nå(tac.clock).toLocalDate()
            val (sak1, _, _, _) = opprettSøknadsbehandlingOgSettPåVent(tac = tac, frist = dagensDato.plusDays(2))!!
            val (sak2, _, _, _) = opprettSøknadsbehandlingOgSettPåVent(tac = tac, frist = dagensDato.plusDays(1))!!

            val (actualAsc, _) = benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(
                    sortering = BenkSortering(BenkSorteringKolonne.FRIST, SorteringRetning.ASC),
                ),
            )
            val (actualDesc, _) = benkOversiktRepo.hentÅpneBehandlinger(
                newCommand(
                    sortering = BenkSortering(BenkSorteringKolonne.FRIST, SorteringRetning.DESC),
                ),
            )

            // Stigende frist betyr nærmeste frist først; sak2 har kortest frist.
            actualAsc.let {
                it.first().sakId shouldBe sak2.id
                it.last().sakId shouldBe sak1.id
            }

            actualDesc.let {
                it.first().sakId shouldBe sak1.id
                it.last().sakId shouldBe sak2.id
            }
        }
    }
}

private fun TestApplicationContextMedPostgres.verifiserViHarNMeldekortbehandlinger(antall: Int) {
    sessionFactory.withSession { session ->
        session.run(
            queryOf("SELECT COUNT(*) FROM meldekortbehandling", emptyMap()).map {
                it.int(1)
            }.asSingle,
        ) shouldBe antall
    }
}
