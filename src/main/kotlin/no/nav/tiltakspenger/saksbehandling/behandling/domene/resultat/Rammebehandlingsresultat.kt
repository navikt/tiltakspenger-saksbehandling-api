package no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat

import arrow.core.Either
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.trekkFra
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern

sealed interface Rammebehandlingsresultat {

    /** Kan være null ved sære tilfeller av avslag, og når behandlingen er uferdig */
    val vedtaksperiode: Periode?

    /** Vil kun være satt for søknadsbehandling, forlengelse, revurdering (inkl. omgjøring) til innvilgelse. */
    val innvilgelsesperioder: Innvilgelsesperioder?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val valgteTiltaksdeltakelser: IkkeTomPeriodisering<TiltaksdeltakelseIntern>?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode>?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val barnetillegg: Barnetillegg?

    val omgjørRammevedtak: OmgjørRammevedtak

    /**
     * Sier noe om tilstanden til resultatet.
     * Om det er klart for å sendes til beslutter og/eller iverksettes.
     *
     * @param saksopplysninger Kan være null dersom søker ikke har søkt på et tiltak [no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad], gir kun mening ved avslag.
     */
    fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean

    fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Rammebehandlingsresultat>

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
            val ikkeOverlappendePerioder =
                barnetillegg!!.periodisering.perioder.trekkFra(innvilgelsesperioder!!.perioder)

            return ikkeOverlappendePerioder.isEmpty()
        }
    }

    sealed interface IkkeValgt : Rammebehandlingsresultat {
        fun vedtakError(): Nothing
    }
}

enum class SøknadsbehandlingsresultatType {
    INNVILGELSE,
    AVSLAG,
    IKKE_VALGT,
}

enum class RevurderingsresultatType {
    STANS,
    INNVILGELSE,
    OMGJØRING_INNVILGELSE,
    OMGJØRING_OPPHØR,
    OMGJØRING_IKKE_VALGT,
    FORLENGELSE,
    UBESTEMT,
}
