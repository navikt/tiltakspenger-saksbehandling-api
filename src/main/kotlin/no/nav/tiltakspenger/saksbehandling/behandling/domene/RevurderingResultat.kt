package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

sealed interface RevurderingResultat : BehandlingResultat {

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
    ) : RevurderingResultat {

        override val virkningsperiode = stansperiode
        override val innvilgelsesperiode = null
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null

        /**
         * True dersom [valgtHjemmel] ikke er tom og [stansperiode] ikke er null.
         * Bruker ikke saksopplysninger her, da vi må kunne stanse selv om det ikke er noen tiltaksdeltakelser.
         */
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger?): Boolean =
            saksopplysninger != null && valgtHjemmel.isNotEmpty() && stansperiode != null

        companion object {
            val empty: Stans = Stans(
                valgtHjemmel = emptyList(),
                harValgtStansFraFørsteDagSomGirRett = null,
                harValgtStansTilSisteDagSomGirRett = null,
                stansperiode = null,
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
        override val innvilgelsesperiode: Periode?,
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
        override val barnetillegg: Barnetillegg?,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
    ) : BehandlingResultat.Innvilgelse,
        RevurderingResultat {
        override val virkningsperiode = innvilgelsesperiode

        fun nullstill() = empty

        companion object {
            val empty = Innvilgelse(
                valgteTiltaksdeltakelser = null,
                barnetillegg = null,
                antallDagerPerMeldeperiode = null,
                innvilgelsesperiode = null,
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
     * @param omgjøringsperiode Tilsvarer den nye vedtaksperioden også kalt virkningsperiode. Kan inneholde en kombinasjon av Rett og Ikke rett. Må være lik eller større enn [omgjørRammevedtak] sin periode.
     * @param innvilgelsesperiode Periode som kun inneholder dager med rett. Må være en delperiode av [omgjøringsperiode].
     */
    data class Omgjøring(
        val omgjøringsperiode: Periode,
        override val innvilgelsesperiode: Periode,
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser,
        override val barnetillegg: Barnetillegg,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
        val omgjørRammevedtak: Rammevedtak,
    ) : RevurderingResultat {

        // Kommentar jah: Avventer med å extende BehandlingResultat.Innvilgelse inntil vi har på plass periodisering av innvilgelsesperioden.
        // Det er ikke sikkert at vi ønsker å gjenbruke logikken derfra.

        override val virkningsperiode = omgjøringsperiode

        constructor(
            omgjørRammevedtak: Rammevedtak,
        ) : this(
            // Ved opprettelse defaulter vi bare til det gamle vedtaket. Dette kan endres av saksbehandler hvis det er perioden de skal endre.
            omgjøringsperiode = omgjørRammevedtak.periode,
            // Hvis vedtaket vi omgjør er en delvis innvilgelse, så bruker vi denne.
            innvilgelsesperiode = omgjørRammevedtak.innvilgelsesperiode ?: omgjørRammevedtak.periode,
            valgteTiltaksdeltakelser = omgjørRammevedtak.valgteTiltaksdeltakelser!!,
            barnetillegg = omgjørRammevedtak.barnetillegg!!,
            antallDagerPerMeldeperiode = omgjørRammevedtak.antallDagerPerMeldeperiode!!,
            omgjørRammevedtak = omgjørRammevedtak,
        )

        fun oppdater(
            innvilgelsesperiode: Periode,
            valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser,
            barnetillegg: Barnetillegg,
            antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
        ): Omgjøring {
            val omgjøringFraOgMed = minOf(this.omgjøringsperiode.fraOgMed, innvilgelsesperiode.fraOgMed)
            val omgjøringTilOgMed = maxOf(this.omgjøringsperiode.tilOgMed, innvilgelsesperiode.tilOgMed)
            return this.copy(
                omgjøringsperiode = Periode(omgjøringFraOgMed, omgjøringTilOgMed),
                innvilgelsesperiode = innvilgelsesperiode,
                valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
                barnetillegg = barnetillegg,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                omgjørRammevedtak = this.omgjørRammevedtak,
            )
        }

        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger?): Boolean {
            if (saksopplysninger == null) return false
            if (antallDagerPerMeldeperiode.totalPeriode != innvilgelsesperiode) return false
            if (valgteTiltaksdeltakelser.periodisering.totalPeriode != innvilgelsesperiode) return false
            if (barnetillegg.periodisering.totalPeriode != innvilgelsesperiode) return false
            if (saksopplysninger.tiltaksdeltagelser.isEmpty()) return false
            if (innvilgelsesperiode.fraOgMed < saksopplysninger.tiltaksdeltagelser.totalPeriode!!.fraOgMed) {
                // Innvilgelsesperioden kan ikke starte før tiltaksdeltagelsene
                return false
            }
            if (innvilgelsesperiode.tilOgMed > saksopplysninger.tiltaksdeltagelser.totalPeriode!!.tilOgMed) {
                // Innvilgelsesperioden kan ikke slutte etter tiltaksdeltagelsene
                return false
            }
            return true
        }

        init {
            require(omgjøringsperiode.inneholderHele(omgjørRammevedtak.periode)) {
                "Omgjøringsperioden ($omgjøringsperiode!!) må være lik eller større enn omgjort rammevedtak sin periode (${omgjørRammevedtak.periode})"
            }
            require(omgjøringsperiode.inneholderHele(innvilgelsesperiode)) {
                "Omgjøringsperioden ($omgjøringsperiode) må inneholde hele innvilgelsesperiode ($innvilgelsesperiode)"
            }
            if (omgjøringsperiode.fraOgMed < omgjørRammevedtak.fraOgMed) {
                require(innvilgelsesperiode.fraOgMed == omgjørRammevedtak.fraOgMed) {
                    "Når omgjøringsperioden ($omgjøringsperiode) starter før det omgjorte vedtaket (${omgjørRammevedtak.periode}), må innvilgelsesperiode ($innvilgelsesperiode) starte samtidig som det omgjorte vedtaket (${omgjørRammevedtak.periode})"
                }
            }
            if (omgjøringsperiode.tilOgMed > omgjørRammevedtak.tilOgMed) {
                require(innvilgelsesperiode.tilOgMed == omgjørRammevedtak.tilOgMed) {
                    "Når omgjøringsperioden ($omgjøringsperiode) slutter etter det omgjorte vedtaket (${omgjørRammevedtak.periode}), må innvilgelsesperiode ($innvilgelsesperiode) slutte samtidig som det omgjorte vedtaket (${omgjørRammevedtak.periode})"
                }
            }
        }
    }
}
