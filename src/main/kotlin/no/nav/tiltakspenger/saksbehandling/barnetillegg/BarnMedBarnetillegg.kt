package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import java.time.LocalDate

/**
 * Et barn saksbehandler har valgt å gi barnetillegg for, se [BarnetilleggV2].
 *
 * @param fnr Kan mangle for barn som er lagt til av saksbehandler, eller som er oppgitt uten fnr i søknaden.
 * @param kilde Hvor saksbehandler fant barnet.
 * @param perioder Periodene saksbehandler har valgt å gi barnetillegg for barnet, eller null for hele innvilgelsesperioden.
 * Sortert og uten overlapp, se [BarnetilleggV2.create].
 * Uansett gis det bare barnetillegg mens barnet har rett, se [periodeMedRett].
 */
data class BarnMedBarnetillegg(
    val fnr: Fnr?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val fødselsdato: LocalDate,
    val kilde: Kilde,
    val perioder: NonEmptyList<Periode>?,
) {
    /** Fra og med fødselsdatoen til og med dagen før barnet fyller 16 år. */
    val periodeMedRett: Periode = periodeMedRettPåBarnetillegg(fødselsdato)

    val erHelePerioden: Boolean = perioder == null

    /**
     * Barna har ingen egen id, så de er samme barn når fnr er likt, eller når fødselsdato og navn er like.
     * Det siste gjelder også når barna har ulikt fnr, f.eks. når saksbehandler har lagt til barnet med et nytt fnr.
     * Barn med ulikt fnr må da ha fornavn og etternavn, siden f.eks. tvillinger uten navn ellers ville blitt regnet som samme barn.
     */
    fun erSammeBarn(other: BarnMedBarnetillegg): Boolean {
        if (fnr != null && fnr == other.fnr) return true
        val harSammeNavnOgFødselsdato = fødselsdato == other.fødselsdato &&
            fornavn.normaliser() == other.fornavn.normaliser() &&
            mellomnavn.normaliser() == other.mellomnavn.normaliser() &&
            etternavn.normaliser() == other.etternavn.normaliser()
        if (!harSammeNavnOgFødselsdato) return false
        return fnr == null || other.fnr == null || (fornavn.normaliser() != null && etternavn.normaliser() != null)
    }

    sealed interface Kilde {
        data class Søknad(val søknadId: SøknadId) : Kilde

        data object Saksbehandler : Kilde
    }
}

private fun String?.normaliser(): String? = this?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")?.ifBlank { null }

/** Barnet fra søknaden [søknadId], med barnetillegg for hele innvilgelsesperioden. */
fun BarnetilleggFraSøknad.tilBarnMedBarnetillegg(søknadId: SøknadId): BarnMedBarnetillegg = BarnMedBarnetillegg(
    fnr = (this as? BarnetilleggFraSøknad.FraPdl)?.fnr,
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fødselsdato = fødselsdato,
    kilde = BarnMedBarnetillegg.Kilde.Søknad(søknadId),
    perioder = null,
)
