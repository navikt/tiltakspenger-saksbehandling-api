package no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat.IkkeValgt
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak

/**
 * Omgjør ett eller flere tidligere vedtak helt eller delvis.
 * Den nye vedtaksperioden kan være større, mindre eller samme som tidligere, men må overlappe med minst ett tidligere vedtak.
 * Perioden som omgjøres kan ha hull, både innad i et vedtak og mellom vedtakene.
 * Dersom vedtaksperioden går utenfor periodene som omgjøres, må den overskytende delen være innvilgelse.
 * Kan føre til en innvilgelse, delvis innvilgelse eller opphør.
 * Ved delvis innvilgelse vil den resterende implisitt ikke gi rett.
 * Så lenge behandlingen er under behandling, kan innvilgelsesperiodene gå utenfor tiltaksdeltakelsene, dersom de har endret seg etter at innvilgelsen ble fylt ut.
 * Saksbehandler må da rette innvilgelsen før den kan lagres eller sendes til beslutning, se [no.nav.tiltakspenger.saksbehandling.behandling.domene.UgyldigInnvilgelsesperiode].
 * En omgjøring er den eneste behandlingen som kan innvilge perioder uten gyldig tiltaksdeltakelse, som reversert opphør.
 * Se [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør].
 *
 * @param vedtaksperiode Tilsvarer den nye vedtaksperioden.
 * @param innvilgelsesperioder Perioder som kun inneholder dager med rett.
 * Må være delperiode(r) av [vedtaksperiode].
 */
sealed interface Omgjøringsresultat : Revurderingsresultat {

    data class OmgjøringInnvilgelse(
        override val vedtaksperiode: Periode,
        override val innvilgelsesperioder: Innvilgelsesperioder?,
        override val barnetillegg: Barnetillegg?,
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : Omgjøringsresultat,
        Rammebehandlingsresultat.Innvilgelse {
        override val valgteTiltaksdeltakelser = innvilgelsesperioder?.valgteTiltaksdeltagelser
        override val antallDagerPerMeldeperiode = innvilgelsesperioder?.antallDagerPerMeldeperiode

        /**
         * Innvilgelsen krympes eller nullstilles ikke dersom tiltaksdeltakelsene er endret.
         * Se [Innvilgelsesperioder.medOppdaterteTiltaksdeltakelser].
         */
        override fun oppdaterSaksopplysninger(
            oppdaterteSaksopplysninger: Saksopplysninger,
        ): Either<KunneIkkeOppdatereSaksopplysninger, OmgjøringInnvilgelse> {
            return this.copy(
                innvilgelsesperioder = innvilgelsesperioder?.medOppdaterteTiltaksdeltakelser(oppdaterteSaksopplysninger.tiltaksdeltakelser),
            ).right()
        }

        init {
            require(vedtaksperiode.overlapperMed(omgjørRammevedtak.perioder)) {
                "Vedtaksperioden ($vedtaksperiode) må overlappe med omgjort rammevedtak sin(e) periode(r): ${omgjørRammevedtak.perioder}"
            }

            require(omgjørRammevedtak.size >= 1) {
                "En omgjøring må omgjøre minst ett vedtak"
            }

            if (innvilgelsesperioder != null) {
                require(vedtaksperiode.inneholderHele(innvilgelsesperioder.totalPeriode)) {
                    "Vedtaksperioden ($vedtaksperiode) må inneholde alle innvilgelsesperiodene ($innvilgelsesperioder)"
                }

                val omgjortFraOgMed = omgjørRammevedtak.fraOgMed!!
                val omgjortTilOgMed = omgjørRammevedtak.tilOgMed!!

                if (vedtaksperiode.fraOgMed < omgjortFraOgMed) {
                    require(innvilgelsesperioder.fraOgMed == vedtaksperiode.fraOgMed) {
                        "Når vedtaksperioden sin fraOgMed (${vedtaksperiode.fraOgMed}) starter før de omgjorte vedtakene sin fraOgMed ($omgjortFraOgMed), må innvilgelsesperioden sin fraOgMed (${innvilgelsesperioder.fraOgMed}) være lik vedtaksperioden"
                    }
                }

                if (vedtaksperiode.tilOgMed > omgjortTilOgMed) {
                    require(innvilgelsesperioder.tilOgMed == vedtaksperiode.tilOgMed) {
                        "Når vedtaksperioden sin tilOgMed (${vedtaksperiode.tilOgMed}) slutter etter de omgjorte vedtakene sin tilOgMed ($omgjortTilOgMed), må innvilgelsesperioden sin tilOgMed (${innvilgelsesperioder.tilOgMed}) være lik vedtaksperioden"
                    }
                }
            }
        }
    }

    data class OmgjøringOpphør(
        override val vedtaksperiode: Periode,
        override val omgjørRammevedtak: OmgjørRammevedtak,
        val valgteHjemler: NonEmptySet<HjemmelForOpphør>,
    ) : Omgjøringsresultat {
        override val innvilgelsesperioder = null
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null

        /** Opphør er ikke avhengig av saksopplysningene */
        override fun oppdaterSaksopplysninger(
            oppdaterteSaksopplysninger: Saksopplysninger,
        ): Either<KunneIkkeOppdatereSaksopplysninger, OmgjøringOpphør> {
            return this.right()
        }

        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean {
            return true
        }

        companion object {
            val hjemlerSomMåHaFritekst: Set<HjemmelForOpphør> = setOf(
                HjemmelForOpphør.FremmetForSent,
                HjemmelForOpphør.Alder,
            )
        }
    }

    data class OmgjøringIkkeValgt(
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : Omgjøringsresultat,
        IkkeValgt {
        override val vedtaksperiode = null
        override val innvilgelsesperioder = null
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null

        override fun oppdaterSaksopplysninger(
            oppdaterteSaksopplysninger: Saksopplysninger,
        ): Either<KunneIkkeOppdatereSaksopplysninger, OmgjøringIkkeValgt> {
            return this.right()
        }

        /** IkkeValgt er aldri en ferdig utfylling */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean {
            return false
        }

        override fun vedtakError(): Nothing {
            throw IllegalStateException("Omgjøring uten valgt resultat kan ikke vedtas")
        }
    }
}
