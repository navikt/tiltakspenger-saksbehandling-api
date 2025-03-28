package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import java.time.LocalDate

enum class InnmeldtStatusDb {
    DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_ANNET,
    IKKE_REGISTRERT,
    IKKE_DELTATT,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

data class BrukersMeldekortDagDbJson(
    val dato: LocalDate,
    val status: InnmeldtStatusDb,
)

fun LagreBrukersMeldekortKommando.toDbJson(): String {
    return dager.map {
        BrukersMeldekortDagDbJson(
            dato = it.dato,
            status = when (it.status) {
                InnmeldtStatus.DELTATT -> InnmeldtStatusDb.DELTATT
                InnmeldtStatus.FRAVÆR_SYK -> InnmeldtStatusDb.FRAVÆR_SYK
                InnmeldtStatus.FRAVÆR_SYKT_BARN -> InnmeldtStatusDb.FRAVÆR_SYKT_BARN
                InnmeldtStatus.FRAVÆR_ANNET -> InnmeldtStatusDb.FRAVÆR_ANNET
                InnmeldtStatus.IKKE_REGISTRERT -> InnmeldtStatusDb.IKKE_REGISTRERT
                InnmeldtStatus.IKKE_DELTATT -> InnmeldtStatusDb.IKKE_DELTATT
                InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER -> InnmeldtStatusDb.IKKE_RETT_TIL_TILTAKSPENGER
            },
        )
    }.let { serialize(it) }
}

fun String.toMeldekortDager(): List<BrukersMeldekort.BrukersMeldekortDag> {
    return deserialize<List<BrukersMeldekortDagDbJson>>(this).map {
        BrukersMeldekort.BrukersMeldekortDag(
            dato = it.dato,
            status = when (it.status) {
                InnmeldtStatusDb.DELTATT -> InnmeldtStatus.DELTATT
                InnmeldtStatusDb.FRAVÆR_SYK -> InnmeldtStatus.FRAVÆR_SYK
                InnmeldtStatusDb.FRAVÆR_SYKT_BARN -> InnmeldtStatus.FRAVÆR_SYKT_BARN
                InnmeldtStatusDb.FRAVÆR_ANNET -> InnmeldtStatus.FRAVÆR_ANNET
                InnmeldtStatusDb.IKKE_REGISTRERT -> InnmeldtStatus.IKKE_REGISTRERT
                InnmeldtStatusDb.IKKE_DELTATT -> InnmeldtStatus.IKKE_DELTATT
                InnmeldtStatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER
            },
        )
    }
}
