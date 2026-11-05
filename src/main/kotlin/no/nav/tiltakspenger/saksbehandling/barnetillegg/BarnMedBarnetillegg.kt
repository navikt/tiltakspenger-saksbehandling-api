package no.nav.tiltakspenger.saksbehandling.barnetillegg

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
 * @param periode Perioden saksbehandler har valgt å gi barnetillegg for barnet, eller null for hele innvilgelsesperioden.
 * Uansett gis det bare barnetillegg mens barnet har rett, se [periodeMedRett].
 */
data class BarnMedBarnetillegg(
    val fnr: Fnr?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val fødselsdato: LocalDate,
    val kilde: Kilde,
    val periode: Periode?,
) {
    /** Fra og med fødselsdatoen til og med dagen før barnet fyller 16 år. */
    val periodeMedRett: Periode = periodeMedRettPåBarnetillegg(fødselsdato)

    val erHelePerioden: Boolean = periode == null

    /**
     * Barna har ingen egen id, så de identifiseres med fnr når begge har det, og ellers med navn og fødselsdato.
     */
    fun erSammeBarn(other: BarnMedBarnetillegg): Boolean {
        if (fnr != null && other.fnr != null) return fnr == other.fnr
        return fødselsdato == other.fødselsdato &&
            fornavn.normaliser() == other.fornavn.normaliser() &&
            mellomnavn.normaliser() == other.mellomnavn.normaliser() &&
            etternavn.normaliser() == other.etternavn.normaliser()
    }

    sealed interface Kilde {
        data class Søknad(val søknadId: SøknadId) : Kilde

        data object Saksbehandler : Kilde
    }
}

private fun String?.normaliser(): String? = this?.trim()?.lowercase()?.ifBlank { null }

/** Barnet fra søknaden [søknadId], med barnetillegg for hele innvilgelsesperioden. */
fun BarnetilleggFraSøknad.tilBarnMedBarnetillegg(søknadId: SøknadId): BarnMedBarnetillegg = BarnMedBarnetillegg(
    fnr = (this as? BarnetilleggFraSøknad.FraPdl)?.fnr,
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fødselsdato = fødselsdato,
    kilde = BarnMedBarnetillegg.Kilde.Søknad(søknadId),
    periode = null,
)
