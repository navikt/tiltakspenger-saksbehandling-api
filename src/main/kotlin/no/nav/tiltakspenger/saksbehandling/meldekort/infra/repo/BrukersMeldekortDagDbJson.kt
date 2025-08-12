package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import java.time.LocalDate

enum class InnmeldtStatusDb {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_GODKJENT_AV_NAV,
    FRAVÆR_ANNET,
    IKKE_BESVART,
    IKKE_TILTAKSDAG,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

data class BrukersMeldekortDagDbJson(
    val dato: LocalDate,
    val status: InnmeldtStatusDb,
)

fun List<BrukersMeldekort.BrukersMeldekortDag>.toDbJson(): String {
    return this.map {
        BrukersMeldekortDagDbJson(
            dato = it.dato,
            status = when (it.status) {
                InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> InnmeldtStatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
                InnmeldtStatus.DELTATT_MED_LØNN_I_TILTAKET -> InnmeldtStatusDb.DELTATT_MED_LØNN_I_TILTAKET
                InnmeldtStatus.FRAVÆR_SYK -> InnmeldtStatusDb.FRAVÆR_SYK
                InnmeldtStatus.FRAVÆR_SYKT_BARN -> InnmeldtStatusDb.FRAVÆR_SYKT_BARN
                InnmeldtStatus.FRAVÆR_GODKJENT_AV_NAV -> InnmeldtStatusDb.FRAVÆR_GODKJENT_AV_NAV
                InnmeldtStatus.FRAVÆR_ANNET -> InnmeldtStatusDb.FRAVÆR_ANNET
                InnmeldtStatus.IKKE_BESVART -> InnmeldtStatusDb.IKKE_BESVART
                InnmeldtStatus.IKKE_TILTAKSDAG -> InnmeldtStatusDb.IKKE_TILTAKSDAG
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
                InnmeldtStatusDb.DELTATT_UTEN_LØNN_I_TILTAKET -> InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET
                InnmeldtStatusDb.DELTATT_MED_LØNN_I_TILTAKET -> InnmeldtStatus.DELTATT_MED_LØNN_I_TILTAKET
                InnmeldtStatusDb.FRAVÆR_SYK -> InnmeldtStatus.FRAVÆR_SYK
                InnmeldtStatusDb.FRAVÆR_SYKT_BARN -> InnmeldtStatus.FRAVÆR_SYKT_BARN
                InnmeldtStatusDb.FRAVÆR_GODKJENT_AV_NAV -> InnmeldtStatus.FRAVÆR_GODKJENT_AV_NAV
                InnmeldtStatusDb.FRAVÆR_ANNET -> InnmeldtStatus.FRAVÆR_ANNET
                InnmeldtStatusDb.IKKE_BESVART -> InnmeldtStatus.IKKE_BESVART
                InnmeldtStatusDb.IKKE_TILTAKSDAG -> InnmeldtStatus.IKKE_TILTAKSDAG
                InnmeldtStatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER
            },
        )
    }
}
