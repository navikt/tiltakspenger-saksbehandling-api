package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet stans-revurdering uten saksbehandler.
 *
 * @param prioritering Satt når stansen gjelder dager det er sendt utbetaling for, men som oppdragssystemet ikke har utbetalt ennå.
 * Da må stansen besluttes før oppdragssystemet utbetaler, ellers blir det feilutbetaling.
 */
data class AutomatiskStans(
    val hjemmel: HjemmelForStans,
    val stansFraOgMed: ValgtStansFraOgMed,
    val begrunnelse: Begrunnelse,
    val prioritering: Prioritering?,
)

private val avsluttedeDeltakerstatuser = setOf(
    TiltakDeltakerstatus.Avbrutt,
    TiltakDeltakerstatus.HarSluttet,
    TiltakDeltakerstatus.Fullført,
)

/**
 * Utleder verdiene for å fylle ut en stans av [tiltaksdeltakerId] uten saksbehandler.
 * Gir alle grunnene til at stansen må behandles manuelt dersom verdiene ikke kan utledes med stor sikkerhet.
 * Sjekker som bygger på en verdi som ikke kunne utledes, hoppes over, slik at saksbehandler ikke får følgefeil.
 *
 * Foreløpig støttes kun [HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak], utledet fra at deltakelsen er avsluttet.
 * De andre hjemlene for stans får vi ikke nok informasjon om til å kunne automatisere.
 *
 * Deltakelsen må ha en avsluttet status og en sluttdato som er passert, siden en sluttdato i fremtiden fortsatt kan endres.
 * Stansen gjelder fra første innvilgede dag for deltakelsen etter sluttdatoen, slik at den ikke starter i en periode som allerede er stanset eller opphørt.
 * Er det ingen innvilgede dager etter sluttdatoen, er det ingenting å stanse.
 *
 * Stans støtter ikke feilutbetaling, men en utbetaling vi har sendt kan fortsatt stanses fram til oppdragssystemet har utbetalt den.
 * Derfor simuleres stansen først fra sluttdatoen, og utledes på nytt med [sisteDagMedFeilutbetaling] bare dersom simuleringen viser feilutbetaling.
 * Stansen gjelder da tidligst fra dagen etter siste dag med feilutbetaling.
 * Er alle innvilgede dager etter sluttdatoen allerede utbetalt, overlates stansen til saksbehandler.
 * Er andre deltakelser innvilget fra stansdatoen, vil stansen berøre mer enn denne deltakelsen.
 * Er noe innvilget som reversert opphør fra stansdatoen, overlates stansen til saksbehandler, siden det er innvilget uten gyldig tiltaksdeltakelse.
 *
 * Gjelder stansen dager vi har sendt utbetaling for, er utbetalingen ikke utbetalt av oppdragssystemet ennå, og stansen prioriteres.
 *
 * @param nåtilstand Deltakelsen slik den er registrert nå, typisk fra saksopplysningene til revurderingen.
 * @param sisteDagMedFeilutbetaling Siste dag simuleringen av stansen viser feilutbetaling for, eller null før stansen er simulert.
 */
