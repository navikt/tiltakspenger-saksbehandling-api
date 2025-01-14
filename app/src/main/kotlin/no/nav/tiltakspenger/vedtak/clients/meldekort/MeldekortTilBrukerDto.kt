package no.nav.tiltakspenger.vedtak.clients.meldekort

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.IkkeUtfylt
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Sperret
import java.time.LocalDate

// TODO abn: Dette er fortsatt work in progress
// Bør inn i felles libs asap, brukes også i meldekort-api

enum class MeldekortStatusTilBrukerDTO {
    KAN_UTFYLLES,
    KAN_IKKE_UTFYLLES,
    GODKJENT,
}

// TODO abn: bør kanskje splittes opp i rapportering for dagen, og status (ie sperret/godkjent/ikke utfylt etc)
// (Se det ann når vi skriver om datamodellen for meldekort/behandling/periode)
enum class MeldekortDagStatusTilBrukerDTO {
    DELTATT_UTEN_LØNN,
    DELTATT_MED_LØNN,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_ANNET_MED_RETT,
    FRAVÆR_ANNET_UTEN_RETT,
    IKKE_DELTATT,
    IKKE_REGISTRERT,
    IKKE_RETT,
}

data class MeldekortDagTilBrukerDTO(
    val dag: LocalDate,
    val status: MeldekortDagStatusTilBrukerDTO,
    val tiltakstype: TiltakstypeSomGirRett,
)

data class MeldekortTilBrukerDTO(
    val id: String,
    val fnr: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: MeldekortStatusTilBrukerDTO,
    val meldekortDager: List<MeldekortDagTilBrukerDTO>,
)

fun Meldeperiode.tilBrukerDTO(): MeldekortTilBrukerDTO {
    return MeldekortTilBrukerDTO(
        id = this.hendelseId.toString(),
        fnr = this.fnr.verdi,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        status = MeldekortStatusTilBrukerDTO.KAN_UTFYLLES,
        meldekortDager = this.girRett.map {
            val status: MeldekortDagStatusTilBrukerDTO =
                if (it.value) MeldekortDagStatusTilBrukerDTO.IKKE_REGISTRERT else MeldekortDagStatusTilBrukerDTO.IKKE_REGISTRERT

            MeldekortDagTilBrukerDTO(
                dag = it.key,
                status = status,
                tiltakstype = TiltakstypeSomGirRett.JOBBKLUBB,
            )
        },
    )
}

fun MeldekortBehandling.tilBrukerDTO(): MeldekortTilBrukerDTO {
    return MeldekortTilBrukerDTO(
        id = this.id.toString(),
        fnr = this.fnr.verdi,
        fraOgMed = this.fraOgMed,
        tilOgMed = this.tilOgMed,
        status = this.tilBrukerStatusDTO(),
        meldekortDager = this.beregning.dager.map {
            MeldekortDagTilBrukerDTO(
                dag = it.dato,
                status = it.tilBrukerStatusDTO(),
                tiltakstype = this.tiltakstype,
            )
        },
    )
}

private fun MeldekortBehandling.tilBrukerStatusDTO(): MeldekortStatusTilBrukerDTO =
    when (this) {
        is MeldekortBehandling.IkkeUtfyltMeldekort -> if (this.erKlarTilUtfylling()) MeldekortStatusTilBrukerDTO.KAN_UTFYLLES else MeldekortStatusTilBrukerDTO.KAN_IKKE_UTFYLLES
        is MeldekortBehandling.UtfyltMeldekort -> when (this.status) {
            MeldekortBehandlingStatus.GODKJENT -> MeldekortStatusTilBrukerDTO.GODKJENT
            else -> MeldekortStatusTilBrukerDTO.KAN_IKKE_UTFYLLES
        }
    }

private fun MeldeperiodeBeregningDag.tilBrukerStatusDTO(): MeldekortDagStatusTilBrukerDTO =
    when (this) {
        is DeltattMedLønnITiltaket -> MeldekortDagStatusTilBrukerDTO.DELTATT_MED_LØNN
        is DeltattUtenLønnITiltaket -> MeldekortDagStatusTilBrukerDTO.DELTATT_UTEN_LØNN
        is SykBruker -> MeldekortDagStatusTilBrukerDTO.FRAVÆR_SYK
        is SyktBarn -> MeldekortDagStatusTilBrukerDTO.FRAVÆR_SYKT_BARN
        is VelferdGodkjentAvNav -> MeldekortDagStatusTilBrukerDTO.FRAVÆR_ANNET_MED_RETT
        is VelferdIkkeGodkjentAvNav -> MeldekortDagStatusTilBrukerDTO.FRAVÆR_ANNET_UTEN_RETT
        is IkkeDeltatt -> MeldekortDagStatusTilBrukerDTO.IKKE_DELTATT
        is IkkeUtfylt -> MeldekortDagStatusTilBrukerDTO.IKKE_REGISTRERT
        is Sperret -> MeldekortDagStatusTilBrukerDTO.IKKE_RETT
    }
