package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import java.time.LocalDate
import java.time.LocalDateTime

private data class DatadelingGodkjentMeldekortJson(
    val meldekortbehandlingId: String,
    val kjedeId: String,
    val sakId: String,
    val meldeperiodeId: String,
    val mottattTidspunkt: LocalDateTime?,
    val vedtattTidspunkt: LocalDateTime,
    val behandletAutomatisk: Boolean,
    val korrigert: Boolean,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortdager: List<MeldekortDagDTO>,
    val journalpostId: String,
    val totaltBelop: Int,
    val totalDifferanse: Int?,
    val barnetillegg: Boolean,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
) {
    data class MeldekortDagDTO(
        val dato: LocalDate,
        val status: String,
        val reduksjon: String,
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

fun Meldekortvedtak.toDatadelingJson(totalDifferanse: Int?): String {
    return DatadelingGodkjentMeldekortJson(
        meldekortbehandlingId = meldekortBehandling.id.toString(),
        kjedeId = meldekortBehandling.kjedeId.toString(),
        sakId = sakId.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        mottattTidspunkt = meldekortBehandling.brukersMeldekort?.mottatt,
        vedtattTidspunkt = opprettet,
        behandletAutomatisk = automatiskBehandlet,
        korrigert = erKorrigering,
        fraOgMed = meldekortBehandling.fraOgMed,
        tilOgMed = meldekortBehandling.tilOgMed,
        meldekortdager = meldekortBehandling.dager.verdi.map { it.toDatadelingMeldekortDagDTO() },
        journalpostId = journalpostId!!.toString(),
        totaltBelop = meldekortBehandling.beløpTotal,
        totalDifferanse = totalDifferanse,
        barnetillegg = meldekortBehandling.barnetilleggBeløp != 0,
        opprettet = opprettet,
        sistEndret = meldekortBehandling.sistEndret,
    ).let { serialize(it) }
}

private fun MeldekortDag.toDatadelingMeldekortDagDTO() = DatadelingGodkjentMeldekortJson.MeldekortDagDTO(
    dato = dato,
    status = status.tilDatadelingMeldekortDagStatus(),
    reduksjon = status.tilDatadelingReduksjon(),
)

fun MeldekortDagStatus.tilDatadelingMeldekortDagStatus(): String =
    when (this) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET.name
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.DELTATT_MED_LONN_I_TILTAKET.name
        MeldekortDagStatus.FRAVÆR_SYK -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.FRAVAER_SYK.name
        MeldekortDagStatus.FRAVÆR_SYKT_BARN -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.FRAVAER_SYKT_BARN.name
        MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.FRAVAER_GODKJENT_AV_NAV.name
        MeldekortDagStatus.FRAVÆR_ANNET -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.FRAVAER_ANNET.name
        MeldekortDagStatus.IKKE_BESVART -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.IKKE_BESVART.name
        MeldekortDagStatus.IKKE_TILTAKSDAG -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.IKKE_TILTAKSDAG.name
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER.name
    }

fun MeldekortDagStatus.tilDatadelingReduksjon(): String =
    when (this) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV,
        -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.Reduksjon.INGEN_REDUKSJON.name

        MeldekortDagStatus.FRAVÆR_SYK,
        MeldekortDagStatus.FRAVÆR_SYKT_BARN,
        -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.Reduksjon.UKJENT.name

        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET,
        MeldekortDagStatus.FRAVÆR_ANNET,
        MeldekortDagStatus.IKKE_BESVART,
        MeldekortDagStatus.IKKE_TILTAKSDAG,
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
        -> DatadelingGodkjentMeldekortJson.MeldekortDagDTO.Reduksjon.YTELSEN_FALLER_BORT.name
    }
