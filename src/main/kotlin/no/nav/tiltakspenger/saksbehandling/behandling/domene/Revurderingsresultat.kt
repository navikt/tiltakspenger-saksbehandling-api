package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.overlappendePerioder
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

sealed interface Revurderingsresultat : Rammebehandlingsresultat {

    override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Revurderingsresultat>

    /**
     * Når man oppretter en revurdering til stans, lagres det før saksbehandler tar stilling til disse feltene.
     * Alle bør være satt når behandlingen er til beslutning.
     *
     * [vedtaksperiode] og [stansperiode] vil være 1-1 ved denne revurderingstypen. [innvilgelsesperioder] vil alltid være null.
     *
     * @param harValgtStansFraFørsteDagSomGirRett Dersom saksbehandler har valgt at det skal stanses fra første dag som gir rett. Vil være null når man oppretter stansen.
     */
    data class Stans(
        val valgtHjemmel: NonEmptySet<ValgtHjemmelForStans>?,
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
     * Omgjør det tidligere vedtaket i sin helhet.
     * Den nye vedtaksperioden kan være den samme eller større enn det tidligere vedtaket.
     * Dersom den er større, må den overskytende delen være innvilgelse.
     * Kan føre til en innvilgelse, eller delvis innvilgelse (ved delvis innvilgelse vil den resterende implisitt ikke gi rett).
     * Støtter ikke hull i innvilgelsesperioden (enda).
     * Tanken er at så lenge behandlingen er under behandling, kan innvilgelsesperioden være større enn tiltaksdeltakelsen (for å støtte at den har krympet uten å måtte resette store deler av behandlingen. Tanken er at saksbehandler kan gjøre det selv).
     *
     * @param vedtaksperiode Tilsvarer den nye vedtaksperioden. Må minst være like stor som vedtaket du omgjør/erstatter. Kan inneholde en kombinasjon av Rett og Ikke rett. Må være lik eller større enn [omgjørRammevedtak] sin periode.
     * @param innvilgelsesperioder Periode som kun inneholder dager med rett. Må være en delperiode av [vedtaksperiode].
     */
    data class Omgjøring(
        override val vedtaksperiode: Periode,
        override val innvilgelsesperioder: Innvilgelsesperioder?,
        override val barnetillegg: Barnetillegg?,
        override val omgjørRammevedtak: OmgjørRammevedtak,
        val harValgtSkalOmgjøreHeleVedtaksperioden: Boolean,
    ) : Revurderingsresultat,
        Rammebehandlingsresultat.Innvilgelse {
        override val valgteTiltaksdeltakelser = innvilgelsesperioder?.valgteTiltaksdeltagelser
        override val antallDagerPerMeldeperiode = innvilgelsesperioder?.antallDagerPerMeldeperiode

        // Per 27. nov 2025 krever vi at en omgjøringsbehandling omgjør ett enkelt vedtak, men vi har ikke noen begrensning på å utvide omgjøringen, slik at den omgjør flere vedtak.
        // Tanken med dette feltet er de tilfellene man har spesifikt valgt å omgjøre et spesifikt vedtak i sin helhet.
        // TODO jah: Anders, hva gjør vi? Legger tilbake omgjørVedtakId? Det føles forvirrende. Skal vi heller sperre for at den kan omgjøre flere vedtak?
        val omgjortVedtak: Omgjøringsperiode by lazy {
            omgjørRammevedtak.single()
        }

        // Kommentar jah: Avventer med å extende BehandlingResultat.Innvilgelse inntil vi har på plass periodisering av innvilgelsesperioden.
        // Det er ikke sikkert at vi ønsker å gjenbruke logikken derfra.

        // Abn: extender Innvilgelse for nå, slik at Omgjøring mappes til innvilgelse ved exhaustive mappinger for vedtak, statistikk osv.
        // Fjernes når omgjøring ikke lengre alltid skal føre til innvilgelse. Må da ha en annen mekanisme for å avgjøre om omgjøringen er en innvilgelse

        fun oppdater(
            oppdatertInnvilgelsesperioder: Innvilgelsesperioder,
            oppdatertBarnetillegg: Barnetillegg,
            nyVedtaksperiode: Periode,
            omgjørRammevedtak: OmgjørRammevedtak,
        ): Either<KanIkkeOppdatereBehandling, Omgjøring> {
            require(nyVedtaksperiode.inneholderHele(oppdatertInnvilgelsesperioder.totalPeriode)) {
                "Valgt vedtaksperiode ($nyVedtaksperiode) må inneholde alle innvilgelsesperiodene (${oppdatertInnvilgelsesperioder.totalPeriode})"
            }

            if (omgjørRammevedtak.perioder.size != 1) {
                return KanIkkeOppdatereBehandling.PerioderSomOmgjøresMåVæreSammenhengede.left()
            }

            return this.copy(
                vedtaksperiode = nyVedtaksperiode,
                innvilgelsesperioder = oppdatertInnvilgelsesperioder,
                barnetillegg = oppdatertBarnetillegg,
                omgjørRammevedtak = omgjørRammevedtak,
                harValgtSkalOmgjøreHeleVedtaksperioden = nyVedtaksperiode != null,
            ).right()
        }

        /**
         * Validerer [oppdaterteSaksopplysninger] opp mot resultatet.
         * Det finnes tenkte ugyldige tilstander, som f.eks. at den [valgteTiltaksdeltakelser] ikke lenger matcher tiltaksdeltakelsen i [oppdaterteSaksopplysninger].
         */
        override fun oppdaterSaksopplysninger(
            oppdaterteSaksopplysninger: Saksopplysninger,
        ): Either<KunneIkkeOppdatereSaksopplysninger, Omgjøring> {
            val innvilgelsesperioder =
                innvilgelsesperioder?.oppdaterTiltaksdeltakelser(oppdaterteSaksopplysninger.tiltaksdeltakelser)

            val barnetillegg =
                innvilgelsesperioder?.let { barnetillegg?.krympTilPerioder(innvilgelsesperioder.perioder) }

            return this.copy(
                vedtaksperiode = if (innvilgelsesperioder == null) {
                    omgjortVedtak.periode
                } else {
                    utledNyVedtaksperiode(
                        omgjortVedtak.periode,
                        innvilgelsesperioder.totalPeriode,
                    )
                },
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
            ).right()
        }

        companion object {
            fun create(
                omgjørRammevedtak: Rammevedtak,
                saksopplysninger: Saksopplysninger,
            ): Either<KunneIkkeOppretteOmgjøring, Omgjøring> {
                val perioderSomKanInnvilges = omgjørRammevedtak.gjeldendePerioder
                    .overlappendePerioder(saksopplysninger.tiltaksdeltakelser.perioder)

                if (perioderSomKanInnvilges.isEmpty()) {
                    return KunneIkkeOppretteOmgjøring.KanKunStarteOmgjøringDersomViKanInnvilgeMinst1Dag.left()
                }

                val innvilgelsesperioder = omgjørRammevedtak.innvilgelsesperioder
                    ?.krymp(perioderSomKanInnvilges)
                    ?.oppdaterTiltaksdeltakelser(saksopplysninger.tiltaksdeltakelser)

                val barnetillegg = innvilgelsesperioder?.let {
                    omgjørRammevedtak.barnetillegg!!.krympTilPerioder(it.perioder)
                }

                return Omgjøring(
                    // Ved opprettelse defaulter vi bare til det gamle vedtaket. Dette kan endres av saksbehandler hvis det er perioden de skal endre.
                    vedtaksperiode = omgjørRammevedtak.gjeldendeTotalPeriode!!,
                    // Hvis vedtaket vi omgjør er en delvis innvilgelse, så bruker vi denne.
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetillegg = barnetillegg,
                    omgjørRammevedtak = OmgjørRammevedtak.create(omgjørRammevedtak),
                    harValgtSkalOmgjøreHeleVedtaksperioden = true,
                ).right()
            }

            // Ny vedtaksperiode må alltid inneholde hele perioden for vedtaket som omgjøres
            fun utledNyVedtaksperiode(
                omgjortVedtaksperiode: Periode,
                nyInnvilgelsesperiode: Periode,
            ): Periode {
                return Periode(
                    fraOgMed = minOf(omgjortVedtaksperiode.fraOgMed, nyInnvilgelsesperiode.fraOgMed),
                    tilOgMed = maxOf(omgjortVedtaksperiode.tilOgMed, nyInnvilgelsesperiode.tilOgMed),
                )
            }
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
}
