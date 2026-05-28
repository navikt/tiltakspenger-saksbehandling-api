package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteKontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Klientkall
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorhistorikkKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KontorhistorikkMedMetadata
import java.time.LocalDateTime

class KontorhistorikkFakeKlient(
    private val historikk: (Fnr) -> Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata> = {
        KontorhistorikkMedMetadata(
            kontorhistorikk = defaultHistorikk(),
            kall = defaultKall(it),
        ).right()
    },
) : KontorhistorikkKlient {
    override suspend fun hentKontorhistorikk(
        fnr: Fnr,
        sakId: String?,
        saksnummer: String?,
        rammebehandlingId: String?,
        meldekortbehandlingId: String?,
    ): Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata> = historikk(fnr)

    companion object {
        fun defaultHistorikk(): Kontorhistorikk =
            Kontorhistorikk(
                listOf(
                    Kontorhistorikk.Kontorhistorikkinnslag(
                        kontorId = "0123",
                        kontorNavn = "NAV Fake Kontor",
                        kontorType = Kontorhistorikk.KontorType.ARENA,
                        endretTidspunkt = LocalDateTime.parse("2024-01-01T12:00:00"),
                    ),
                ),
            )

        fun defaultKall(fnr: Fnr): Klientkall =
            Klientkall(
                request = """{"query":"...","variables":{"ident":"${fnr.verdi}"}}""",
                response = """{"data":{"kontorHistorikk":[]}}""",
                httpStatus = 200,
            )
    }
}
