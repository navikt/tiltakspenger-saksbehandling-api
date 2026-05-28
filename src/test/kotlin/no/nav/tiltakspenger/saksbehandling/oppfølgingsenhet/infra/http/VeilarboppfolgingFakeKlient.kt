package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.BruktNavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteOppfølgingsenhet
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Klientkall
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorMedMetadata
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.VeilarboppfolgingKlient

class VeilarboppfolgingFakeKlient(
    private val svar: (Fnr) -> Either<KanIkkeHenteOppfølgingsenhet, NavkontorMedMetadata> = {
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
) : VeilarboppfolgingKlient {
    override suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String?,
        saksnummer: String?,
        rammebehandlingId: String?,
        meldekortbehandlingId: String?,
    ): Either<KanIkkeHenteOppfølgingsenhet, NavkontorMedMetadata> = svar(fnr)
}
