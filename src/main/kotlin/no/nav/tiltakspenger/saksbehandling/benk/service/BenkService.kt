package no.nav.tiltakspenger.saksbehandling.benk.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkFiltrering
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
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
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
import no.nav.tiltakspenger.saksbehandling.person.AdressebeskyttelseOgSkjerming
import no.nav.tiltakspenger.saksbehandling.person.AdressebeskyttelseOgSkjermingService

/**
 * Henter én fane av benken og beriker alle radene med tilgang og personmarkører.
 * Tilgangen slås opp i ett bulkkall mot Tilgangsmaskinen for de unike personene på siden.
 * Rader uten tilgang blir med i svaret; det er DTO-laget som sladder dem.
 * Filteret på adressebeskyttelse og skjerming slår opp personene i fanen og avgrenser spørringen til treffene.
 * Filtreringen skjer i databasen, så totalene og pagineringen stemmer.
 * Loggingen av bulkkallet skjer i [no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService], som kjenner saksbehandleren og correlationId-en.
 */
class BenkService(
    private val benkRepo: BenkRepo,
    private val tilgangskontrollService: TilgangskontrollService,
    private val adressebeskyttelseOgSkjermingService: AdressebeskyttelseOgSkjermingService,
) {
    suspend fun hentSøknader(
        kommando: HentBenkKommando<BenkSøknaderFiltrering, BenkSøknaderKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkSøknadsbehandling>> =
        hentFane(kommando, BenkFane.SØKNADER, saksbehandlerToken) { avgrenset, limit, offset ->
            benkRepo.hentSøknader(avgrenset, limit = limit, offset = offset)
        }

    suspend fun hentRevurderinger(
        kommando: HentBenkKommando<BenkRevurderingerFiltrering, BenkRevurderingerKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkRevurdering>> =
        hentFane(kommando, BenkFane.REVURDERINGER, saksbehandlerToken) { avgrenset, limit, offset ->
            benkRepo.hentRevurderinger(avgrenset, limit = limit, offset = offset)
        }

    suspend fun hentMeldekort(
        kommando: HentBenkKommando<BenkMeldekortFiltrering, BenkMeldekortKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkMeldekort>> =
        hentFane(kommando, BenkFane.MELDEKORT, saksbehandlerToken) { avgrenset, limit, offset ->
            benkRepo.hentMeldekort(avgrenset, limit = limit, offset = offset)
        }

    suspend fun hentKlager(
        kommando: HentBenkKommando<BenkKlageFiltrering, BenkKlageKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkKlagebehandling>> =
        hentFane(kommando, BenkFane.KLAGE, saksbehandlerToken) { avgrenset, limit, offset ->
            benkRepo.hentKlager(avgrenset, limit = limit, offset = offset)
        }

    suspend fun hentTilbakekrevinger(
        kommando: HentBenkKommando<BenkTilbakekrevingFiltrering, BenkTilbakekrevingKolonne>,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<BenkTilbakekreving>> =
        hentFane(kommando, BenkFane.TILBAKEKREVING, saksbehandlerToken) { avgrenset, limit, offset ->
            benkRepo.hentTilbakekrevinger(avgrenset, limit = limit, offset = offset)
        }

    /**
     * Seksjonene i mine-fanen deler ett bulkkall mot Tilgangsmaskinen, slik at fanen ikke koster ett kall per seksjon.
     * Seksjonene pagineres ikke, så filteret på adressebeskyttelse og skjerming avgrenser radene i minnet, og bare personene i seksjonene slås opp.
     */
    suspend fun hentMine(
        kommando: HentMineKommando,
        saksbehandlerToken: String,
    ): Either<KunneIkkeHenteBenk, BenkMineResponsMedTilgang> = either {
        val antallPerFane = benkRepo.hentAntallPerFane(kommando.saksbehandler.navIdent)
        val alleSeksjoner = benkRepo.hentMine(kommando)
        val beskyttelser = if (kommando.kunAdressebeskyttetEllerSkjermet) {
            hentAdressebeskyttelseOgSkjerming(alleSeksjoner.values.flatMap { it.fødselsnummere() }, kommando).bind()
        } else {
            null
        }
        val seksjoner = beskyttelser?.beskyttede()?.let { personer ->
            alleSeksjoner.mapValues { (_, oversikt) -> oversikt.avgrensTil(personer) }
        } ?: alleSeksjoner
        val tilganger = hentTilganger(seksjoner.values.flatMap { it.fødselsnummere() }, kommando, saksbehandlerToken).bind()

        BenkMineResponsMedTilgang(
            antallPerFane = antallPerFane,
            seksjoner = seksjoner.mapValues { (_, oversikt) ->
                oversikt.medTilgang(tilganger, side = 0, beskyttelser)
            },
        )
    }

    /**
     * Svaret til en bruker uten benkrolle.
     * Ruten svarer med dette før fanespørringen, så det ikke gjøres databaseoppslag eller tilgangskall.
     */
    fun tomRespons(): BenkResponsUtenTilgang = BenkResponsUtenTilgang

    private suspend fun <F : BenkFiltrering, K : BenkSorteringKolonne, T : BenkBehandling> hentFane(
        kommando: HentBenkKommando<F, K>,
        fane: BenkFane,
        saksbehandlerToken: String,
        hent: (kommando: HentBenkKommando<F, K>, limit: Int, offset: Int) -> BenkOversikt<T>,
    ): Either<KunneIkkeHenteBenk, BenkResponsMedTilgang<T>> = either {
        val antallPerFane = benkRepo.hentAntallPerFane(kommando.saksbehandler.navIdent)
        val beskyttelser = if (kommando.filtrering.kunAdressebeskyttetEllerSkjermet) {
            hentAdressebeskyttelseOgSkjerming(benkRepo.hentPersoner(fane), kommando).bind()
        } else {
            null
        }
        val avgrenset = beskyttelser?.let { kommando.avgrensTil(it.beskyttede()) } ?: kommando
        val oversikt = hent(avgrenset, kommando.paginering.limit(), kommando.paginering.offset())
        val tilganger = hentTilganger(oversikt.fødselsnummere(), kommando, saksbehandlerToken).bind()

        BenkResponsMedTilgang(
            antallPerFane = antallPerFane,
            oversikt = oversikt.medTilgang(tilganger, kommando.paginering.side, beskyttelser),
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

    private suspend fun hentAdressebeskyttelseOgSkjerming(
        fnrs: List<Fnr>,
        kommando: ServiceCommand,
    ): Either<KunneIkkeHenteBenk, Map<Fnr, AdressebeskyttelseOgSkjerming>> =
        adressebeskyttelseOgSkjermingService.hent(fnrs, kommando.correlationId)
            .mapLeft { KunneIkkeHenteBenk.AdressebeskyttelseOgSkjerming(it) }

    private fun Map<Fnr, AdressebeskyttelseOgSkjerming>.beskyttede(): Set<Fnr> =
        filterValues { it.harAdressebeskyttelseEllerSkjerming }.keys

    /**
     * Beriker radene med tilgang og personmarkører.
     * Nøkkelsettet i [tilganger] er garantert av bulksvarets egen validering, så oppslaget kan ikke bomme.
     * Når [beskyttelser] er satt, er radene avgrenset til personene i oppslaget, så hver rad har en oppføring der.
     */
    private fun <T : BenkBehandling> BenkOversikt<T>.medTilgang(
        tilganger: Map<Fnr, TilgangsvurderingBulk>,
        side: Int,
        beskyttelser: Map<Fnr, AdressebeskyttelseOgSkjerming>?,
    ): BenkOversiktMedTilgang<T> {
        val rader = behandlinger.map { behandling ->
            val tilgang = tilganger.getValue(behandling.fnr)
            BenkRad(
                behandling = behandling,
                tilgang = tilgang,
                personmarkører = BenkPersonmarkører.fra(tilgang, beskyttelser?.getValue(behandling.fnr)),
            )
        }

        return BenkOversiktMedTilgang(
            rader = rader,
            totalAntall = totalAntall,
            totalAntallUfiltrert = totalAntallUfiltrert,
            oppsummering = BenkOppsummering.fra(rader),
            saksbehandlere = saksbehandlere,
            besluttere = besluttere,
            side = side,
        )
    }
}
