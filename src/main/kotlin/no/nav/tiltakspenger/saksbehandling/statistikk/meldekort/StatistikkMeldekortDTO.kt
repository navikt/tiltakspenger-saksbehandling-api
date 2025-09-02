package no.nav.tiltakspenger.saksbehandling.statistikk.meldekort

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class StatistikkMeldekortDTO(
    val meldeperiodeKjedeId: String,
    val sakId: String,
    val meldekortBehandlingId: String,
    val brukerId: String,
    val saksnummer: String,
    val vedtattTidspunkt: LocalDateTime,
    val behandletAutomatisk: Boolean,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortdager: List<StatistikkMeldekortDag>,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
) {
    data class StatistikkMeldekortDag(
        val dato: LocalDate,
        val status: MeldekortDagStatus,
        val reduksjon: Reduksjon,
    ) {
        enum class Reduksjon {
            INGEN_REDUKSJON,
            UKJENT,
            YTELSEN_FALLER_BORT,
        }

        enum class MeldekortDagStatus {
            DELTATT_UTEN_LONN_I_TILTAKET,
            DELTATT_MED_LONN_I_TILTAKET,
            FRAVAER_SYK,
            FRAVAER_SYKT_BARN,
            FRAVAER_GODKJENT_AV_NAV,
            FRAVAER_ANNET,
            IKKE_BESVART,
            IKKE_TILTAKSDAG,
            IKKE_RETT_TIL_TILTAKSPENGER,
        }
    }
}

fun MeldekortBehandling.Behandlet.tilStatistikkMeldekortDTO(): StatistikkMeldekortDTO =
    StatistikkMeldekortDTO(
        meldeperiodeKjedeId = kjedeId.toString(),
        sakId = sakId.toString(),
        meldekortBehandlingId = id.toString(),
        brukerId = fnr.verdi,
        saksnummer = saksnummer.verdi,
        vedtattTidspunkt = iverksattTidspunkt!!,
        behandletAutomatisk = this is MeldekortBehandletAutomatisk,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        meldekortdager = dager.verdi.map { it.tilStatistikkMeldekortDag() },
        opprettet = opprettet,
        sistEndret = LocalDateTime.now(),
    )

fun MeldekortDag.tilStatistikkMeldekortDag(): StatistikkMeldekortDTO.StatistikkMeldekortDag {
    return StatistikkMeldekortDTO.StatistikkMeldekortDag(
        dato = dato,
        status = status.tilStatistikkMeldekortDagStatus(),
        reduksjon = status.tilReduksjon(),
    )
}

fun MeldekortDagStatus.tilStatistikkMeldekortDagStatus(): StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus =
    when (this) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.DELTATT_MED_LONN_I_TILTAKET
        MeldekortDagStatus.FRAVÆR_SYK -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.FRAVAER_SYK
        MeldekortDagStatus.FRAVÆR_SYKT_BARN -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.FRAVAER_SYKT_BARN
        MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.FRAVAER_GODKJENT_AV_NAV
        MeldekortDagStatus.FRAVÆR_ANNET -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.FRAVAER_ANNET
        MeldekortDagStatus.IKKE_BESVART -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.IKKE_BESVART
        MeldekortDagStatus.IKKE_TILTAKSDAG -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.IKKE_TILTAKSDAG
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> StatistikkMeldekortDTO.StatistikkMeldekortDag.MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }

fun MeldekortDagStatus.tilReduksjon(): StatistikkMeldekortDTO.StatistikkMeldekortDag.Reduksjon =
    when (this) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV,
        -> StatistikkMeldekortDTO.StatistikkMeldekortDag.Reduksjon.INGEN_REDUKSJON

        MeldekortDagStatus.FRAVÆR_SYK,
        MeldekortDagStatus.FRAVÆR_SYKT_BARN,
        -> StatistikkMeldekortDTO.StatistikkMeldekortDag.Reduksjon.UKJENT

        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET,
        MeldekortDagStatus.FRAVÆR_ANNET,
        MeldekortDagStatus.IKKE_BESVART,
        MeldekortDagStatus.IKKE_TILTAKSDAG,
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
        -> StatistikkMeldekortDTO.StatistikkMeldekortDag.Reduksjon.YTELSEN_FALLER_BORT
    }
