package no.nav.tiltakspenger.saksbehandling.benk.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortFiltrering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMineResponsMedTilgang
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOppsummering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversiktMedTilgang
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkPersonmarkører
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRad
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRepo
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkResponsMedTilgang
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkResponsUtenTilgang
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
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentMineKommando
import no.nav.tiltakspenger.saksbehandling.benk.domene.KunneIkkeHenteBenk
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

/**
 * Henter én fane av benken og beriker alle radene med tilgang og personmarkører.
 * Tilgangen slås opp i ett bulkkall mot Tilgangsmaskinen for de unike personene på siden.
 * Rader uten tilgang blir med i svaret; det er DTO-laget som sladder dem.
 * Loggingen av bulkkallet skjer i [no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService], som kjenner saksbehandleren og correlationId-en.
 */
class BenkService(
    private val benkRepo: BenkRepo,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    suspend fun hentSøknader(
        kommando: HentBenkKommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkSøknadsbehandling>> =
        hentFane(kommando, saksbehandlerToken) { limit, offset ->
            benkRepo.hentSøknader(kommando, limit = limit, offset = offset)
        }

    suspend fun hentRevurderinger(
        kommando: HentBenkKommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkRevurdering>> =
        hentFane(kommando, saksbehandlerToken) { limit, offset ->
            benkRepo.hentRevurderinger(kommando, limit = limit, offset = offset)
        }

    suspend fun hentMeldekort(
        kommando: HentBenkKommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkMeldekort>> =
        hentFane(kommando, saksbehandlerToken) { limit, offset ->
            benkRepo.hentMeldekort(kommando, limit = limit, offset = offset)
        }

    suspend fun hentKlager(
        kommando: HentBenkKommando<BenkKlageFiltrering, BenkKlageKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkKlagebehandling>> =
        hentFane(kommando, saksbehandlerToken) { limit, offset ->
            benkRepo.hentKlager(kommando, limit = limit, offset = offset)
        }

    suspend fun hentTilbakekrevinger(
        kommando: HentBenkKommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkTilbakekreving>> =
        hentFane(kommando, saksbehandlerToken) { limit, offset ->
            benkRepo.hentTilbakekrevinger(kommando, limit = limit, offset = offset)
        }

    /**
     * Seksjonene i mine-fanen deler ett bulkkall mot Tilgangsmaskinen, slik at fanen ikke koster ett kall per seksjon.
     */
    suspend fun hentMine(
        kommando: HentMineKommando,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkMineResponsMedTilgang> = either {
        val antallPerFane = benkRepo.hentAntallPerFane(kommando.saksbehandler.navIdent)
        val seksjoner = benkRepo.hentMine(kommando)
        val tilganger = hentTilganger(seksjoner.values.flatMap { it.fødselsnummere() }, kommando, saksbehandlerToken).bind()

        BenkMineResponsMedTilgang(
            antallPerFane = antallPerFane,
            seksjoner = seksjoner.mapValues { (_, oversikt) ->
                oversikt.medTilgang(tilganger, kommando.skjulUtenTilgang, side = 0)
            },
        )
    }

    /**
     * Svaret til en bruker uten benkrolle.
     * Ruten svarer med dette før fanespørringen, så det ikke gjøres databaseoppslag eller tilgangskall.
     */
    fun tomRespons(): BenkResponsUtenTilgang = BenkResponsUtenTilgang

    private suspend fun <T : BenkBehandling> hentFane(
        kommando: HentBenkKommando<*, *>,
        saksbehandlerToken: String,
        hent: (limit: Int, offset: Int) -> BenkOversikt<T>,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<T>> = either {
        val antallPerFane = benkRepo.hentAntallPerFane(kommando.saksbehandler.navIdent)
        val oversikt = hent(kommando.paginering.limit(), kommando.paginering.offset())
        val tilganger = hentTilganger(oversikt.fødselsnummere(), kommando, saksbehandlerToken).bind()

        BenkResponsMedTilgang(
            antallPerFane = antallPerFane,
            oversikt = oversikt.medTilgang(tilganger, kommando.filtrering.skjulUtenTilgang, kommando.paginering.side),
        )
    }

    /** Uten personer å slå opp gjøres det ikke noe kall, og svaret er et tomt oppslag. */
    private suspend fun hentTilganger(
        fnrs: List<Fnr>,
        kommando: ServiceCommand,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, Map<Fnr, TilgangsvurderingBulk>> {
        val unike = fnrs.distinct().sortedBy { it.verdi }.toNonEmptyListOrNull() ?: return emptyMap<Fnr, TilgangsvurderingBulk>().right()
        return tilgangskontrollService.harTilgangTilPersoner(
            fnrs = unike,
            saksbehandlerToken = saksbehandlerToken,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ).mapLeft { KunneIkkeHenteBenk.Tilgangskontroll }
    }

    /**
     * Beriker radene med tilgang og personmarkører.
     * Nøkkelsettet i [tilganger] er garantert av bulksvarets egen validering, så oppslaget kan ikke bomme.
     *
     * Tilgangen er først kjent nå, så [skjulUtenTilgang] kan ikke være en del av spørringen.
     * Siden kan derfor få færre rader enn sideantallet, og totalAntall trekker fra radene som ble tatt bort her.
     */
    private fun <T : BenkBehandling> BenkOversikt<T>.medTilgang(
        tilganger: Map<Fnr, TilgangsvurderingBulk>,
        skjulUtenTilgang: Boolean,
        side: Int,
    ): BenkOversiktMedTilgang<T> {
        val alleRader = behandlinger.map { behandling ->
            val tilgang = tilganger.getValue(behandling.fnr)
            BenkRad(
                behandling = behandling,
                tilgang = tilgang,
                personmarkører = BenkPersonmarkører.fra(tilgang),
            )
        }
        val oppsummering = BenkOppsummering.fra(alleRader)

        BenkResponsMedTilgang(
            antallPerFane = antallPerFane,
            oversikt = BenkOversiktMedTilgang(
                rader = alleRader,
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
