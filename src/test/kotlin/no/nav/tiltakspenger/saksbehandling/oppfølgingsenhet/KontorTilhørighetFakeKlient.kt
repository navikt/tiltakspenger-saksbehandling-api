package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorTilhørighet.KontorType

/** Svarer default med Arena-kontoret [ObjectMother.navkontor]. */
class KontorTilhørighetFakeKlient(
    private val svar: (Fnr) -> Either<KanIkkeHenteKontorTilhørighet, KontorTilhørighetMedMetadata> = {
        val navkontor = ObjectMother.navkontor()
        KontorTilhørighetMedMetadata(
            kontorTilhørighet = KontorTilhørighet(
                kontorId = navkontor.kontornummer,
                kontorNavn = navkontor.kontornavn!!,
                kontorType = KontorType.ARENA,
            ),
            httpKlientMetadata = ObjectMother.httpKlientResponse(body = Unit).metadata,
        ).right()
    },
) : KontorTilhørighetKlient {
    override suspend fun hentKontorTilhørighet(
        fnr: Fnr,
    ): Either<KanIkkeHenteKontorTilhørighet, KontorTilhørighetMedMetadata> = svar(fnr)
}
