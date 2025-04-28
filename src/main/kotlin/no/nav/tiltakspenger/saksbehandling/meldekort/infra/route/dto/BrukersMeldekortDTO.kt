package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import java.time.LocalDate
import java.time.LocalDateTime

private enum class InnmeldtStatusDTO {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_VELFERD_GODKJENT_AV_NAV,
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
    IKKE_REGISTRERT,
}

enum class BrukersMeldekortBehandletAutomatiskStatusDTO {
    VENTER_BEHANDLING,
    BEHANDLET,
    UKJENT_FEIL,
    HENTE_NAVKONTOR_FEILET,
    BEHANDLING_FEILET_PÅ_SAK,
    UTBETALING_FEILET_PÅ_SAK,
    SKAL_IKKE_BEHANDLES_AUTOMATISK,
    ALLEREDE_BEHANDLET,
    UTDATERT_MELDEPERIODE,
}

data class BrukersMeldekortDTO(
    val id: String,
    val mottatt: LocalDateTime,
    val dager: List<DagDTO>,
    val behandletAutomatiskStatus: BrukersMeldekortBehandletAutomatiskStatusDTO?,
) {
    data class DagDTO(
        val status: String,
        val dato: LocalDate,
    )
}

fun BrukersMeldekort.toBrukersMeldekortDTO(): BrukersMeldekortDTO {
    return BrukersMeldekortDTO(
        id = id.toString(),
        mottatt = mottatt,
        dager = dager.map {
            BrukersMeldekortDTO.DagDTO(
                status = it.status.toInnmeldtStatusString(),
                dato = it.dato,
            )
        },
        behandletAutomatiskStatus = tilBehandletAutomatiskStatusDTO(),
    )
}

fun BrukersMeldekort.tilBehandletAutomatiskStatusDTO(): BrukersMeldekortBehandletAutomatiskStatusDTO? {
    return when (this.behandletAutomatiskStatus) {
        BrukersMeldekortBehandletAutomatiskStatus.BEHANDLET -> BrukersMeldekortBehandletAutomatiskStatusDTO.BEHANDLET
        BrukersMeldekortBehandletAutomatiskStatus.UKJENT_FEIL -> BrukersMeldekortBehandletAutomatiskStatusDTO.UKJENT_FEIL
        BrukersMeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET -> BrukersMeldekortBehandletAutomatiskStatusDTO.HENTE_NAVKONTOR_FEILET
        BrukersMeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK -> BrukersMeldekortBehandletAutomatiskStatusDTO.BEHANDLING_FEILET_PÅ_SAK
        BrukersMeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK -> BrukersMeldekortBehandletAutomatiskStatusDTO.UTBETALING_FEILET_PÅ_SAK
        BrukersMeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK -> BrukersMeldekortBehandletAutomatiskStatusDTO.SKAL_IKKE_BEHANDLES_AUTOMATISK
        BrukersMeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET -> BrukersMeldekortBehandletAutomatiskStatusDTO.ALLEREDE_BEHANDLET
        BrukersMeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE -> BrukersMeldekortBehandletAutomatiskStatusDTO.UTDATERT_MELDEPERIODE
        null ->
            if (this.behandlesAutomatisk) {
                BrukersMeldekortBehandletAutomatiskStatusDTO.VENTER_BEHANDLING
            } else {
                null
            }
    }
}

private fun InnmeldtStatus.toInnmeldtStatusString(): String = when (this) {
    InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> InnmeldtStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
    InnmeldtStatus.DELTATT_MED_LØNN_I_TILTAKET -> InnmeldtStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
    InnmeldtStatus.FRAVÆR_SYK -> InnmeldtStatusDTO.FRAVÆR_SYK
    InnmeldtStatus.FRAVÆR_SYKT_BARN -> InnmeldtStatusDTO.FRAVÆR_SYKT_BARN
    InnmeldtStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> InnmeldtStatusDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
    InnmeldtStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> InnmeldtStatusDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
    InnmeldtStatus.IKKE_REGISTRERT -> InnmeldtStatusDTO.IKKE_REGISTRERT
}.toString()
