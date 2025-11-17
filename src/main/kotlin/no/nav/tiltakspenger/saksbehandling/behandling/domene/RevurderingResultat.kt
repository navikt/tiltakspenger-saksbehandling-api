package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

sealed interface RevurderingResultat : BehandlingResultat {

    override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): RevurderingResultat

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
        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean {
            return valgtHjemmel.isNotEmpty() && stansperiode != null
        }

        /** En stans er ikke avhengig av saksopplysningene */
        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Stans = this

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

        override fun oppdaterSaksopplysninger(oppdaterteSaksopplysninger: Saksopplysninger): Innvilgelse {
            return if (valgteTiltaksdeltakelser == null || skalNullstilleResultatVedNyeSaksopplysninger(
                    valgteTiltaksdeltakelser,
                    oppdaterteSaksopplysninger,
                )
            ) {
                nullstill()
            } else {
                this
            }
        }

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
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
        override val barnetillegg: Barnetillegg,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
        val omgjørRammevedtak: Rammevedtak,
    ) : RevurderingResultat,
        BehandlingResultat.Innvilgelse {

        // Kommentar jah: Avventer med å extende BehandlingResultat.Innvilgelse inntil vi har på plass periodisering av innvilgelsesperioden.
        // Det er ikke sikkert at vi ønsker å gjenbruke logikken derfra.

        // Abn: extender Innvilgelse for nå, slik at Omgjøring mappes til innvilgelse ved exhaustive mappinger for vedtak, statistikk osv.
        // Fjernes når omgjøring ikke lengre alltid skal føre til innvilgelse. Må da ha en annen mekanisme for å avgjøre om omgjøringen er en innvilgelse

        fun oppdater(
            innvilgelsesperiode: Periode,
            valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser,
            barnetillegg: Barnetillegg,
            antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>,
            saksopplysninger: Saksopplysninger,
        ): Omgjøring {
            require(
                resetTiltaksdeltagelserDersomDeErInkompatible(
                    valgteTiltaksdeltakelser,
                    saksopplysninger.tiltaksdeltagelser,
                ) == valgteTiltaksdeltakelser,
            ) {
                // Dersom vi denne kaster og vi savner mer sakskontekst, bør denne returnere Either, slik at callee kan håndtere feilen.
                "Valgte tiltaksdeltakelser er ikke kompatible med saksopplysninger.tiltaksdeltagelser."
            }
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

        /**
         * Validerer [oppdaterteSaksopplysninger] opp mot resultatet.
         * Det finnes tenkte ugyldige tilstander, som f.eks. at den [valgteTiltaksdeltakelser] ikke lenger matcher tiltaksdeltagelsen i [oppdaterteSaksopplysninger].
         */
        override fun oppdaterSaksopplysninger(
            oppdaterteSaksopplysninger: Saksopplysninger,
        ): Omgjøring {
            val innvilgelsesperiode =
                oppdaterteSaksopplysninger.tiltaksdeltagelser.totalPeriode?.overlappendePeriode(innvilgelsesperiode)
                    ?: throw IllegalArgumentException(
                        "Kan kun starte omgjøring dersom vi kan innvilge minst en dag. En ren opphørsomgjøring kommer senere.",
                    )
            val valgteTiltaksdeltakelser = valgteTiltaksdeltakelser?.krympPeriode(innvilgelsesperiode)
            val barnetillegg = barnetillegg.krympPeriode(innvilgelsesperiode)
            val antallDagerPerMeldeperiode =
                antallDagerPerMeldeperiode.krympPeriode(innvilgelsesperiode) as SammenhengendePeriodisering<AntallDagerForMeldeperiode>
            return Omgjøring(
                virkningsperiode = virkningsperiode,
                innvilgelsesperiode = innvilgelsesperiode,
                valgteTiltaksdeltakelser = resetTiltaksdeltagelserDersomDeErInkompatible(
                    valgteTiltaksdeltakelser,
                    oppdaterteSaksopplysninger.tiltaksdeltagelser,
                ),
                barnetillegg = barnetillegg,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                omgjørRammevedtak = omgjørRammevedtak,
            )
        }

        override fun erFerdigutfylt(saksopplysninger: Saksopplysninger): Boolean {
            if (antallDagerPerMeldeperiode.totalPeriode != innvilgelsesperiode) return false
            if (valgteTiltaksdeltakelser == null) return false
            if (valgteTiltaksdeltakelser.periodisering.totalPeriode != innvilgelsesperiode) return false
            if (barnetillegg.periodisering.totalPeriode != innvilgelsesperiode) return false
            if (saksopplysninger.tiltaksdeltagelser.isEmpty()) return false
            valgteTiltaksdeltakelser.periodisering.forEach { (deltakelse, periode) ->
                val saksopplysningsDeltakelse =
                    saksopplysninger.tiltaksdeltagelser.getTiltaksdeltagelse(deltakelse.eksternDeltagelseId)
                        ?: return false
                return saksopplysningsDeltakelse.periode?.inneholderHele(periode) ?: false
            }
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

        companion object {
            fun create(
                omgjørRammevedtak: Rammevedtak,
                saksopplysninger: Saksopplysninger,
            ): Omgjøring {
                val innvilgelsesperiode = omgjørRammevedtak.innvilgelsesperiode?.let {
                    // Vi har en generell begrensning om innvilgelseserperioden ikke kan være større enn tiltaksdeltagelsene.
                    // TODO ved flere innvilgelsesperioder: endre denne logikken
                    saksopplysninger.tiltaksdeltagelser.totalPeriode?.overlappendePeriode(it)
                        ?: throw IllegalArgumentException(
                            "Kan kun starte omgjøring dersom vi kan innvilge minst en dag. En ren opphørsomgjøring kommer senere. sakId: ${omgjørRammevedtak.sakId}, tidligere innvilgelsesperiode: ${omgjørRammevedtak.innvilgelsesperiode}, nye tiltaksdeltagelser: ${saksopplysninger.tiltaksdeltagelser}",
                        )
                } ?: omgjørRammevedtak.periode
                val valgteTiltaksdeltakelser =
                    omgjørRammevedtak.valgteTiltaksdeltakelser!!.krympPeriode(innvilgelsesperiode)
                val barnetillegg = omgjørRammevedtak.barnetillegg!!.krympPeriode(innvilgelsesperiode)
                val antallDagerPerMeldeperiode =
                    omgjørRammevedtak.antallDagerPerMeldeperiode!!.krympPeriode(innvilgelsesperiode) as SammenhengendePeriodisering<AntallDagerForMeldeperiode>
                return Omgjøring(
                    // Ved opprettelse defaulter vi bare til det gamle vedtaket. Dette kan endres av saksbehandler hvis det er perioden de skal endre.
                    virkningsperiode = omgjørRammevedtak.periode,
                    // Hvis vedtaket vi omgjør er en delvis innvilgelse, så bruker vi denne.
                    innvilgelsesperiode = innvilgelsesperiode,
                    valgteTiltaksdeltakelser = resetTiltaksdeltagelserDersomDeErInkompatible(
                        valgteTiltaksdeltakelser,
                        saksopplysninger.tiltaksdeltagelser,
                    ),
                    barnetillegg = barnetillegg,
                    antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                    omgjørRammevedtak = omgjørRammevedtak,
                )
            }
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

/**
 * Resetter [valgteTiltaksdeltakelser] dersom noen av tiltaksdeltagelsene ikke lenger finnes i [oppdaterteTiltaksdeltagelser].
 */
private fun resetTiltaksdeltagelserDersomDeErInkompatible(
    valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
    oppdaterteTiltaksdeltagelser: Tiltaksdeltagelser,
): ValgteTiltaksdeltakelser? {
    if (valgteTiltaksdeltakelser == null || oppdaterteTiltaksdeltagelser.isEmpty()) return null
    valgteTiltaksdeltakelser.periodisering.forEach { (verdi, _) ->
        oppdaterteTiltaksdeltagelser.getTiltaksdeltagelse(verdi.eksternDeltagelseId) ?: return null
    }
    return valgteTiltaksdeltakelser
}
