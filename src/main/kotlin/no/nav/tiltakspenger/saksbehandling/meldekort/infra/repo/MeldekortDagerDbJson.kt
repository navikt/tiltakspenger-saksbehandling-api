package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import java.time.LocalDate

data class MeldekortDagDbJson(
    val dato: LocalDate,
    val status: MeldekortDagStatusDb,
)

enum class MeldekortDagStatusDb {
    SPERRET,
    IKKE_UTFYLT,
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    IKKE_DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_VELFERD_GODKJENT_AV_NAV,
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
}

fun MeldekortDager.tilMeldekortDagerDbJson(): String =
    this.map {
        MeldekortDagDbJson(
            dato = it.dato,
            status = when (it.status) {
                MeldekortDagStatus.SPERRET -> MeldekortDagStatusDb.SPERRET
                MeldekortDagStatus.IKKE_UTFYLT -> MeldekortDagStatusDb.IKKE_UTFYLT
                MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatusDb.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagStatus.IKKE_DELTATT -> MeldekortDagStatusDb.IKKE_DELTATT
                MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusDb.FRAVÆR_SYK
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusDb.FRAVÆR_SYKT_BARN
                MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatusDb.FRAVÆR_VELFERD_GODKJENT_AV_NAV
                MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatusDb.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
            },
        )
    }.let { serialize(it) }

fun String.tilMeldekortDager(): MeldekortDager = MeldekortDager(
    deserializeList<MeldekortDagDbJson>(this).map {
        MeldekortDag(
            dato = it.dato,
            status = when (it.status) {
                MeldekortDagStatusDb.SPERRET -> MeldekortDagStatus.SPERRET
                MeldekortDagStatusDb.IKKE_UTFYLT -> MeldekortDagStatus.IKKE_UTFYLT
                MeldekortDagStatusDb.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagStatusDb.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagStatusDb.IKKE_DELTATT -> MeldekortDagStatus.IKKE_DELTATT
                MeldekortDagStatusDb.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
                MeldekortDagStatusDb.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
                MeldekortDagStatusDb.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV
                MeldekortDagStatusDb.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
            },
        )
    },
)
