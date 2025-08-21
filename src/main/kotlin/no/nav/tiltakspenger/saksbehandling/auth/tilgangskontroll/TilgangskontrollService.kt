package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinClient

class TilgangskontrollService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Either<IkkeTilgangDetaljer, Boolean> {
        try {
            tilgangsmaskinClient.harTilgangTilPerson(fnr, saksbehandlerToken).fold(
                ifLeft = {
                    return IkkeTilgangDetaljer.AvvistTilgang(
                        regel = it.title,
                        begrunnelse = it.begrunnelse,
                    ).left()
                },
                ifRight = {
                    return it.right()
                },
            )
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for person: ${e.message}" }
            return IkkeTilgangDetaljer.UkjentFeil(e.message).left()
        }
    }

    suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<IkkeTilgangDetaljer, Map<Fnr, Boolean>> {
        try {
            val respons = tilgangsmaskinClient.harTilgangTilPersoner(fnrs, saksbehandlerToken)
            return respons.resultater.associate { Fnr.fromString(it.brukerId) to it.harTilgangTilPerson() }.right()
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for flere personer: ${e.message}" }
            return IkkeTilgangDetaljer.UkjentFeil(e.message).left()
        }
    }
}
