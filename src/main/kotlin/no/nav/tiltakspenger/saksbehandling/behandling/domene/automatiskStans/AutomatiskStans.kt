package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet stans-revurdering uten saksbehandler.
 */
data class AutomatiskStans(
    val hjemmel: HjemmelForStans,
    val stansFraOgMed: ValgtStansFraOgMed,
    val begrunnelse: Begrunnelse,
)

private val avsluttedeDeltakerstatuser = setOf(
    TiltakDeltakerstatus.Avbrutt,
    TiltakDeltakerstatus.HarSluttet,
    TiltakDeltakerstatus.Fullført,
)

/**
 * Utleder verdiene for å fylle ut en stans av [tiltaksdeltakerId] uten saksbehandler.
 * Gir grunnen til at stansen må behandles manuelt dersom verdiene ikke kan utledes med stor sikkerhet.
 *
 * Foreløpig støttes kun [HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak], utledet fra at deltakelsen er avsluttet.
 * De andre hjemlene for stans får vi ikke nok informasjon om til å kunne automatisere.
 *
 * Deltakelsen må ha en avsluttet status og en sluttdato som er passert, siden en sluttdato i fremtiden fortsatt kan endres.
 * Stansen gjelder fra første innvilgede dag for deltakelsen etter sluttdatoen, slik at den ikke starter i en periode som allerede er stanset eller opphørt.
 * Er det ingen innvilgede dager etter sluttdatoen, er det ingenting å stanse.
 * Stansen gjelder tidligst fra dagen etter siste dag med utbetaling på saken, siden stans ikke støtter feilutbetaling.
 * Er alle innvilgede dager etter sluttdatoen allerede utbetalt, overlates stansen til saksbehandler.
 * Er andre deltakelser innvilget fra stansdatoen, vil stansen berøre mer enn denne deltakelsen.
 *
 * @param nåtilstand Deltakelsen slik den er registrert nå, typisk fra saksopplysningene til revurderingen.
 */
fun Sak.utledAutomatiskStans(
    tiltaksdeltakerId: TiltaksdeltakerId,
    nåtilstand: TiltaksdeltakelseIntern?,
    iDag: LocalDate,
): Either<ManueltBehandlesGrunn, AutomatiskStans> {
    if (nåtilstand == null) {
        return ManueltBehandlesGrunn.STANS_FANT_IKKE_TILTAKSDELTAKELSE.left()
    }
    val status = nåtilstand.deltakelseStatus
    if (status !in avsluttedeDeltakerstatuser) {
        return ManueltBehandlesGrunn.STANS_DELTAKELSEN_ER_IKKE_AVSLUTTET.left()
    }
    val sluttdato = nåtilstand.deltakelseTilOgMed
        ?: return ManueltBehandlesGrunn.STANS_DELTAKELSEN_MANGLER_SLUTTDATO.left()
    if (sluttdato.isAfter(iDag)) {
        return ManueltBehandlesGrunn.STANS_SLUTTDATO_ER_IKKE_PASSERT.left()
    }

    val innvilgedePerioder = rammevedtaksliste.valgteTiltaksdeltakelser.perioderMedVerdi
    val innvilgedePerioderForDeltakelsen = innvilgedePerioder.filter { it.verdi.internDeltakelseId == tiltaksdeltakerId }
    val dagenEtterSluttdato = sluttdato.plusDays(1)
    if (innvilgedePerioderForDeltakelsen.all { it.periode.tilOgMed.isBefore(dagenEtterSluttdato) }) {
        return ManueltBehandlesGrunn.STANS_INGEN_INNVILGEDE_DAGER_ETTER_SLUTTDATO.left()
    }

    // TODO: Kanskje utlede denne fra simuleringen isteden, slik at utbetalinger som ikke er fullført i OS/UR fortsatt kan stanses.
    // Det blir en "hastesak" å få besluttet den isåfall... :thinking:
    val utbetaltEtterSluttdatoTilOgMed = meldeperiodeBeregninger.sisteDagMedUtbetaling?.takeIf { it.isAfter(sluttdato) }
    val tidligsteStansdato = utbetaltEtterSluttdatoTilOgMed?.plusDays(1) ?: dagenEtterSluttdato
    val stansFraOgMed = innvilgedePerioderForDeltakelsen
        .firstOrNull { !it.periode.tilOgMed.isBefore(tidligsteStansdato) }
        ?.let { maxOf(it.periode.fraOgMed, tidligsteStansdato) }
        ?: return ManueltBehandlesGrunn.STANS_INGEN_INNVILGEDE_DAGER_ETTER_SISTE_UTBETALING.left()

    val harAndreDeltakelserInnvilgetEtterStans = innvilgedePerioder.any {
        !it.periode.tilOgMed.isBefore(stansFraOgMed) && it.verdi.internDeltakelseId != tiltaksdeltakerId
    }
    if (harAndreDeltakelserInnvilgetEtterStans) {
        return ManueltBehandlesGrunn.STANS_ANDRE_DELTAKELSER_INNVILGET_ETTER_SLUTTDATO.left()
    }

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
                    append("Det er allerede utbetalt tiltakspenger til og med ${utbetaltEtterSluttdatoTilOgMed.format(norskDatoFormatter)}, så stansen gjelder tidligst fra dagen etter. ")
                }
                append("Tiltakspengene stanses fra og med ${stansFraOgMed.format(norskDatoFormatter)} fordi bruker ikke lenger deltar på arbeidsmarkedstiltak.")
            },
        )!!,
    ).right()
}
