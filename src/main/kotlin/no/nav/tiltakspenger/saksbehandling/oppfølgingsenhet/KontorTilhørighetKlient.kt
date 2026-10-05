package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

/**
 * Port for oppslag av kontortilhørigheten til en person.
 * Implementeres av [no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.KontorTilhørighetHttpklient].
 *
 * Klienten logger ikke selv.
 * [NavkontorService] logger med domenekontekst.
 */
interface KontorTilhørighetKlient {
    suspend fun hentKontorTilhørighet(
        fnr: Fnr,
    ): Either<KanIkkeHenteKontorTilhørighet, KontorTilhørighetMedMetadata>
}
