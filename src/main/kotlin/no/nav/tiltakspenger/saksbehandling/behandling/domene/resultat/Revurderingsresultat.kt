package no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ForlengerRammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak

sealed interface Revurderingsresultat : Rammebehandlingsresultat {

    override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Revurderingsresultat>

    /**
     * Når man oppretter en revurdering til stans, lagres det før saksbehandler tar stilling til disse feltene.
     * Alle bør være satt når behandlingen er til beslutning.
     *
     * [vedtaksperiode] og [stansperiode] vil være 1-1 ved denne revurderingstypen. [innvilgelsesperioder] vil alltid være null.
     *
     * @param harValgtStansFraFørsteDagSomGirRett Dersom saksbehandler har valgt at det skal stanses fra første dag som gir rett.
     * Vil være null når man oppretter stansen.
     */
    data class Stans(
        val valgtHjemmel: NonEmptySet<HjemmelForStans>?,
        val harValgtStansFraFørsteDagSomGirRett: Boolean?,
        val stansperiode: Periode?,
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : Revurderingsresultat {

        override val vedtaksperiode = stansperiode
        override val innvilgelsesperioder = null
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null

        /**
         * True dersom [valgtHjemmel] ikke er tom og [stansperiode] ikke er null.
         * Bruker ikke saksopplysninger her, da vi må kunne stanse selv om det ikke er noen tiltaksdeltakelser.
         */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean {
            return !valgtHjemmel.isNullOrEmpty() && stansperiode != null
        }

        /** En stans er ikke avhengig av saksopplysningene */
        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Stans> =
            this.right()

        companion object {
            val empty: Stans = Stans(
                valgtHjemmel = null,
                harValgtStansFraFørsteDagSomGirRett = null,
                stansperiode = null,
                omgjørRammevedtak = OmgjørRammevedtak.empty,
            )
        }
    }

    /**
     * Når man oppretter en revurdering og velger innvilgelse, har man ikke tatt stilling til disse feltene ennå.
     * Alle bør være satt når behandlingen er til beslutning.
     *
     * Vedtaksperioden og innvilgelsesperioden vil være 1-1 ved denne revurderingstypen.
     */
    data class Innvilgelse(
        override val innvilgelsesperioder: Innvilgelsesperioder?,
        override val barnetillegg: Barnetillegg?,
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : Rammebehandlingsresultat.Innvilgelse,
        Revurderingsresultat {
        override val vedtaksperiode = innvilgelsesperioder?.totalPeriode
        override val valgteTiltaksdeltakelser = innvilgelsesperioder?.valgteTiltaksdeltagelser
        override val antallDagerPerMeldeperiode = innvilgelsesperioder?.antallDagerPerMeldeperiode

        fun nullstill() = empty

        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Innvilgelse> {
            return if (valgteTiltaksdeltakelser == null || skalNullstilleResultatVedNyeSaksopplysninger(
                    valgteTiltaksdeltakelser.verdier,
                    oppdaterteSaksopplysninger,
                )
            ) {
                nullstill()
            } else {
                this
            }.right()
        }

        companion object {
            val empty = Innvilgelse(
                barnetillegg = null,
                innvilgelsesperioder = null,
                omgjørRammevedtak = OmgjørRammevedtak.empty,
            )
        }
    }

    /**
     * Forlenger et gjeldende, innvilget rammevedtak fra dagen etter at det slutter.
     * Saksbehandler velger bare ny til og med-dato, og kan eventuelt endre barnetillegget.
     * Tiltaksdeltakelsen og antall dager per meldeperiode videreføres fra siste dag i vedtaket som forlenges.
     *
     * Vedtaksperioden og innvilgelsesperioden vil være 1-1, og starter alltid på [ForlengerRammevedtak.forlengesFraOgMed].
     * Forlengelsen kan ikke overlappe andre vedtak, så den omgjør aldri andre vedtak.
     * Når man oppretter forlengelsen, har man ikke tatt stilling til til og med-datoen ennå, og [innvilgelsesperioder] og [barnetillegg] er null.
     *
     * @param harEndretBarnetillegg Om saksbehandler har valgt barnetillegget selv.
     * Er det false, er barnetillegget utledet fra vedtaket som forlenges, og utledes på nytt ved neste oppdatering.
     */
    data class Forlengelse(
        val forlenger: ForlengerRammevedtak,
        override val innvilgelsesperioder: Innvilgelsesperioder?,
        override val barnetillegg: Barnetillegg?,
        val harEndretBarnetillegg: Boolean,
    ) : Rammebehandlingsresultat.Innvilgelse,
        Revurderingsresultat {
        override val vedtaksperiode = innvilgelsesperioder?.totalPeriode
        override val valgteTiltaksdeltakelser = innvilgelsesperioder?.valgteTiltaksdeltagelser
        override val antallDagerPerMeldeperiode = innvilgelsesperioder?.antallDagerPerMeldeperiode
        override val omgjørRammevedtak = OmgjørRammevedtak.empty

        init {
            if (innvilgelsesperioder != null) {
                require(innvilgelsesperioder.fraOgMed == forlenger.forlengesFraOgMed) {
                    "Forlengelsen må starte ${forlenger.forlengesFraOgMed}, men starter ${innvilgelsesperioder.fraOgMed}"
                }
            }
            if (harEndretBarnetillegg) {
                require(barnetillegg != null) { "Forlengelsen har valgt barnetillegg, men mangler barnetillegg" }
            }
        }

        fun nullstill() = Forlengelse(
            forlenger = forlenger,
            innvilgelsesperioder = null,
            barnetillegg = null,
            harEndretBarnetillegg = false,
        )

        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Forlengelse> {
            return if (valgteTiltaksdeltakelser == null || skalNullstilleResultatVedNyeSaksopplysninger(
                    valgteTiltaksdeltakelser.verdier,
                    oppdaterteSaksopplysninger,
                )
            ) {
                nullstill()
            } else {
                this
            }.right()
        }

        companion object {
            fun opprett(forlenger: ForlengerRammevedtak) = Forlengelse(
                forlenger = forlenger,
                innvilgelsesperioder = null,
                barnetillegg = null,
                harEndretBarnetillegg = false,
            )
        }
    }
}
