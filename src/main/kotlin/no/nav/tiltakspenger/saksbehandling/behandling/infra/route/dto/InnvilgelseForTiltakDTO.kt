package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.inneholderOverlapp
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.slåSammenTilstøtendePerioder
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgetTiltaksdeltakelseKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.UgyldigInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.delOppEtterOverlapp
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * Innvilgelsen for én tiltaksdeltakelse, med periodene og antall dager per meldeperiode som innvilges for den.
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
 * @param antallDagerPerMeldeperiode Antall dager som innvilges for denne tiltaksdeltakelsen i perioden.
 * @param ugyldigTiltaksdeltakelse Settes bare i responser, og er null dersom tiltaksdeltakelsen passer med perioden.
 * Ignoreres i requester.
 * Er alltid null for reversert opphør, siden perioden da ikke valideres mot tiltaksdeltakelsene i saksopplysningene.
 */
data class InnvilgelsesperiodeForTiltakDTO(
    val periode: PeriodeDTO,
    val antallDagerPerMeldeperiode: Int,
    val ugyldigTiltaksdeltakelse: UgyldigTiltaksdeltakelseDTO? = null,
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

typealias InnvilgelsesperioderDTO = List<InnvilgelseForTiltakDTO>

private data class InnvilgelsesperiodeForTiltak(
    val tiltaksdeltakelse: InnvilgetTiltaksdeltakelseKommando,
    val erReversertOpphør: Boolean,
    val periode: Periode,
)

private data class Delperiodeverdi(
    val tiltaksdeltakelser: NonEmptyList<InnvilgetTiltaksdeltakelseKommando>,
    val erReversertOpphør: Boolean,
)

/**
 * Deler opp periodene for tiltaksdeltakelsene i delperioder der de samme tiltaksdeltakelsene overlapper.
 * Tilstøtende delperioder med like tiltaksdeltakelser slås sammen, og delperioder uten tiltaksdeltakelser blir hull.
 * Kaster dersom periodene for samme tiltaksdeltakelse overlapper, dersom perioder med og uten reversert opphør overlapper, eller dersom det ikke finnes noen perioder.
 */
fun InnvilgelsesperioderDTO.tilKommando(): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> {
    val perioderForTiltak = this.flatMap { tiltak ->
        val internDeltakelseId = TiltaksdeltakerId.fromString(tiltak.internDeltakelseId)
        tiltak.perioder.map {
            InnvilgelsesperiodeForTiltak(
                tiltaksdeltakelse = InnvilgetTiltaksdeltakelseKommando(
                    internDeltakelseId = internDeltakelseId,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                ),
                erReversertOpphør = tiltak.erReversertOpphør,
                periode = it.periode.toDomain(),
            )
        }
    }

    require(perioderForTiltak.isNotEmpty()) { "Må ha minst én innvilgelsesperiode" }

    perioderForTiltak.groupBy { it.tiltaksdeltakelse.internDeltakelseId }.forEach { (internDeltakelseId, perioder) ->
        require(!perioder.map { it.periode }.inneholderOverlapp()) {
            "Periodene for tiltaksdeltakelsen $internDeltakelseId kan ikke overlappe"
        }
    }

    val (reversertOpphør, ikkeReversertOpphør) = perioderForTiltak.partition { it.erReversertOpphør }
    require(reversertOpphør.none { it.periode.overlapperMed(ikkeReversertOpphør.map { annen -> annen.periode }) }) {
        "Perioder som er reversert opphør kan ikke overlappe perioder som ikke er reversert opphør"
    }

    val totalPeriode = Periode(
        fraOgMed = perioderForTiltak.minOf { it.periode.fraOgMed },
        tilOgMed = perioderForTiltak.maxOf { it.periode.tilOgMed },
    )

    return totalPeriode
        .delOppEtterOverlapp(perioderForTiltak.map { it to it.periode })
        .map { (perioder, delperiode) ->
            PeriodeMedVerdi(
                periode = delperiode,
                verdi = Delperiodeverdi(
                    tiltaksdeltakelser = perioder.map { it.tiltaksdeltakelse },
                    erReversertOpphør = perioder.head.erReversertOpphør,
                ),
            )
        }
        .tilIkkeTomPeriodisering()
        .map { verdi, periode ->
            InnvilgelsesperiodeKommando(
                periode = periode,
                tiltaksdeltakelser = verdi.tiltaksdeltakelser,
                erReversertOpphør = verdi.erReversertOpphør,
            )
        }
}

/**
 * Gir én innvilgelse per tiltaksdeltakelse, i rekkefølgen tiltaksdeltakelsene først forekommer i innvilgelsesperiodene.
 * En tiltaksdeltakelse som både er innvilget med og uten reversert opphør, gir én innvilgelse for hver.
 * Tilstøtende perioder med likt antall dager for tiltaksdeltakelsen slås sammen.
 * @param tiltaksdeltakelser Tiltaksdeltakelsene fra saksopplysningene som innvilgelsesperiodene valideres mot.
 */
fun Innvilgelsesperioder.tilDTO(tiltaksdeltakelser: Tiltaksdeltakelser): InnvilgelsesperioderDTO {
    val perioderPerTiltak = linkedMapOf<Pair<TiltaksdeltakerId, Boolean>, MutableList<PeriodeMedVerdi<AntallDagerForMeldeperiode>>>()

    this.periodisering.perioderMedVerdi.forEach { (verdi, periode) ->
        verdi.tiltaksdeltakelser.forEach {
            perioderPerTiltak
                .getOrPut(it.internDeltakelseId to verdi.erReversertOpphør) { mutableListOf() }
                .add(PeriodeMedVerdi(periode = periode, verdi = it.antallDagerPerMeldeperiode))
        }
    }

    return perioderPerTiltak.map { (nøkkel, perioder) ->
        val (internDeltakelseId, erReversertOpphør) = nøkkel

        InnvilgelseForTiltakDTO(
            internDeltakelseId = internDeltakelseId.toString(),
            erReversertOpphør = erReversertOpphør,
            perioder = perioder.slåSammenTilstøtendePerioder().map { (antallDager, periode) ->
                InnvilgelsesperiodeForTiltakDTO(
                    periode = periode.toDTO(),
                    antallDagerPerMeldeperiode = antallDager.value,
                    ugyldigTiltaksdeltakelse = if (erReversertOpphør) {
                        null
                    } else {
                        tiltaksdeltakelser.validerInnvilgelsesperiode(periode, internDeltakelseId)?.tilDTO()
                    },
                )
            },
        )
    }
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
