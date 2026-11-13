package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.inneholderOverlapp
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periode.trekkFra
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.slåSammenTilstøtendePerioder
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeVerdi
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.UgyldigInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.delOppEtterOverlapp
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * Innvilgelsesperiodene, med periodene som innvilges for hver tiltaksdeltakelse, og antall dager per meldeperiode som innvilges i periodene.
 *
 * Domenet deles opp i delperioder ved alle start- og sluttdatoer for periodene i [tiltak], og hver delperiode får tiltaksdeltakelsene som dekker den.
 * Antall dager per meldeperiode er felles for alle tiltaksdeltakelsene i en delperiode.
 *
 * @param tiltak Én innvilgelse per tiltaksdeltakelse, se [InnvilgelseForTiltakDTO].
 * @param antallDagerPerMeldeperiode Antall dager per meldeperiode for periodene.
 * Periodene kan ikke overlappe, og i requester må hver delperiode ligge helt innenfor én av dem, og de kan ikke dekke dager utenfor delperiodene.
 * I responser er tilstøtende perioder med likt antall dager slått sammen.
 */
data class InnvilgelsesperioderDTO(
    val tiltak: List<InnvilgelseForTiltakDTO>,
    val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>,
)

/**
 * Innvilgelsen for én tiltaksdeltakelse, med periodene som innvilges for den.
 * Periodene for ulike tiltaksdeltakelser kan overlappe, og domenet får da flere tiltaksdeltakelser i de overlappende periodene.
 * Rekkefølgen på tiltaksdeltakelsene bestemmer rekkefølgen i domenet, der den første bestemmer tiltakstypen for perioden.
 * @param erReversertOpphør Periodene innvilges uten gyldig tiltaksdeltakelse, på nytt etter å ha vært opphørt eller stanset.
 * Se [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør].
 * Samme tiltaksdeltakelse kan forekomme både med og uten reversert opphør, men da for ulike perioder.
 * @param perioder Periodene for samme tiltaksdeltakelse kan ikke overlappe.
 */
data class InnvilgelseForTiltakDTO(
    val internDeltakelseId: String,
    val erReversertOpphør: Boolean = false,
    val perioder: List<InnvilgelsesperiodeForTiltakDTO>,
)

/**
 * @param ugyldigTiltaksdeltakelse Settes bare i responser, og er null dersom tiltaksdeltakelsen passer med perioden.
 * Ignoreres i requester.
 * Er alltid null for reversert opphør, siden perioden da ikke valideres mot tiltaksdeltakelsene i saksopplysningene.
 */
data class InnvilgelsesperiodeForTiltakDTO(
    val periode: PeriodeDTO,
    val ugyldigTiltaksdeltakelse: UgyldigTiltaksdeltakelseDTO? = null,
)

/** Antall dager per meldeperiode som innvilges i [periode], felles for alle tiltaksdeltakelsene som innvilges i perioden. */
data class AntallDagerPerMeldeperiodeDTO(
    val periode: PeriodeDTO,
    val antallDagerPerMeldeperiode: Int,
)

/**
 * Valgt tiltaksdeltakelse passer ikke med innvilgelsesperioden i saksopplysningene, se [UgyldigInnvilgelsesperiode].
 * @param deltakelsesperiode Settes bare for [Type.DEKKER_IKKE_PERIODEN].
 */
data class UgyldigTiltaksdeltakelseDTO(
    val type: Type,
    val deltakelsesperiode: PeriodeDTO?,
) {
    enum class Type {
        FINNES_IKKE,
        KAN_IKKE_INNVILGES,
        DEKKER_IKKE_PERIODEN,
    }
}

private data class InnvilgelsesperiodeForTiltak(
    val internDeltakelseId: TiltaksdeltakerId,
    val erReversertOpphør: Boolean,
    val periode: Periode,
)

/**
 * Deler opp periodene for tiltaksdeltakelsene i delperioder ved alle start- og sluttdatoer for periodene, der hver delperiode får tiltaksdeltakelsene som dekker den.
 * Tilstøtende delperioder slås ikke sammen, og dager uten tiltaksdeltakelser blir hull.
 * Hver delperiode får antall dager fra perioden i [InnvilgelsesperioderDTO.antallDagerPerMeldeperiode] som inneholder hele delperioden.
 *
 * Kaster dersom det ikke finnes noen perioder, dersom periodene for samme tiltaksdeltakelse overlapper, eller dersom perioder med og uten reversert opphør overlapper.
 * Kaster også dersom periodene for antall dager overlapper, dekker dager uten tiltaksdeltakelser, eller dersom en delperiode ikke ligger helt innenfor nøyaktig én av dem.
 */
