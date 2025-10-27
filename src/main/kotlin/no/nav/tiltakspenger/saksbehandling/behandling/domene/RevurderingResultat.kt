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
     * @param virkningsperiode Tilsvarer den nye vedtaksperioden. Må minst være like stor som vedtaket du omgjør/erstatter. Kan inneholde en kombinasjon av Rett og Ikke rett. Må være lik eller større enn [omgjørRammevedtak] sin periode.
     * @param innvilgelsesperiode Periode som kun inneholder dager med rett. Må være en delperiode av [virkningsperiode].
     */
    data class Omgjøring(
        override val virkningsperiode: Periode,
        override val innvilgelsesperiode: Periode,
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser,
        override val barnetillegg: Barnetillegg,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
        val omgjørRammevedtak: Rammevedtak,
    ) : RevurderingResultat {

        // Kommentar jah: Avventer med å extende BehandlingResultat.Innvilgelse inntil vi har på plass periodisering av innvilgelsesperioden.
        // Det er ikke sikkert at vi ønsker å gjenbruke logikken derfra.

        constructor(
            omgjørRammevedtak: Rammevedtak,
        ) : this(
            // Ved opprettelse defaulter vi bare til det gamle vedtaket. Dette kan endres av saksbehandler hvis det er perioden de skal endre.
            virkningsperiode = omgjørRammevedtak.periode,
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
            return this.copy(
                virkningsperiode = Periode(
                    minOf(this.virkningsperiode.fraOgMed, innvilgelsesperiode.fraOgMed),
                    maxOf(this.virkningsperiode.tilOgMed, innvilgelsesperiode.tilOgMed),
                ),
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
            require(virkningsperiode.inneholderHele(omgjørRammevedtak.periode)) {
                "Virkningsperioden ($virkningsperiode!!) må være lik eller større enn omgjort rammevedtak sin periode (${omgjørRammevedtak.periode})"
            }
            require(virkningsperiode.inneholderHele(innvilgelsesperiode)) {
                "Virkningsperioden ($virkningsperiode) må inneholde hele innvilgelsesperiode ($innvilgelsesperiode)"
            }
            if (virkningsperiode.fraOgMed < omgjørRammevedtak.fraOgMed) {
                require(innvilgelsesperiode.fraOgMed == omgjørRammevedtak.fraOgMed) {
                    "Når virkningsperioden ($virkningsperiode) starter før det omgjorte vedtaket (${omgjørRammevedtak.periode}), må innvilgelsesperiode ($innvilgelsesperiode) starte samtidig som det omgjorte vedtaket (${omgjørRammevedtak.periode})"
                }
            }
            if (virkningsperiode.tilOgMed > omgjørRammevedtak.tilOgMed) {
                require(innvilgelsesperiode.tilOgMed == omgjørRammevedtak.tilOgMed) {
                    "Når virkningsperioden ($virkningsperiode) slutter etter det omgjorte vedtaket (${omgjørRammevedtak.periode}), må innvilgelsesperiode ($innvilgelsesperiode) slutte samtidig som det omgjorte vedtaket (${omgjørRammevedtak.periode})"
                }
            }
        }
    }
}
