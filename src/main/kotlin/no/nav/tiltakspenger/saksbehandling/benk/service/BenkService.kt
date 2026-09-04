package no.nav.tiltakspenger.saksbehandling.benk.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRepo
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentBenkKommando

/**
 * Henter én fane av benken, og tar bort radene saksbehandler ikke har tilgang til.
 *
 * Tilgangen avgjøres per person, ikke per behandling, så oppslaget gjøres én gang for de unike fødselsnumrene i fanen.
 * Er tilgangen ukjent, filtreres raden bort: benken skal ikke vise en person vi ikke fikk avklart tilgangen til.
 */
class BenkService(
    private val benkRepo: BenkRepo,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun hentSøknader(
        command: HentBenkKommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        saksbehandlerToken: String,
    ): BenkRespons<BenkSøknadsbehandling> = hentFane(command, saksbehandlerToken) { c, limit, offset ->
        benkRepo.hentSøknader(c, limit = limit, offset = offset)
    }

    suspend fun hentRevurderinger(
        command: HentBenkKommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        saksbehandlerToken: String,
    ): BenkRespons<BenkRevurdering> = hentFane(command, saksbehandlerToken) { c, limit, offset ->
        benkRepo.hentRevurderinger(c, limit = limit, offset = offset)
    }

    suspend fun hentMeldekort(
        command: HentBenkKommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        saksbehandlerToken: String,
    ): BenkRespons<BenkMeldekort> = hentFane(command, saksbehandlerToken) { c, limit, offset ->
        benkRepo.hentMeldekort(c, limit = limit, offset = offset)
    }

    suspend fun hentKlager(
        command: HentBenkKommando<BenkKlageFiltrering, BenkKlageKolonne>,
        saksbehandlerToken: String,
    ): BenkRespons<BenkKlagebehandling> = hentFane(command, saksbehandlerToken) { c, limit, offset ->
        benkRepo.hentKlager(c, limit = limit, offset = offset)
    }

    suspend fun hentTilbakekrevinger(
        command: HentBenkKommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        saksbehandlerToken: String,
    ): BenkRespons<BenkTilbakekreving> = hentFane(command, saksbehandlerToken) { c, limit, offset ->
        benkRepo.hentTilbakekrevinger(c, limit = limit, offset = offset)
    }

    private suspend fun <F : BenkFiltrering, K : BenkSorteringKolonne, T : BenkBehandling> hentFane(
        command: HentBenkKommando<F, K>,
        saksbehandlerToken: String,
        hent: (HentBenkKommando<F, K>, Int, Int) -> BenkOversikt<T>,
    ): BenkRespons<T> {
        val antallPerFane = benkRepo.hentAntallPerFane()
        val oversikt = hent(command, command.paginering.limit(), command.paginering.offset())

        if (oversikt.isEmpty()) {
            return BenkRespons(
                antallPerFane = antallPerFane,
                oversikt = TilgangsfiltrertBenkOversikt(
                    behandlinger = emptyList(),
                    totalAntall = oversikt.totalAntall,
                    totalAntallUfiltrert = oversikt.totalAntallUfiltrert,
                    antallFiltrertPgaTilgang = 0,
                    saksbehandlere = oversikt.saksbehandlere,
                    besluttere = oversikt.besluttere,
                    side = command.paginering.side,
                ),
            )
        }

        val tilganger = tilgangskontrollService.harTilgangTilPersoner(
            fnrs = oversikt.fødselsnummere(),
            saksbehandlerToken = saksbehandlerToken,
            saksbehandler = command.saksbehandler,
        )

        val medTilgang = oversikt.filtrer { harTilgang(it.fnr, tilganger, command.saksbehandler) }

        return BenkRespons(
            antallPerFane = antallPerFane,
            oversikt = TilgangsfiltrertBenkOversikt(
                behandlinger = medTilgang.behandlinger,
                totalAntall = oversikt.totalAntall,
                totalAntallUfiltrert = oversikt.totalAntallUfiltrert,
                antallFiltrertPgaTilgang = oversikt.behandlinger.size - medTilgang.behandlinger.size,
                saksbehandlere = oversikt.saksbehandlere,
                besluttere = oversikt.besluttere,
                side = command.paginering.side,
            ),
        )
    }

    private fun harTilgang(fnr: Fnr, tilganger: Map<Fnr, Boolean>, saksbehandler: Saksbehandler): Boolean {
        val harTilgang = tilganger[fnr]
        if (harTilgang == null) {
            logger.debug { "tilgangsstyring: Filtrerte vekk bruker fra benk v2 for saksbehandler $saksbehandler. Kunne ikke avgjøre om hen har tilgang. Se sikkerlogg for mer kontekst." }
            Sikkerlogg.debug { "tilgangsstyring: Filtrerte vekk bruker ${fnr.verdi} fra benk v2 for saksbehandler $saksbehandler. Kunne ikke avgjøre om hen har tilgang." }
        }
        if (harTilgang == false) {
            logger.debug { "tilgangsstyring: Filtrerte vekk bruker fra benk v2 for saksbehandler $saksbehandler. Saksbehandler har ikke tilgang. Se sikkerlogg for mer kontekst." }
            Sikkerlogg.debug { "tilgangsstyring: Filtrerte vekk bruker ${fnr.verdi} fra benk v2 for saksbehandler $saksbehandler. Saksbehandler har ikke tilgang." }
        }
        return harTilgang == true
    }
}
