package no.nav.tiltakspenger.saksbehandling.benk.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOppsummering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversiktMedTilgang
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkPersonmarkører
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRad
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRepo
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRespons
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentBenkKommando
import no.nav.tiltakspenger.saksbehandling.benk.domene.KunneIkkeHenteBenk

/**
 * Henter én fane av benken og beriker alle radene med tilgang og personmarkører.
 * Tilgangen slås opp i ett bulkkall mot Tilgangsmaskinen for de unike personene på siden.
 * Rader uten tilgang blir med med mindre filteret `skjulUtenTilgang` er satt; ellers er det DTO-laget som sladder dem.
 * Loggingen av bulkkallet skjer i [no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService], som kjenner saksbehandleren og correlationId-en.
 */
class BenkService(
    private val benkRepo: BenkRepo,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    suspend fun hentSøknader(
        kommando: HentBenkKommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkRespons<BenkSøknadsbehandling>> = hentFane(kommando, saksbehandlerToken) { limit, offset ->
        benkRepo.hentSøknader(kommando, limit = limit, offset = offset)
    }

    suspend fun hentRevurderinger(
        kommando: HentBenkKommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkRespons<BenkRevurdering>> = hentFane(kommando, saksbehandlerToken) { limit, offset ->
        benkRepo.hentRevurderinger(kommando, limit = limit, offset = offset)
    }

    suspend fun hentMeldekort(
        kommando: HentBenkKommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkRespons<BenkMeldekort>> = hentFane(kommando, saksbehandlerToken) { limit, offset ->
        benkRepo.hentMeldekort(kommando, limit = limit, offset = offset)
    }

    suspend fun hentKlager(
        kommando: HentBenkKommando<BenkKlageFiltrering, BenkKlageKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkRespons<BenkKlagebehandling>> = hentFane(kommando, saksbehandlerToken) { limit, offset ->
        benkRepo.hentKlager(kommando, limit = limit, offset = offset)
    }

    suspend fun hentTilbakekrevinger(
        kommando: HentBenkKommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkRespons<BenkTilbakekreving>> = hentFane(kommando, saksbehandlerToken) { limit, offset ->
        benkRepo.hentTilbakekrevinger(kommando, limit = limit, offset = offset)
    }

    private suspend fun <T : BenkBehandling> hentFane(
        kommando: HentBenkKommando<*, *>,
        saksbehandlerToken: String,
        hent: (limit: Int, offset: Int) -> BenkOversikt<T>,
    ): Either<KunneIkkeHenteBenk, BenkRespons<T>> = either {
        val antallPerFane = benkRepo.hentAntallPerFane()
        val oversikt = hent(kommando.paginering.limit(), kommando.paginering.offset())

        val fnrs = oversikt.fødselsnummere().toNonEmptyListOrNull()
            ?: return@either BenkRespons(
                antallPerFane = antallPerFane,
                oversikt = BenkOversiktMedTilgang(
                    rader = emptyList(),
                    totalAntall = oversikt.totalAntall,
                    totalAntallUfiltrert = oversikt.totalAntallUfiltrert,
                    oppsummering = BenkOppsummering.fra(emptyList<BenkRad<T>>()),
                    saksbehandlere = oversikt.saksbehandlere,
                    besluttere = oversikt.besluttere,
                    side = kommando.paginering.side,
                ),
            )

        val tilganger = tilgangskontrollService.harTilgangTilPersoner(
            fnrs = fnrs,
            saksbehandlerToken = saksbehandlerToken,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ).mapLeft { KunneIkkeHenteBenk.Tilgangskontroll }.bind()

        // Nøkkelsettet er garantert av bulksvarets egen validering, så oppslaget kan ikke bomme.
        val alleRader = oversikt.behandlinger.map { behandling ->
            val tilgang = tilganger.getValue(behandling.fnr)
            BenkRad(
                behandling = behandling,
                tilgang = tilgang,
                personmarkører = BenkPersonmarkører.fra(tilgang),
            )
        }
        // Tilgangen er først kjent nå, så filteret kan ikke være en del av spørringen.
        // Siden kan derfor få færre rader enn sideantallet, og totalAntall teller fortsatt radene før tilgangsfiltreringen.
        val rader = if (kommando.filtrering.skjulUtenTilgang) {
            alleRader.filter { it.tilgang is TilgangsvurderingBulk.Godkjent }
        } else {
            alleRader
        }
        val oppsummering = BenkOppsummering.fra(rader)

        BenkRespons(
            antallPerFane = antallPerFane,
            oversikt = BenkOversiktMedTilgang(
                rader = rader,
                totalAntall = oversikt.totalAntall,
                totalAntallUfiltrert = oversikt.totalAntallUfiltrert,
                oppsummering = oppsummering,
                saksbehandlere = oversikt.saksbehandlere,
                besluttere = oversikt.besluttere,
                side = kommando.paginering.side,
            ),
        )
    }
}
