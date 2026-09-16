package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.httpklient.loggSuksess
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException

class TilgangskontrollService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val sakService: SakService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
        saksbehandler: Saksbehandler,
    ) {
        val vurdering = tilgangsmaskinClient.harTilgangTilPerson(fnr, saksbehandlerToken).getOrElse { feil ->
            håndterTilgangskontrollFeil(feil, saksbehandler, "Saksbehandler ${saksbehandler.navIdent}")
        }

        when (vurdering) {
            is Tilgangsvurdering.Avvist -> throw TilgangException(
                vurdering.årsak.toTilgangsnektårsak(),
                "Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person: ${vurdering.begrunnelse}",
            )

            Tilgangsvurdering.Godkjent -> Unit
        }
    }

    suspend fun harTilgangTilPersonForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        try {
            val fnr = sakService.hentFnrForSakId(sakId)
            harTilgangTilPerson(fnr, saksbehandlerToken, saksbehandler)
        } catch (tilgangException: TilgangException) {
            throw tilgangException
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for person for sakId $sakId: ${e.message}" }
            throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent}: ${e.message}}")
        }
    }

    suspend fun harTilgangTilPersonerForSakIder(
        sakIder: NonEmptySet<SakId>,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        sakIder.forEach {
            try {
                val fnr = sakService.hentFnrForSakId(it)
                harTilgangTilPerson(fnr, saksbehandlerToken, saksbehandler)
            } catch (tilgangException: TilgangException) {
                throw tilgangException
            } catch (e: Exception) {
                log.error { "Noe gikk galt ved sjekk av tilgang for person for sakId $it: ${e.message}" }
                throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent}: ${e.message}}")
            }
        }
    }

    suspend fun harTilgangTilPersonForSaksnummer(
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        try {
            val fnr = sakService.hentFnrForSaksnummer(saksnummer)
            harTilgangTilPerson(fnr, saksbehandlerToken, saksbehandler)
        } catch (tilgangException: TilgangException) {
            throw tilgangException
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for person for saksnummer $saksnummer: ${e.message}" }
            throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent}: ${e.message}}")
        }
    }

    /**
     * Bulkkallet gir én logglinje, positiv eller negativ.
     * Linja skrives her fordi det er dette laget som kjenner både saksbehandleren og correlationId-en.
     * [loggSuksess] skriver rå request og respons til sikkerloggen, så fnr og avvisningskoder per person finnes der uten en egen linje.
     */
    suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<TilgangskontrollFeil, Map<Fnr, TilgangsvurderingBulk>> {
        val kontekst = "Saksbehandler ${saksbehandler.navIdent}, ${fnrs.size} personer, correlationId $correlationId"
        return tilgangsmaskinClient.harTilgangTilPersoner(fnrs, saksbehandlerToken)
            .onLeft { loggTilgangskontrollFeil(it, kontekst) }
            .map { respons ->
                val antallAvvist = respons.body.values.count { it is TilgangsvurderingBulk.Avvist }
                respons.loggSuksess(
                    log,
                    "Tilgangskontroll i bulk: ${fnrs.size} personer, $antallAvvist avvist. Saksbehandler ${saksbehandler.navIdent}, correlationId $correlationId.",
                )
                respons.body
            }
    }

    /**
     * Logger feilen én gang, med samme deling mellom vanlig logg og sikkerlogg som httpklient sin egen [loggFeil].
     * Beskrivelsen i [TilgangskontrollFeil.UgyldigSvar] er vår egen og PII-fri, mens den rå responsen kun hører hjemme i sikkerlogg.
     */
    private fun loggTilgangskontrollFeil(
        feil: TilgangskontrollFeil,
        kontekst: String,
    ) {
        when (feil) {
            is TilgangskontrollFeil.Uventet -> feil.underliggende.loggFeil(
                logger = log,
                operasjon = "tilgangskontroll mot tilgangsmaskinen",
                kontekst = kontekst,
            )

            TilgangskontrollFeil.ForMangeIdenter -> log.error {
                "Feil ved tilgangskontroll mot tilgangsmaskinen. $kontekst. Ba om tilgang for flere enn maksgrensen."
            }

            is TilgangskontrollFeil.UgyldigSvar -> {
                log.error { "Feil ved tilgangskontroll mot tilgangsmaskinen. $kontekst. ${feil.beskrivelse} Endepunkt: ${feil.metadata.endepunkt}. Se sikkerlogg for responsen." }
                Sikkerlogg.error { "Feil ved tilgangskontroll mot tilgangsmaskinen. $kontekst. ${feil.beskrivelse} Endepunkt: ${feil.metadata.endepunkt}. Response: ${feil.metadata.rawResponseString}." }
            }
        }
    }

    /**
     * Enkeltoppslagets stier signaliserer fortsatt med kast; loggingen er felles med bulkstien.
     */
    private fun håndterTilgangskontrollFeil(
        feil: TilgangskontrollFeil,
        saksbehandler: Saksbehandler,
        kontekst: String,
    ): Nothing {
        loggTilgangskontrollFeil(feil, kontekst)
        throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent} - feil mot tilgangsmaskinen")
    }
}