fun InnvilgelsesperioderDTO.tilKommando(): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> {
    val perioderForTiltak = this.tiltak.flatMap { tiltak ->
        val internDeltakelseId = TiltaksdeltakerId.fromString(tiltak.internDeltakelseId)
        tiltak.perioder.map {
            InnvilgelsesperiodeForTiltak(
                internDeltakelseId = internDeltakelseId,
                erReversertOpphør = tiltak.erReversertOpphør,
                periode = it.periode.toDomain(),
            )
        }
    }

    require(perioderForTiltak.isNotEmpty()) { "Må ha minst én innvilgelsesperiode" }

    perioderForTiltak.groupBy { it.internDeltakelseId }.forEach { (internDeltakelseId, perioder) ->
        require(!perioder.map { it.periode }.inneholderOverlapp()) {
            "Periodene for tiltaksdeltakelsen $internDeltakelseId kan ikke overlappe"
        }
    }

    val (reversertOpphør, ikkeReversertOpphør) = perioderForTiltak.partition { it.erReversertOpphør }
    require(reversertOpphør.none { it.periode.overlapperMed(ikkeReversertOpphør.map { annen -> annen.periode }) }) {
        "Perioder som er reversert opphør kan ikke overlappe perioder som ikke er reversert opphør"
    }

    val antallDager = this.antallDagerPerMeldeperiode.map {
        PeriodeMedVerdi(
            verdi = AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
            periode = it.periode.toDomain(),
        )
    }

    require(!antallDager.map { it.periode }.inneholderOverlapp()) {
        "Periodene for antall dager per meldeperiode kan ikke overlappe"
    }

    val totalPeriode = Periode(
        fraOgMed = perioderForTiltak.minOf { it.periode.fraOgMed },
        tilOgMed = perioderForTiltak.maxOf { it.periode.tilOgMed },
    )

    val delperioder = totalPeriode.delOppEtterOverlapp(perioderForTiltak.map { it to it.periode })

    require(antallDager.map { it.periode }.trekkFra(delperioder.map { it.periode }).isEmpty()) {
        "Periodene for antall dager per meldeperiode kan ikke dekke dager uten tiltaksdeltakelser"
    }

    return delperioder
        .map { (perioder, delperiode) ->
            val antallDagerForDelperiode = antallDager.filter { it.periode.inneholderHele(delperiode) }

            require(antallDagerForDelperiode.size == 1) {
                "Delperioden $delperiode må ligge helt innenfor nøyaktig én periode for antall dager per meldeperiode"
            }

            PeriodeMedVerdi(
                periode = delperiode,
                verdi = InnvilgelsesperiodeKommando(
                    periode = delperiode,
                    internDeltakelseIder = perioder.map { it.internDeltakelseId },
                    antallDagerPerMeldeperiode = antallDagerForDelperiode.single().verdi,
                    erReversertOpphør = perioder.head.erReversertOpphør,
                ),
            )
        }
        .tilIkkeTomPeriodisering()
}

/**
 * Antall dager per meldeperiode er periodene i domenet, der tilstøtende perioder med likt antall dager er slått sammen.
 *
 * Tiltakene har én innvilgelse per tiltaksdeltakelse, i rekkefølgen tiltaksdeltakelsene først forekommer i innvilgelsesperiodene.
 * En tiltaksdeltakelse som både er innvilget med og uten reversert opphør, gir én innvilgelse for hver.
 * Tilstøtende perioder i domenet for samme tiltaksdeltakelse slås sammen, unntatt når periodene på hver side av grensen har de samme tiltaksdeltakelsene og ulikt antall dager.
 * Ved [tilKommando] deles det bare opp ved start- og sluttdatoer for periodene til tiltaksdeltakelsene, så hver grense for antall dager må også være en grense for en tiltaksdeltakelse.
 * Når tiltaksdeltakelsene er ulike på hver side, finnes det allerede en slik grense hos en annen tiltaksdeltakelse.
 * Da gir en ny runde gjennom [tilKommando] de samme periodene i domenet.
 *
 * @param tiltaksdeltakelser Tiltaksdeltakelsene fra saksopplysningene som innvilgelsesperiodene valideres mot.
 */
