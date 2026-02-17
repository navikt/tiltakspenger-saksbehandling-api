package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus
import java.time.LocalDate
import java.time.LocalDateTime

private enum class InnmeldtStatusDTO {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
    FRAVÆR_GODKJENT_AV_NAV,
    FRAVÆR_ANNET,
    IKKE_BESVART,
    IKKE_TILTAKSDAG,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

enum class MeldekortBehandletAutomatiskStatusDTO {
    VENTER_BEHANDLING,
    BEHANDLET,
    UKJENT_FEIL,
    UKJENT_FEIL_PRØVER_IGJEN,
    HENTE_NAVKONTOR_FEILET,
    BEHANDLING_FEILET_PÅ_SAK,
    UTBETALING_FEILET_PÅ_SAK,
    SKAL_IKKE_BEHANDLES_AUTOMATISK,
    ALLEREDE_BEHANDLET,
    UTDATERT_MELDEPERIODE,
    ER_UNDER_REVURDERING,
    FOR_MANGE_DAGER_REGISTRERT,
    KAN_IKKE_MELDE_HELG,
    FOR_MANGE_DAGER_GODKJENT_FRAVÆR,
    HAR_ÅPEN_BEHANDLING,
    MÅ_BEHANDLE_FØRSTE_KJEDE,
    MÅ_BEHANDLE_NESTE_KJEDE,
    INGEN_DAGER_GIR_RETT,
}

data class BrukersMeldekortDTO(
    val id: String,
    val mottatt: LocalDateTime,
    val dager: List<DagDTO>,
    val behandletAutomatiskStatus: MeldekortBehandletAutomatiskStatusDTO,
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
        behandletAutomatiskStatus = behandletAutomatiskStatus.tilBehandletAutomatiskStatusDTO(),
    )
}

private fun MeldekortBehandletAutomatiskStatus.tilBehandletAutomatiskStatusDTO(): MeldekortBehandletAutomatiskStatusDTO {
    return when (this) {
        MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING -> MeldekortBehandletAutomatiskStatusDTO.VENTER_BEHANDLING
        MeldekortBehandletAutomatiskStatus.BEHANDLET -> MeldekortBehandletAutomatiskStatusDTO.BEHANDLET
        MeldekortBehandletAutomatiskStatus.UKJENT_FEIL -> MeldekortBehandletAutomatiskStatusDTO.UKJENT_FEIL
        MeldekortBehandletAutomatiskStatus.UKJENT_FEIL_PRØVER_IGJEN -> MeldekortBehandletAutomatiskStatusDTO.UKJENT_FEIL_PRØVER_IGJEN
        MeldekortBehandletAutomatiskStatus.HENTE_NAVKONTOR_FEILET -> MeldekortBehandletAutomatiskStatusDTO.HENTE_NAVKONTOR_FEILET
        MeldekortBehandletAutomatiskStatus.BEHANDLING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDTO.BEHANDLING_FEILET_PÅ_SAK
        MeldekortBehandletAutomatiskStatus.UTBETALING_FEILET_PÅ_SAK -> MeldekortBehandletAutomatiskStatusDTO.UTBETALING_FEILET_PÅ_SAK
        MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK -> MeldekortBehandletAutomatiskStatusDTO.SKAL_IKKE_BEHANDLES_AUTOMATISK
        MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET -> MeldekortBehandletAutomatiskStatusDTO.ALLEREDE_BEHANDLET
        MeldekortBehandletAutomatiskStatus.UTDATERT_MELDEPERIODE -> MeldekortBehandletAutomatiskStatusDTO.UTDATERT_MELDEPERIODE
        MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING -> MeldekortBehandletAutomatiskStatusDTO.ER_UNDER_REVURDERING
        MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_REGISTRERT -> MeldekortBehandletAutomatiskStatusDTO.FOR_MANGE_DAGER_REGISTRERT
        MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG -> MeldekortBehandletAutomatiskStatusDTO.KAN_IKKE_MELDE_HELG
        MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR -> MeldekortBehandletAutomatiskStatusDTO.FOR_MANGE_DAGER_GODKJENT_FRAVÆR
        MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING -> MeldekortBehandletAutomatiskStatusDTO.HAR_ÅPEN_BEHANDLING
        MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE -> MeldekortBehandletAutomatiskStatusDTO.MÅ_BEHANDLE_FØRSTE_KJEDE
        MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE -> MeldekortBehandletAutomatiskStatusDTO.MÅ_BEHANDLE_NESTE_KJEDE
        MeldekortBehandletAutomatiskStatus.INGEN_DAGER_GIR_RETT -> MeldekortBehandletAutomatiskStatusDTO.INGEN_DAGER_GIR_RETT
    }
}

private fun InnmeldtStatus.toInnmeldtStatusString(): String = when (this) {
    InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> InnmeldtStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
    InnmeldtStatus.DELTATT_MED_LØNN_I_TILTAKET -> InnmeldtStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
    InnmeldtStatus.FRAVÆR_SYK -> InnmeldtStatusDTO.FRAVÆR_SYK
    InnmeldtStatus.FRAVÆR_SYKT_BARN -> InnmeldtStatusDTO.FRAVÆR_SYKT_BARN
    InnmeldtStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> InnmeldtStatusDTO.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
    InnmeldtStatus.FRAVÆR_GODKJENT_AV_NAV -> InnmeldtStatusDTO.FRAVÆR_GODKJENT_AV_NAV
    InnmeldtStatus.FRAVÆR_ANNET -> InnmeldtStatusDTO.FRAVÆR_ANNET
    InnmeldtStatus.IKKE_BESVART -> InnmeldtStatusDTO.IKKE_BESVART
    InnmeldtStatus.IKKE_TILTAKSDAG -> InnmeldtStatusDTO.IKKE_TILTAKSDAG
    InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER -> InnmeldtStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
}.toString()
