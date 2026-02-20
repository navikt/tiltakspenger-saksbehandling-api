package no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat.IkkeValgt
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode

/**
 * Omgjør det tidligere vedtaket helt eller delvis.
 * Den nye vedtaksperioden kan være større, mindre eller samme som tidligere, men må overlappe med det tidligere vedtaket.
 * Dersom den er større, må den overskytende delen være innvilgelse.
 * Kan føre til en innvilgelse, delvis innvilgelse eller opphør. Ved delvis innvilgelse vil den resterende implisitt ikke gi rett.
 * Tanken er at så lenge behandlingen er under behandling, kan innvilgelsesperioden være større enn tiltaksdeltakelsen (for å støtte at den har krympet uten å måtte resette store deler av behandlingen. Tanken er at saksbehandler kan gjøre det selv).
 *
 * @param vedtaksperiode Tilsvarer den nye vedtaksperioden.
 * @param innvilgelsesperioder Perioder som kun inneholder dager med rett. Må være delperiode(r) av [vedtaksperiode].
 */
sealed interface Omgjøringsresultat : Revurderingsresultat {

    // Per 27. nov 2025 krever vi at en omgjøringsbehandling omgjør ett enkelt vedtak, men vi har ikke noen begrensning på å utvide omgjøringen, slik at den omgjør flere vedtak.
    // Tanken med dette feltet er de tilfellene man har spesifikt valgt å omgjøre et spesifikt vedtak i sin helhet.
    // TODO jah: Anders, hva gjør vi? Legger tilbake omgjørVedtakId? Det føles forvirrende. Skal vi heller sperre for at den kan omgjøre flere vedtak?
    val omgjortVedtak: Omgjøringsperiode get() = omgjørRammevedtak.single()

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
         * Validerer [oppdaterteSaksopplysninger] opp mot resultatet.
         * Det finnes tenkte ugyldige tilstander, som f.eks. at den [valgteTiltaksdeltakelser] ikke lenger matcher tiltaksdeltakelsen i [oppdaterteSaksopplysninger].
         */
        override fun oppdaterSaksopplysninger(
            oppdaterteSaksopplysninger: Saksopplysninger,
        ): Either<KunneIkkeOppdatereSaksopplysninger, OmgjøringInnvilgelse> {
            val innvilgelsesperioder =
                innvilgelsesperioder?.oppdaterTiltaksdeltakelser(oppdaterteSaksopplysninger.tiltaksdeltakelser)

            val barnetillegg =
                innvilgelsesperioder?.let { barnetillegg?.krympTilPerioder(innvilgelsesperioder.perioder) }

            return this.copy(
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
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

                if (vedtaksperiode.fraOgMed < omgjortVedtak.periode.fraOgMed) {
                    require(innvilgelsesperioder.fraOgMed == vedtaksperiode.fraOgMed) {
                        "Når vedtaksperioden sin fraOgMed (${vedtaksperiode.fraOgMed}) starter før det omgjorte vedtaket sin fraOgMed (${omgjortVedtak.periode.fraOgMed}), må innvilgelsesperioden sin fraOgMed (${innvilgelsesperioder.fraOgMed}) være lik vedtaksperioden"
                    }
                }

                if (vedtaksperiode.tilOgMed > omgjortVedtak.periode.tilOgMed) {
                    require(innvilgelsesperioder.tilOgMed == vedtaksperiode.tilOgMed) {
                        "Når vedtaksperioden sin tilOgMed (${vedtaksperiode.tilOgMed}) slutter etter det omgjorte vedtaket sin tilOgMed (${omgjortVedtak.periode.tilOgMed}), må innvilgelsesperioden sin tilOgMed (${innvilgelsesperioder.tilOgMed}) være lik vedtaksperioden"
                    }
                }
            }
        }
    }

    data class OmgjøringOpphør(
        override val vedtaksperiode: Periode,
        override val omgjørRammevedtak: OmgjørRammevedtak,
        val valgteHjemler: NonEmptySet<HjemmelForStansEllerOpphør>,
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
            val hjemlerSomMåHaFritekst: Set<HjemmelForStansEllerOpphør> = setOf(
                HjemmelForStansEllerOpphør.FremmetForSent,
                HjemmelForStansEllerOpphør.Alder,
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
