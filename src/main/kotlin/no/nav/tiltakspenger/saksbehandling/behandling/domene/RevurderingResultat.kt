package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

sealed interface RevurderingResultat : BehandlingResultat {

    override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, RevurderingResultat>

    /**
     * Når man oppretter en revurdering til stans, lagres det før saksbehandler tar stilling til disse feltene.
     * Alle bør være satt når behandlingen er til beslutning.
     *
     * Virkningsperioden/vedtaksperioden og innvilgelsesperioden vil være 1-1 ved denne revurderingstypen.
     *
     * @param harValgtStansFraFørsteDagSomGirRett Dersom saksbehandler har valgt at det skal stanses fra første dag som gir rett. Vil være null når man oppretter stansen.
     * @param harValgtStansTilSisteDagSomGirRett Dersom saksbehandler har valgt at det skal stanses til siste dag som gir rett. Vil være null når man oppretter stansen.
     */
    data class Stans(
        val valgtHjemmel: List<ValgtHjemmelForStans>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean?,
        val harValgtStansTilSisteDagSomGirRett: Boolean?,
        val stansperiode: Periode?,
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : RevurderingResultat {

        override val virkningsperiode = stansperiode
        override val innvilgelsesperioder = null
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null

        /**
         * True dersom [valgtHjemmel] ikke er tom og [stansperiode] ikke er null.
         * Bruker ikke saksopplysninger her, da vi må kunne stanse selv om det ikke er noen tiltaksdeltakelser.
         */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean {
            return valgtHjemmel.isNotEmpty() && stansperiode != null
        }

        /** En stans er ikke avhengig av saksopplysningene */
        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Either<KunneIkkeOppdatereSaksopplysninger, Stans> =
            this.right()

        companion object {
            val empty: Stans = Stans(
                valgtHjemmel = emptyList(),
                harValgtStansFraFørsteDagSomGirRett = null,
                harValgtStansTilSisteDagSomGirRett = null,
                stansperiode = null,
                omgjørRammevedtak = OmgjørRammevedtak.empty,
            )
        }
    }

    /**
     * Når man oppretter en revurdering og velger innvilgelse, har man ikke tatt stilling til disse feltene ennå.
     * Alle bør være satt når behandlingen er til beslutning.
     *
     * Virkningsperioden/vedtaksperioden og innvilgelsesperioden vil være 1-1 ved denne revurderingstypen.
     */
    data class Innvilgelse(
        override val innvilgelsesperioder: Innvilgelsesperioder?,
        override val barnetillegg: Barnetillegg?,
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : BehandlingResultat.Innvilgelse,
        RevurderingResultat {
        override val virkningsperiode = innvilgelsesperioder?.totalPeriode
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
     * @param virkningsperiode Tilsvarer den nye vedtaksperioden. Må minst være like stor som vedtaket du omgjør/erstatter. Kan inneholde en kombinasjon av Rett og Ikke rett. Må være lik eller større enn [omgjørRammevedtak] sin periode.
     * @param innvilgelsesperioder Periode som kun inneholder dager med rett. Må være en delperiode av [virkningsperiode].
     */
    data class Omgjøring(
        override val virkningsperiode: Periode,
        override val innvilgelsesperioder: Innvilgelsesperioder?,
        override val barnetillegg: Barnetillegg?,
        override val omgjørRammevedtak: OmgjørRammevedtak,
    ) : RevurderingResultat,
        BehandlingResultat.Innvilgelse {
        override val valgteTiltaksdeltakelser = innvilgelsesperioder?.valgteTiltaksdeltagelser
        override val antallDagerPerMeldeperiode = innvilgelsesperioder?.antallDagerPerMeldeperiode

        // Kommentar jah: Avventer med å extende BehandlingResultat.Innvilgelse inntil vi har på plass periodisering av innvilgelsesperioden.
        // Det er ikke sikkert at vi ønsker å gjenbruke logikken derfra.

        // Abn: extender Innvilgelse for nå, slik at Omgjøring mappes til innvilgelse ved exhaustive mappinger for vedtak, statistikk osv.
        // Fjernes når omgjøring ikke lengre alltid skal føre til innvilgelse. Må da ha en annen mekanisme for å avgjøre om omgjøringen er en innvilgelse

        fun oppdater(
            oppdatertInnvilgelsesperioder: Innvilgelsesperioder,
            oppdatertBarnetillegg: Barnetillegg,
            saksopplysninger: Saksopplysninger,
        ): Omgjøring {
            val innvilgelsesperioderMedTiltaksdeltakelse =
                oppdatertInnvilgelsesperioder.krympTilTiltaksdeltakelsesperioder(saksopplysninger.tiltaksdeltakelser)

            requireNotNull(innvilgelsesperioderMedTiltaksdeltakelse) {
                // Dersom denne kaster og vi savner mer sakskontekst, bør denne returnere Either, slik at callee kan håndtere feilen.
                "Valgte innvilgelsesperioder har ingen overlapp med tiltaksdeltakelser fra saksopplysningene"
            }

            return this.copy(
                virkningsperiode = utledNyVirkningsperiode(this.virkningsperiode, oppdatertInnvilgelsesperioder),
                innvilgelsesperioder = innvilgelsesperioderMedTiltaksdeltakelse,
                barnetillegg = oppdatertBarnetillegg,
                omgjørRammevedtak = this.omgjørRammevedtak,
            )
        }

        /**
         * Validerer [oppdaterteSaksopplysninger] opp mot resultatet.
         * Det finnes tenkte ugyldige tilstander, som f.eks. at den [valgteTiltaksdeltakelser] ikke lenger matcher tiltaksdeltakelsen i [oppdaterteSaksopplysninger].
         */
        override fun oppdaterSaksopplysninger(
            oppdaterteSaksopplysninger: Saksopplysninger,
        ): Either<KunneIkkeOppdatereSaksopplysninger, Omgjøring> {
            val innvilgelsesperioder =
                innvilgelsesperioder?.krympTilTiltaksdeltakelsesperioder(oppdaterteSaksopplysninger.tiltaksdeltakelser)

            val barnetillegg = innvilgelsesperioder?.let { barnetillegg?.krympPerioder(innvilgelsesperioder.perioder) }

            return Omgjøring(
                virkningsperiode = virkningsperiode,
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
                omgjørRammevedtak = omgjørRammevedtak,
            ).right()
        }

        companion object {
            fun create(
                omgjørRammevedtak: Rammevedtak,
                saksopplysninger: Saksopplysninger,
            ): Either<KunneIkkeOppretteOmgjøring, Omgjøring> {
                // Vi har en generell begrensning om innvilgelseserperioden ikke kan være større enn tiltaksdeltakelsene.
                val innvilgelsesperioder =
                    omgjørRammevedtak.innvilgelsesperioder?.krympTilTiltaksdeltakelsesperioder(saksopplysninger.tiltaksdeltakelser)
                        ?: return KunneIkkeOppretteOmgjøring.KanKunStarteOmgjøringDersomViKanInnvilgeMinst1Dag.left()

                val barnetillegg = omgjørRammevedtak.barnetillegg!!.krympPerioder(innvilgelsesperioder.perioder)

                return Omgjøring(
                    // Ved opprettelse defaulter vi bare til det gamle vedtaket. Dette kan endres av saksbehandler hvis det er perioden de skal endre.
                    virkningsperiode = omgjørRammevedtak.periode,
                    // Hvis vedtaket vi omgjør er en delvis innvilgelse, så bruker vi denne.
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetillegg = barnetillegg,
                    omgjørRammevedtak = OmgjørRammevedtak.create(omgjørRammevedtak),
                ).right()
            }

            fun utledNyVirkningsperiode(
                eksisterendeVirkningsperiode: Periode,
                nyeInnvilgelsesperioder: Innvilgelsesperioder,
            ): Periode {
                return Periode(
                    fraOgMed = minOf(eksisterendeVirkningsperiode.fraOgMed, nyeInnvilgelsesperioder.fraOgMed),
                    tilOgMed = maxOf(eksisterendeVirkningsperiode.tilOgMed, nyeInnvilgelsesperioder.tilOgMed),
                )
            }
        }

        init {
            require(omgjørRammevedtak.perioder.all { virkningsperiode.inneholderHele(it) }) {
                "Virkningsperioden ($virkningsperiode) må være lik eller større enn omgjort rammevedtak sin(e) periode(r): ${omgjørRammevedtak.perioder}"
            }

            require(omgjørRammevedtak.size >= 1) {
                "En omgjøring må omgjøre minst ett vedtak"
            }
            require(omgjørRammevedtak.any { it.omgjøringsgrad == Omgjøringsgrad.HELT }) {
                "Minst ett vedtak må være omgjort i sin helhet"
            }

            if (innvilgelsesperioder != null) {
                require(virkningsperiode.inneholderHele(innvilgelsesperioder.totalPeriode)) {
                    "Virkningsperioden ($virkningsperiode) må inneholde alle innvilgelsesperiodene ($innvilgelsesperioder)"
                }

                if (omgjørRammevedtak.fraOgMed != null && virkningsperiode.fraOgMed < omgjørRammevedtak.fraOgMed) {
                    require(innvilgelsesperioder.fraOgMed == virkningsperiode.fraOgMed) {
                        "Når virkningsperioden sin fraOgMed (${virkningsperiode.fraOgMed}) starter før det omgjorte vedtaket sin fraOgMed (${omgjørRammevedtak.fraOgMed}), må innvilgelsesperioden sin fraOgMed (${innvilgelsesperioder.fraOgMed}) starte samtidig som det omgjorte vedtaket sin fraOgMed (${omgjørRammevedtak.fraOgMed})"
                    }
                }
                if (omgjørRammevedtak.tilOgMed != null && virkningsperiode.tilOgMed > omgjørRammevedtak.tilOgMed) {
                    require(innvilgelsesperioder.tilOgMed == virkningsperiode.tilOgMed) {
                        "Når virkningsperioden sin tilOgMed (${virkningsperiode.tilOgMed}) slutter etter det omgjorte vedtaket sin tilOgMed (${omgjørRammevedtak.tilOgMed}), må innvilgelsesperioden sin tilOgMed (${innvilgelsesperioder.tilOgMed}) slutte samtidig som det omgjorte vedtaket sin tilOgMed (${omgjørRammevedtak.tilOgMed})"
                    }
                }

                val heltOmgjort = omgjørRammevedtak.single {
                    it.omgjøringsgrad == Omgjøringsgrad.HELT
                }

                if (virkningsperiode.fraOgMed < heltOmgjort.periode.fraOgMed) {
                    require(innvilgelsesperioder.fraOgMed == virkningsperiode.fraOgMed) {
                        "Når virkningsperioden sin fraOgMed (${virkningsperiode.fraOgMed}) starter før det omgjorte vedtaket sin fraOgMed (${omgjørRammevedtak.fraOgMed}), må innvilgelsesperioden sin fraOgMed (${innvilgelsesperioder.fraOgMed}) starte samtidig som det omgjorte vedtaket sin fraOgMed (${omgjørRammevedtak.fraOgMed})"
                    }
                }

                if (virkningsperiode.tilOgMed > heltOmgjort.periode.tilOgMed) {
                    require(innvilgelsesperioder.tilOgMed == virkningsperiode.tilOgMed) {
                        "Når virkningsperioden sin tilOgMed (${virkningsperiode.tilOgMed}) slutter etter det omgjorte vedtaket sin tilOgMed (${omgjørRammevedtak.tilOgMed}), må innvilgelsesperioden sin tilOgMed (${innvilgelsesperioder.tilOgMed}) slutte samtidig som det omgjorte vedtaket sin tilOgMed (${omgjørRammevedtak.tilOgMed})"
                    }
                }
            }
        }
    }
}
