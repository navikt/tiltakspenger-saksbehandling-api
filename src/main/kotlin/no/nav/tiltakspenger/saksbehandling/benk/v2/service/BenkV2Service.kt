package no.nav.tiltakspenger.saksbehandling.benk.v2.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Filtrering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Oversikt
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Repo
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2SorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.HentBenkV2Kommando

/**
 * Henter én fane av benken, og tar bort radene saksbehandler ikke har tilgang til.
 *
 * Tilgangen avgjøres per person, ikke per behandling, så oppslaget gjøres én gang for de unike fødselsnumrene i fanen.
 * Er tilgangen ukjent, filtreres raden bort: benken skal ikke vise en person vi ikke fikk avklart tilgangen til.
 */
class BenkV2Service(
    private val benkV2Repo: BenkV2Repo,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun hentSøknader(
        command: HentBenkV2Kommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        saksbehandlerToken: String,
    ): BenkV2Respons<BenkSøknadsbehandling> = hentFane(command, saksbehandlerToken) { benkV2Repo.hentSøknader(it) }

    suspend fun hentRevurderinger(
        command: HentBenkV2Kommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        saksbehandlerToken: String,
    ): BenkV2Respons<BenkRevurdering> = hentFane(command, saksbehandlerToken) { benkV2Repo.hentRevurderinger(it) }

    suspend fun hentMeldekort(
        command: HentBenkV2Kommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        saksbehandlerToken: String,
    ): BenkV2Respons<BenkMeldekort> = hentFane(command, saksbehandlerToken) { benkV2Repo.hentMeldekort(it) }

    suspend fun hentKlager(
        command: HentBenkV2Kommando<BenkKlageFiltrering, BenkKlageKolonne>,
        saksbehandlerToken: String,
    ): BenkV2Respons<BenkKlagebehandling> = hentFane(command, saksbehandlerToken) { benkV2Repo.hentKlager(it) }

    suspend fun hentTilbakekrevinger(
        command: HentBenkV2Kommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        saksbehandlerToken: String,
    ): BenkV2Respons<BenkTilbakekreving> = hentFane(command, saksbehandlerToken) { benkV2Repo.hentTilbakekrevinger(it) }

    private suspend fun <F : BenkV2Filtrering, K : BenkV2SorteringKolonne, T : BenkV2Behandling> hentFane(
        command: HentBenkV2Kommando<F, K>,
        saksbehandlerToken: String,
        hent: (HentBenkV2Kommando<F, K>) -> BenkV2Oversikt<T>,
    ): BenkV2Respons<T> {
        val antallPerFane = benkV2Repo.hentAntallPerFane()
        val oversikt = hent(command)

        if (oversikt.isEmpty()) {
            return BenkV2Respons(
                antallPerFane = antallPerFane,
                oversikt = TilgangsfiltrertBenkV2Oversikt(
                    behandlinger = emptyList(),
                    totalAntall = oversikt.totalAntall,
                    totalAntallUfiltrert = oversikt.totalAntallUfiltrert,
                    antallFiltrertPgaTilgang = 0,
                ),
            )
        }

        val tilganger = tilgangskontrollService.harTilgangTilPersoner(
            fnrs = oversikt.fødselsnummere(),
            saksbehandlerToken = saksbehandlerToken,
            saksbehandler = command.saksbehandler,
        )

        val medTilgang = oversikt.filtrer { harTilgang(it.fnr, tilganger, command.saksbehandler) }

        return BenkV2Respons(
            antallPerFane = antallPerFane,
            oversikt = TilgangsfiltrertBenkV2Oversikt(
                behandlinger = medTilgang.behandlinger,
                totalAntall = oversikt.totalAntall,
                totalAntallUfiltrert = oversikt.totalAntallUfiltrert,
                antallFiltrertPgaTilgang = oversikt.behandlinger.size - medTilgang.behandlinger.size,
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
