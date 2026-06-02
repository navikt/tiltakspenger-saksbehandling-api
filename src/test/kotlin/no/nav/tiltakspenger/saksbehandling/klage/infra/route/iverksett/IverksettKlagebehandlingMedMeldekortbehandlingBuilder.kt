package no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett

import arrow.core.Tuple4
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.klageinstanshendelse.mottaHendelseFraKlageinstansen
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortvedtakOgOpprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.util.UUID

/**
 * Klagebehandling opprettholdelse + meldekortbehandling iverksett-flyt.
 *
 * Meldekortbehandling iverksett route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.iverksettMeldekortRoute]
 */
interface IverksettKlagebehandlingMedMeldekortbehandlingBuilder {

    /**
     * 1. Iverksetter søknadsbehandling og en første meldekortbehandling (vedtaket brukes som formkrav i klagebehandlingen)
     * 2. Starter klagebehandling med vedtakDetKlagesPå = meldekortvedtak
     * 3. Oppdaterer brevtekst
     * 4. Opprettholder (emulerer journalføring, distribuering av vedtaksbrev og oversendelse til klageinstansen)
     * 5. Mottar MEDHOLD fra klageinstansen → klageinstansen omgjør NAVs vedtak → klagebehandling vil gå til OMGJØRING_ETTER_KLAGEINSTANS når ny behandling opprettes
     * 6. Oppretter meldekortbehandling fra klage (type MELDEKORTBEHANDLING) → klagebehandling status blir OMGJØRING_ETTER_KLAGEINSTANS
     * 7. Behandler meldekortbehandlingen (oppdater → send til beslutning → ta)
     * 8. Beslutter iverksetter meldekortbehandlingen → klagebehandling iverksettes (status VEDTATT)
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgIverksettKlagebehandlingOpprettholdelseMedMeldekortbehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
    ): Tuple4<Sak, Meldekortvedtak, MeldekortbehandlingManuell, Klagebehandling>? {
        val (sak, klagebehandling, _) = iverksettMeldekortvedtakOgOpprettholdKlagebehandling(
            tac = tac,
            fnr = fnr,
            saksbehandlerKlagebehandling = saksbehandler,
        ) ?: return null

        // 5: Motta MEDHOLD fra klageinstansen → klagebehandling status blir MOTTATT_FRA_KLAGEINSTANS
        // (og vil gå til OMGJØRING_ETTER_KLAGEINSTANS når opprettMeldekortbehandlingForKlage kalles)
        tac.mottaHendelseFraKlageinstansen(
            GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                kabalReferanse = UUID.randomUUID().toString(),
                avsluttetTidspunkt = nå(tac.clock).toString(),
                utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD,
                journalpostReferanser = emptyList(),
            ),
        )
        tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()

        // Status er nå MOTTATT_FRA_KLAGEINSTANS; den transisjonerer til OMGJØRING_ETTER_KLAGEINSTANS
        // når opprettMeldekortbehandlingForKlage kalles (via oppdaterBehandlingId i domenet)
        val sakEtterKA = tac.sakContext.sakRepo.hentForSakId(sak.id)!!

        // 6: Opprett meldekortbehandling fra klage (dette transisjonerer klagebehandling til OMGJØRING_ETTER_KLAGEINSTANS)
        val førstKjede = sakEtterKA.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first()
        val (_, meldekortUnderBehandling, _) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            kjedeId = førstKjede.kjedeId,
            saksbehandler = saksbehandler,
        ) ?: return null

        // 7a: Oppdater meldekortbehandlingen (allerede UNDER_BEHANDLING etter opprettelse fra klage)
        oppdaterMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortUnderBehandling.id,
            saksbehandler = saksbehandler,
        ) ?: return null

        // 7b: Send til beslutning
        val (_, meldekortTilBeslutning, _) = sendMeldekortbehandlingTilBeslutning(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortUnderBehandling.id,
            saksbehandler = saksbehandler,
        ) ?: return null

        // 7c: Beslutter tar behandlingen
        taMeldekortbehanding(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortTilBeslutning.id,
            saksbehandlerEllerBeslutter = beslutter,
        ) ?: return null

        // 8: Iverksett meldekortbehandlingen (iverksetter også klagebehandlingen)
        val (oppdatertSak, meldekortvedtak, iverksattMeldekort, _) = iverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortTilBeslutning.id,
            beslutter = beslutter,
        ) ?: return null

        val iverksattKlagebehandling = oppdatertSak.hentKlagebehandling(klagebehandling.id)

        return Tuple4(
            oppdatertSak,
            meldekortvedtak,
            iverksattMeldekort,
            iverksattKlagebehandling,
        )
    }
}
