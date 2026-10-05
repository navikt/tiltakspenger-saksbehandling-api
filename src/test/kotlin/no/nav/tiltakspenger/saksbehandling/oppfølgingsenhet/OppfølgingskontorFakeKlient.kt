package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import java.time.LocalDateTime

/** Svarer default med Arena-kontoret [ObjectMother.navkontor] fra begge spørringene. */
class OppfølgingskontorFakeKlient(
    private val kontorTilhørighet: (Fnr) -> Either<KanIkkeHenteOppfølgingskontor, KontorTilhørighetMedMetadata> = {
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
    private val kontorhistorikk: (Fnr) -> Either<KanIkkeHenteOppfølgingskontor, KontorhistorikkMedMetadata> = {
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
) : OppfølgingskontorKlient {
    override suspend fun hentKontorTilhørighet(
        fnr: Fnr,
    ): Either<KanIkkeHenteOppfølgingskontor, KontorTilhørighetMedMetadata> = kontorTilhørighet(fnr)

    override suspend fun hentKontorhistorikk(
        fnr: Fnr,
    ): Either<KanIkkeHenteOppfølgingskontor, KontorhistorikkMedMetadata> = kontorhistorikk(fnr)
}
