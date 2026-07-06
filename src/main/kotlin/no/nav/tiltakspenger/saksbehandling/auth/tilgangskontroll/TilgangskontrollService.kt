package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.infra.http.loggFeil

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

    suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
        saksbehandler: Saksbehandler,
    ): Map<Fnr, Boolean> {
        return tilgangsmaskinClient.harTilgangTilPersoner(fnrs, saksbehandlerToken).getOrElse { feil ->
            håndterTilgangskontrollFeil(feil, saksbehandler, "Saksbehandler ${saksbehandler.navIdent}, ${fnrs.size} personer")
        }
    }

    private fun håndterTilgangskontrollFeil(
        feil: TilgangskontrollFeil,
        saksbehandler: Saksbehandler,
        kontekst: String,
    ): Nothing {
        when (feil) {
            is TilgangskontrollFeil.Uventet -> feil.underliggende.loggFeil(
                logger = log,
                operasjon = "tilgangskontroll mot tilgangsmaskinen",
                kontekst = kontekst,
            )

            TilgangskontrollFeil.ForMangeIdenter -> log.error {
                "Feil ved tilgangskontroll mot tilgangsmaskinen. $kontekst. Ba om tilgang for flere enn maksgrensen."
            }
        }
        throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent} - feil mot tilgangsmaskinen")
    }
}
