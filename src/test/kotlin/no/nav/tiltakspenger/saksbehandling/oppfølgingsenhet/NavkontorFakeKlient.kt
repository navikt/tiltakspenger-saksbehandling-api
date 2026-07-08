package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

class NavkontorFakeKlient(
    private val svar: (Fnr) -> Either<KanIkkeHenteNavkontor, NavkontorMedMetadata> = {
        NavkontorMedMetadata(
            navkontor = ObjectMother.navkontor(),
            brukteKlient = BruktNavkontorKlient.VEILARBOPPFOLGING,
            veilarboppfolgingKall = Klientkall(
                request = """{"fnr":"${it.verdi}"}""",
                response = """{"oppfolgingsenhet":{"navn":"Nav Asker","enhetId":"0220"}}""",
                httpStatus = 200,
            ),
        ).right()
    },
) : NavkontorKlient {
    override suspend fun hentNavkontor(
        fnr: Fnr,
        loggkontekst: String,
    ): Either<KanIkkeHenteNavkontor, NavkontorMedMetadata> = svar(fnr)
}
