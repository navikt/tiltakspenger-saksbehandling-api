package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggKandidat
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.toDTO
import java.time.LocalDate

/**
 * Et barn saksbehandler kan velge å gi barnetillegg for, se [BarnetilleggKandidat].
 * Barnet har barnetillegg for hele innvilgelsesperioden.
 *
 * @param søknadsopplysninger Opplysningene om barnet fra den nyeste søknaden barnet finnes i, eller null om barnet ikke finnes i en søknad.
 * @param internDeltakelseIder Barnet er bare aktuelt når det innvilges for en av disse tiltaksdeltakelsene.
 * Null når barnet er aktuelt uansett hvilke tiltaksdeltakelser det innvilges for.
 * @param fraGjeldendeVedtak Barnet gir barnetillegg i et gjeldende vedtak.
 */
data class BarnetilleggKandidatDTO(
    val fnr: SladdbarVerdi<String?>,
    val fornavn: SladdbarVerdi<String?>,
    val mellomnavn: SladdbarVerdi<String?>,
    val etternavn: SladdbarVerdi<String?>,
    val fødselsdato: SladdbarVerdi<LocalDate>,
    val kilde: BarnKildeDTO,
    val søknadId: String?,
    val søknadsopplysninger: SøknadsopplysningerDTO?,
    val internDeltakelseIder: List<String>?,
    val fraGjeldendeVedtak: Boolean,
) {
    /** @param oppholdssted null for søknader fra før søknaden spurte om oppholdssted. */
    data class SøknadsopplysningerDTO(
        val oppholderSegIEØSSpm: SøknadDTO.JaNeiSpmDTO,
        val oppholdssted: SøknadDTO.OppholdsstedDTO?,
    )
}

fun List<BarnetilleggKandidat>.tilBarnetilleggKandidaterDTO(): List<BarnetilleggKandidatDTO> = map { kandidat ->
    BarnetilleggKandidatDTO(
        fnr = kandidat.barn.fnr?.verdi.ikkeSladdet(),
        fornavn = kandidat.barn.fornavn.ikkeSladdet(),
        mellomnavn = kandidat.barn.mellomnavn.ikkeSladdet(),
        etternavn = kandidat.barn.etternavn.ikkeSladdet(),
        fødselsdato = kandidat.barn.fødselsdato.ikkeSladdet(),
        kilde = when (kandidat.barn.kilde) {
            is BarnMedBarnetillegg.Kilde.Søknad -> BarnKildeDTO.SØKNAD
            BarnMedBarnetillegg.Kilde.Saksbehandler -> BarnKildeDTO.SAKSBEHANDLER
        },
        søknadId = (kandidat.barn.kilde as? BarnMedBarnetillegg.Kilde.Søknad)?.søknadId?.toString(),
        søknadsopplysninger = kandidat.søknadsopplysninger?.let {
            BarnetilleggKandidatDTO.SøknadsopplysningerDTO(
                oppholderSegIEØSSpm = it.oppholderSegIEØS.toDTO(),
                oppholdssted = it.oppholdssted?.toDTO(),
            )
        },
        internDeltakelseIder = kandidat.tiltaksdeltakerIder?.map { it.toString() }?.sorted(),
        fraGjeldendeVedtak = kandidat.fraGjeldendeVedtak,
    )
}

/**
 * Sladding av [BarnetilleggKandidatDTO].
 * Personopplysningene om barnet sladdes som i søknaden.
 * Svarene fra søknaden om opphold er ikke identifiserende og beholdes.
 */
fun BarnetilleggKandidatDTO.sladdet(): BarnetilleggKandidatDTO = this.copy(
    fnr = SladdetVerdi,
    fornavn = SladdetVerdi,
    mellomnavn = SladdetVerdi,
    etternavn = SladdetVerdi,
    fødselsdato = SladdetVerdi,
)
