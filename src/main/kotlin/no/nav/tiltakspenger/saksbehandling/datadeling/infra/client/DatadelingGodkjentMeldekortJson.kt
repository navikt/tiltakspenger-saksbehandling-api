package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

private data class DatadelingGodkjentMeldekortJson(
    val kjedeId: String,
    val sakId: String,
    val meldeperiodeId: String,
    val fnr: String,
    val saksnummer: String,
    val mottattTidspunkt: LocalDateTime?,
    val vedtattTidspunkt: LocalDateTime,
    val behandletAutomatisk: Boolean,
    val korrigert: Boolean,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortdager: List<MeldekortDagDTO>,
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

fun MeldekortBehandling.Behandlet.toDatadelingJson(clock: Clock): String {
    return DatadelingGodkjentMeldekortJson(
        kjedeId = kjedeId.toString(),
        sakId = sakId.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        fnr = fnr.verdi,
        saksnummer = saksnummer.verdi,
        mottattTidspunkt = brukersMeldekort?.mottatt,
        vedtattTidspunkt = iverksattTidspunkt!!,
        behandletAutomatisk = this is MeldekortBehandletAutomatisk,
        korrigert = type == MeldekortBehandlingType.KORRIGERING,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        meldekortdager = dager.verdi.map { it.toDatadelingMeldekortDagDTO() },
        opprettet = opprettet,
        sistEndret = nå(clock),
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
