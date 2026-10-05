package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import java.time.LocalDateTime

/** Svarer default med ett ARENA-innslag for [ObjectMother.navkontor]. */
class KontorhistorikkFakeKlient(
    private val svar: (Fnr) -> Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata> = {
        val navkontor = ObjectMother.navkontor()
        KontorhistorikkMedMetadata(
            kontorhistorikk = Kontorhistorikk(
                listOf(
                    Kontorhistorikkinnslag(
                        kontorId = navkontor.kontornummer,
                        kontorNavn = navkontor.kontornavn,
                        kontorType = KontorType.ARENA,
                        endretTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
                    ),
                ),
            ),
            httpKlientMetadata = ObjectMother.httpKlientResponse(body = Unit).metadata,
        ).right()
    },
) : KontorhistorikkKlient {
    override suspend fun hentKontorhistorikk(
        fnr: Fnr,
    ): Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata> = svar(fnr)
}
