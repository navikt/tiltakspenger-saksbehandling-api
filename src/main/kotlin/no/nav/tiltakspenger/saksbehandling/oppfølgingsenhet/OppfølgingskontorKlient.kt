package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

/**
 * Port for oppslag mot ao-oppfolgingskontor.
 * Implementeres av [no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.OppfølgingskontorHttpklient].
 *
 * Klienten logger ikke selv.
 * [NavkontorService] logger med domenekontekst.
 */
interface OppfølgingskontorKlient {
    /** Kontoret tjenesten har valgt for personen. */
    suspend fun hentKontorTilhørighet(
        fnr: Fnr,
    ): Either<KanIkkeHenteOppfølgingskontor, KontorTilhørighetMedMetadata>

    /** Alle innslag i kontorhistorikken, uten filtrering. */
    suspend fun hentKontorhistorikk(
        fnr: Fnr,
    ): Either<KanIkkeHenteOppfølgingskontor, KontorhistorikkMedMetadata>
}
