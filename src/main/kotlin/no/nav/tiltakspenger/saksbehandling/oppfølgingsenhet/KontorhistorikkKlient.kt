package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

/**
 * Port for oppslag av kontorhistorikken til en person.
 * Implementeres av [no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.KontorhistorikkHttpklient].
 *
 * Klienten returnerer rådata uten å velge kontor og logger ikke selv.
 * [NavkontorService] velger kontor ([Kontorhistorikk.nyesteAktuelleKontor]) og logger med domenekontekst.
 */
interface KontorhistorikkKlient {
    suspend fun hentKontorhistorikk(
        fnr: Fnr,
    ): Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata>
}
