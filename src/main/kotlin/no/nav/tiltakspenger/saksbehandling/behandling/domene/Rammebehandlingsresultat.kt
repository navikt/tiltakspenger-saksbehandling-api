package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.trekkFra
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse

sealed interface Rammebehandlingsresultat {

    /** Kan være null ved sære tilfeller av avslag, og når behandlingen er uferdig */
    val vedtaksperiode: Periode?

    /** Vil kun være satt for søknadsbehandling, forlengelse, revurdering (inkl. omgjøring) til innvilgelse. */
    val innvilgelsesperioder: Innvilgelsesperioder?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val valgteTiltaksdeltakelser: IkkeTomPeriodisering<Tiltaksdeltakelse>?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode>?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val barnetillegg: Barnetillegg?

    val omgjørRammevedtak: OmgjørRammevedtak

    /**
     * Sier noe om tilstanden til resultatet. Om det er klart for å sendes til beslutter og/eller iverksettes.
     *
     * @param saksopplysninger Kan være null dersom søker ikke har søkt på et tiltak [no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad], gir kun mening ved avslag.
     */
    fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean

    /** Denne benyttes både i søknadsbehandlinger og revurderinger */
    sealed interface Innvilgelse : Rammebehandlingsresultat {
        /** Kan være null fram til resultatet er ferdigutfylt. */
        override val innvilgelsesperioder: Innvilgelsesperioder?

        /**
         * True dersom [innvilgelsesperioder] og [barnetillegg] ikke er null og er utfylt med gyldige perioder
         * TODO abn: Skriv om denne til å enten kaste exception eller returnere left for spesifikke feil, så det er litt enklere å feilsøke
         */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean {
            if (innvilgelsesperioder == null) {
                return false
            }

            if (!innvilgelsesperioder!!.erInnenforTiltaksperiodene(saksopplysninger)) {
                return false
            }

            if (!validerBarnetillegg()) {
                return false
            }

            return true
        }

        private fun validerBarnetillegg(): Boolean {
            if (barnetillegg == null) {
                return false
            }

            // Alle barnetilleggsperiodene må overlappe fullstendig med innvilgelsesperiodene
            val ikkeOverlappendePerioder = barnetillegg!!.periodisering.perioder.trekkFra(innvilgelsesperioder!!.perioder)

            return ikkeOverlappendePerioder.isEmpty()
        }
    }

    fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Rammebehandlingsresultat?>
}

sealed interface BehandlingResultatType

enum class SøknadsbehandlingType : BehandlingResultatType {
    INNVILGELSE,
    AVSLAG,
}

enum class RevurderingType : BehandlingResultatType {
    STANS,
    INNVILGELSE,
    OMGJØRING,
}

/**
 * Kommentar jah: Håper vi kan bli kvitt en del av denne funksjonen og flytte den inn i BehandlingResultat-implementasjonene.
 * Dersom vi tillater behandlingene og være "dirty", er det eneste vi må påse at vi nullstiller ValgteTiltaksdeltakelser dersom de forvinner eller perioden krymper.
 */
@Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
fun skalNullstilleResultatVedNyeSaksopplysninger(
    valgteTiltaksdeltakelser: List<Tiltaksdeltakelse>,
    nyeSaksopplysninger: Saksopplysninger,
): Boolean {
    return if (valgteTiltaksdeltakelser.size != nyeSaksopplysninger.tiltaksdeltakelser.size) {
        true
    } else {
        (
            valgteTiltaksdeltakelser.sortedBy { it.internDeltakelseId }
                .zip(nyeSaksopplysninger.tiltaksdeltakelser.sortedBy { it.internDeltakelseId }) { forrige, nye ->
                    // Vi nullstiller resultatet og vedtaksperioden dersom det har kommet nye tiltaksdeltakelser eller noen er fjernet. Nullstiller også dersom periodene har endret seg.
                    forrige.internDeltakelseId != nye.internDeltakelseId ||
                        forrige.deltakelseFraOgMed != nye.deltakelseFraOgMed ||
                        forrige.deltakelseTilOgMed != nye.deltakelseTilOgMed
                }.any { it }
            )
    }
}
