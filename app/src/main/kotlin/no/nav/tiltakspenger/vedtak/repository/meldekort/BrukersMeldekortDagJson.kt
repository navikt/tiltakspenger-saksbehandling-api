package no.nav.tiltakspenger.vedtak.repository.meldekort

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.meldekort.domene.NyttBrukersMeldekort
import java.time.LocalDate

data class BrukersMeldekortDagJson(
    val dato: LocalDate,
    val status: String,
)

fun NyttBrukersMeldekort.toDagerJson(): String {
    return dager.map {
        BrukersMeldekortDagJson(
            dato = it.dato,
            status = when (it.status) {
                InnmeldtStatus.DELTATT -> "DELTATT"
                InnmeldtStatus.FRAVÆR_SYK -> "FRAVÆR_SYK"
                InnmeldtStatus.FRAVÆR_SYKT_BARN -> "FRAVÆR_SYKT_BARN"
                InnmeldtStatus.FRAVÆR_ANNET -> "FRAVÆR_ANNET"
                InnmeldtStatus.IKKE_REGISTRERT -> "IKKE_REGISTRERT"
                InnmeldtStatus.IKKE_DELTATT -> "IKKE_DELTATT"
                InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER -> "IKKE_RETT_TIL_TILTAKSPENGER"
            },
        )
    }.let { serialize(it) }
}

fun String.toMeldekortDager(): List<BrukersMeldekort.BrukersMeldekortDag> {
    return deserialize<List<BrukersMeldekortDagJson>>(this).map {
        BrukersMeldekort.BrukersMeldekortDag(
            dato = it.dato,
            status = when (it.status) {
                "DELTATT" -> InnmeldtStatus.DELTATT
                "FRAVÆR_SYK" -> InnmeldtStatus.FRAVÆR_SYK
                "FRAVÆR_SYKT_BARN" -> InnmeldtStatus.FRAVÆR_SYKT_BARN
                "FRAVÆR_ANNET" -> InnmeldtStatus.FRAVÆR_ANNET
                "IKKE_REGISTRERT" -> InnmeldtStatus.IKKE_REGISTRERT
                "IKKE_DELTATT" -> InnmeldtStatus.IKKE_DELTATT
                else -> throw IllegalArgumentException("Ukjent status: $it.status")
            },
        )
    }
}
