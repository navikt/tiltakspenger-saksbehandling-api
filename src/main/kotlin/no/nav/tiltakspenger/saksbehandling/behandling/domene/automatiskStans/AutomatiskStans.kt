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
    val dagenEtterSluttdato = sluttdato.plusDays(1)
    val stansFraOgMed = innvilgedePerioder
        .filter { it.verdi.internDeltakelseId == tiltaksdeltakerId }
        .firstOrNull { !it.periode.tilOgMed.isBefore(dagenEtterSluttdato) }
        ?.let { maxOf(it.periode.fraOgMed, dagenEtterSluttdato) }
        ?: return ManueltBehandlesGrunn.STANS_INGEN_INNVILGEDE_DAGER_ETTER_SLUTTDATO.left()

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
            "Automatisk behandlet. Tiltaksdeltakelsen er registrert med status $status " +
                "og sluttdato ${sluttdato.format(norskDatoFormatter)}. " +
                "Tiltakspengene stanses fra og med ${stansFraOgMed.format(norskDatoFormatter)} fordi bruker ikke lenger deltar på arbeidsmarkedstiltak.",
        )!!,
    ).right()
}