fun Sak.utledAutomatiskStans(
    tiltaksdeltakerId: TiltaksdeltakerId,
    nåtilstand: TiltaksdeltakelseIntern?,
    iDag: LocalDate,
    sisteDagMedFeilutbetaling: LocalDate? = null,
): Either<NonEmptyList<ManueltBehandlesGrunn>, AutomatiskStans> {
    if (nåtilstand == null) {
        return nonEmptyListOf(ManueltBehandlesGrunn.STANS_FANT_IKKE_TILTAKSDELTAKELSE).left()
    }
    val grunner = mutableListOf<ManueltBehandlesGrunn>()

    val status = nåtilstand.deltakelseStatus
    if (status !in avsluttedeDeltakerstatuser) {
        grunner.add(ManueltBehandlesGrunn.STANS_DELTAKELSEN_ER_IKKE_AVSLUTTET)
    }
    val sluttdato = nåtilstand.periode.tilOgMed
        ?: return (grunner + ManueltBehandlesGrunn.STANS_DELTAKELSEN_MANGLER_SLUTTDATO).toNonEmptyListOrThrow().left()
    // Stansdatoen utledes fra sluttdatoen, og en sluttdato i fremtiden kan fortsatt endres.
    if (sluttdato.isAfter(iDag)) {
        return (grunner + ManueltBehandlesGrunn.STANS_SLUTTDATO_ER_IKKE_PASSERT).toNonEmptyListOrThrow().left()
    }

    val dagenEtterSluttdato = sluttdato.plusDays(1)
    if (førsteInnvilgedeDagForDeltakelsen(tiltaksdeltakerId, fraOgMed = dagenEtterSluttdato) == null) {
        return (grunner + ManueltBehandlesGrunn.STANS_INGEN_INNVILGEDE_DAGER_ETTER_SLUTTDATO).toNonEmptyListOrThrow()
            .left()
    }

    val utbetaltEtterSluttdatoTilOgMed = sisteDagMedFeilutbetaling?.takeIf { it.isAfter(sluttdato) }
    val tidligsteStansdato = utbetaltEtterSluttdatoTilOgMed?.plusDays(1) ?: dagenEtterSluttdato
    val stansFraOgMed = førsteInnvilgedeDagForDeltakelsen(tiltaksdeltakerId, fraOgMed = tidligsteStansdato)
        ?: return (grunner + ManueltBehandlesGrunn.STANS_INGEN_INNVILGEDE_DAGER_ETTER_SISTE_UTBETALING).toNonEmptyListOrThrow()
            .left()

    val harAndreDeltakelserInnvilgetEtterStans = rammevedtaksliste.valgteTiltaksdeltakelser.perioderMedVerdi.any {
        !it.periode.tilOgMed.isBefore(stansFraOgMed) && it.verdi.any { tiltaksdeltakelse -> tiltaksdeltakelse.internDeltakelseId != tiltaksdeltakerId }
    }
    if (harAndreDeltakelserInnvilgetEtterStans) {
        grunner.add(ManueltBehandlesGrunn.STANS_ANDRE_DELTAKELSER_INNVILGET_ETTER_SLUTTDATO)
    }

    val harReversertOpphørEtterStans = rammevedtaksliste.innvilgelsesperioder.perioderMedVerdi.any {
        !it.periode.tilOgMed.isBefore(stansFraOgMed) && it.verdi.erReversertOpphør
    }
    if (harReversertOpphørEtterStans) {
        grunner.add(ManueltBehandlesGrunn.STANS_REVERSERT_OPPHOR_ETTER_SLUTTDATO)
    }

    grunner.toNonEmptyListOrNull()?.also { return it.left() }

    val sendtUtbetalingTilOgMed = meldeperiodeBeregninger.sisteDagMedUtbetaling?.takeIf { !it.isBefore(stansFraOgMed) }

    return AutomatiskStans(
        hjemmel = HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak,
        stansFraOgMed = if (stansFraOgMed == førsteDagSomGirRett) {
            ValgtStansFraOgMed.StansFraFørsteDagSomGirRett
        } else {
            ValgtStansFraOgMed.StansFraOgMed(stansFraOgMed)
        },
        begrunnelse = Begrunnelse.create(
            buildString {
                append("Automatisk behandlet. Tiltaksdeltakelsen er registrert med status $status ")
                append("og sluttdato ${sluttdato.format(norskDatoFormatter)}. ")
                if (utbetaltEtterSluttdatoTilOgMed != null) {
                    append(
                        "Oppdragssystemet har allerede utbetalt tiltakspenger til og med ${
                            utbetaltEtterSluttdatoTilOgMed.format(
                                norskDatoFormatter,
                            )
                        }, så stansen gjelder tidligst fra dagen etter. ",
                    )
                }
                append("Tiltakspengene stanses fra og med ${stansFraOgMed.format(norskDatoFormatter)} fordi bruker ikke lenger deltar på arbeidsmarkedstiltak.")
            },
        )!!,
        prioritering = sendtUtbetalingTilOgMed?.let {
            Prioritering.create(
                "Stans må behandles omgående for å hindre feilutbetaling. Det er sendt utbetaling av tiltakspenger til og med ${
                    it.format(
                        norskDatoFormatter,
                    )
                } som oppdragssystemet ikke har utbetalt ennå.",
            )
        },
    ).right()
}

/**
 * Dagen en stans fra den passerte sluttdatoen til deltakelsen ville gjeldt fra, uten sjekkene som avgjør om stansen kan behandles automatisk.
 * Det er første innvilgede dag for deltakelsen etter sluttdatoen, som i [utledAutomatiskStans] før stansen er simulert.
 * Brukes til å vurdere opphør i stedet for stans også når stansen må behandles manuelt, siden heller ikke en manuell stans kan gi feilutbetaling.
 *
 * @return null dersom deltakelsen mangler, mangler sluttdato, sluttdatoen ikke er passert, eller ingen dager er innvilget for deltakelsen etter sluttdatoen.
 */
fun Sak.utledStansFraOgMedEtterSluttdato(
    tiltaksdeltakerId: TiltaksdeltakerId,
    nåtilstand: TiltaksdeltakelseIntern?,
    iDag: LocalDate,
): LocalDate? {
    val sluttdato = nåtilstand?.periode?.tilOgMed?.takeIf { !it.isAfter(iDag) } ?: return null
    return førsteInnvilgedeDagForDeltakelsen(tiltaksdeltakerId, fraOgMed = sluttdato.plusDays(1))
}

/** Første dag fra og med [fraOgMed] som deltakelsen er innvilget i et gjeldende vedtak, eller null dersom ingen dager er innvilget fra da. */
private fun Sak.førsteInnvilgedeDagForDeltakelsen(tiltaksdeltakerId: TiltaksdeltakerId, fraOgMed: LocalDate): LocalDate? =
    rammevedtaksliste.valgteTiltaksdeltakelser.perioderMedVerdi
        .firstOrNull { it.verdi.any { tiltaksdeltakelse -> tiltaksdeltakelse.internDeltakelseId == tiltaksdeltakerId } && !it.periode.tilOgMed.isBefore(fraOgMed) }
        ?.let { maxOf(it.periode.fraOgMed, fraOgMed) }