fun Innvilgelsesperioder.tilDTO(tiltaksdeltakelser: Tiltaksdeltakelser): InnvilgelsesperioderDTO {
    val perioderPerTiltak = linkedMapOf<Pair<TiltaksdeltakerId, Boolean>, MutableList<PeriodeMedVerdi<InnvilgelsesperiodeVerdi>>>()

    this.periodisering.perioderMedVerdi.forEach { periodeMedVerdi ->
        periodeMedVerdi.verdi.tiltaksdeltakelser.forEach {
            perioderPerTiltak
                .getOrPut(it.internDeltakelseId to periodeMedVerdi.verdi.erReversertOpphør) { mutableListOf() }
                .add(periodeMedVerdi)
        }
    }

    return InnvilgelsesperioderDTO(
        tiltak = perioderPerTiltak.map { (nøkkel, perioder) ->
            val (internDeltakelseId, erReversertOpphør) = nøkkel

            InnvilgelseForTiltakDTO(
                internDeltakelseId = internDeltakelseId.toString(),
                erReversertOpphør = erReversertOpphør,
                perioder = perioder.slåSammenForTiltak().map { periode ->
                    InnvilgelsesperiodeForTiltakDTO(
                        periode = periode.toDTO(),
                        ugyldigTiltaksdeltakelse = if (erReversertOpphør) {
                            null
                        } else {
                            tiltaksdeltakelser.validerInnvilgelsesperiode(periode, internDeltakelseId)?.tilDTO()
                        },
                    )
                },
            )
        },
        antallDagerPerMeldeperiode = this.periodisering.perioderMedVerdi
            .map { PeriodeMedVerdi(verdi = it.verdi.antallDagerPerMeldeperiode, periode = it.periode) }
            .slåSammenTilstøtendePerioder()
            .map { (antallDager, periode) ->
                AntallDagerPerMeldeperiodeDTO(
                    periode = periode.toDTO(),
                    antallDagerPerMeldeperiode = antallDager.value,
                )
            },
    )
}

/**
 * Slår sammen tilstøtende perioder for en tiltaksdeltakelse, unntatt der periodene på hver side har de samme tiltaksdeltakelsene og ulikt antall dager.
 * Se [tilDTO].
 */
private fun List<PeriodeMedVerdi<InnvilgelsesperiodeVerdi>>.slåSammenForTiltak(): List<Periode> {
    return this.sortedBy { it.periode.fraOgMed }.zipWithNext().fold(listOf(this.minBy { it.periode.fraOgMed }.periode)) { sammenslåtte, (forrige, neste) ->
        val erTilstøtende = forrige.periode.tilOgMed.plusDays(1) == neste.periode.fraOgMed
        val harSammeTiltaksdeltakelser = forrige.verdi.tiltaksdeltakelserOgReversertOpphør() == neste.verdi.tiltaksdeltakelserOgReversertOpphør()
        val skalDelesOpp = harSammeTiltaksdeltakelser && forrige.verdi.antallDagerPerMeldeperiode != neste.verdi.antallDagerPerMeldeperiode

        if (erTilstøtende && !skalDelesOpp) {
            sammenslåtte.dropLast(1) + Periode(sammenslåtte.last().fraOgMed, neste.periode.tilOgMed)
        } else {
            sammenslåtte + neste.periode
        }
    }
}

private fun InnvilgelsesperiodeVerdi.tiltaksdeltakelserOgReversertOpphør(): Pair<Set<TiltaksdeltakerId>, Boolean> {
    return tiltaksdeltakelser.map { it.internDeltakelseId }.toSet() to erReversertOpphør
}

private fun UgyldigInnvilgelsesperiode.tilDTO(): UgyldigTiltaksdeltakelseDTO {
    return when (this) {
        is UgyldigInnvilgelsesperiode.TiltaksdeltakelsenFinnesIkke -> UgyldigTiltaksdeltakelseDTO(
            type = UgyldigTiltaksdeltakelseDTO.Type.FINNES_IKKE,
            deltakelsesperiode = null,
        )

        is UgyldigInnvilgelsesperiode.TiltaksdeltakelsenKanIkkeInnvilges -> UgyldigTiltaksdeltakelseDTO(
            type = UgyldigTiltaksdeltakelseDTO.Type.KAN_IKKE_INNVILGES,
            deltakelsesperiode = null,
        )

        is UgyldigInnvilgelsesperiode.TiltaksdeltakelsenDekkerIkkePerioden -> UgyldigTiltaksdeltakelseDTO(
            type = UgyldigTiltaksdeltakelseDTO.Type.DEKKER_IKKE_PERIODEN,
            deltakelsesperiode = deltakelsesperiode.toDTO(),
        )
    }
}
