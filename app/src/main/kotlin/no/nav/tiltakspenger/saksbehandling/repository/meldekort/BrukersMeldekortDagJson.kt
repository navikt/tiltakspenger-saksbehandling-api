package no.nav.tiltakspenger.saksbehandling.repository.meldekort

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto.toDTO
import java.time.LocalDate

data class BrukersMeldekortDagJson(
    val dato: LocalDate,
    val status: String,
)

fun LagreBrukersMeldekortKommando.toDagerJson(): String {
    return dager.map {
        BrukersMeldekortDagJson(
            dato = it.dato,
            status = it.status.toDTO(),
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
                "IKKE_RETT_TIL_TILTAKSPENGER" -> InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER
                else -> throw IllegalArgumentException("Ukjent status: $it.status")
            },
        )
    }
}
