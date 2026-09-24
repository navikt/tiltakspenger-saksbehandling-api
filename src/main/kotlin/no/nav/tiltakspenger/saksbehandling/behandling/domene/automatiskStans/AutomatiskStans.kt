package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndringer
import java.time.Clock
import java.time.LocalDate

/**
 * Det systemet har utledet for å kunne fylle ut en automatisk opprettet stans-revurdering uten saksbehandler.
 */
data class AutomatiskStans(
    val hjemmel: HjemmelForStans,
    val stansFraOgMed: ValgtStansFraOgMed,
    val begrunnelse: Begrunnelse,
)

sealed interface KanIkkeStanseAutomatisk : Loggbar {
    data object ErIkkeAutomatiskOpprettetStans : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("revurderingen er ikke en automatisk opprettet stans")
    }

    data class BehandlingenErIkkeKlarTilAutomatiskBehandling(val status: Rammebehandlingsstatus) : KanIkkeStanseAutomatisk {
        override val loggkontekst get() = Loggkontekst("revurderingen har status $status, er tildelt en saksbehandler, står på vent eller er knyttet til en klage")
    }

    data object KanIkkeUtledeHjemmel : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("endringene gir ikke grunnlag for en hjemmel vi kan stanse automatisk på")
    }

    data object HarAndreÅpneBehandlinger : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("saken har andre åpne behandlinger")
    }

    data object SakenHarIkkeRett : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("saken har ingen dager som gir rett")
    }

    data object FantIkkeTiltaksdeltakelse : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("fant ikke tiltaksdeltakelsen i saksopplysningene til revurderingen")
    }

    data class DeltakelsenErIkkeAvsluttet(val status: TiltakDeltakerstatus) : KanIkkeStanseAutomatisk {
        override val loggkontekst get() = Loggkontekst("deltakelsen har status $status, som ikke er en avsluttet status")
    }

    data object DeltakelsenManglerSluttdato : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("deltakelsen mangler sluttdato")
    }

    data class SluttdatoErIFremtiden(val sluttdato: LocalDate) : KanIkkeStanseAutomatisk {
        override val loggkontekst get() = Loggkontekst("sluttdato $sluttdato for deltakelsen er i fremtiden")
    }

    data class IngenRettEtterSluttdato(val sluttdato: LocalDate) : KanIkkeStanseAutomatisk {
        override val loggkontekst get() = Loggkontekst("saken har ingen dager med rett etter sluttdato $sluttdato")
    }

    data object AndreTiltaksdeltakelserGirRett : KanIkkeStanseAutomatisk {
        override val loggkontekst = Loggkontekst("andre tiltaksdeltakelser gir rett i stansperioden")
    }

    /**
     * Vurderingen gikk gjennom, men et av stegene for å ta, fylle ut eller sende revurderingen til beslutning feilet.
     * [årsak] skal være PII-fri, f.eks. klassenavnet til den underliggende feilen.
     */
    data class FeilVedAutomatiskBehandling(val steg: String, val årsak: String) : KanIkkeStanseAutomatisk {
        override val loggkontekst get() = Loggkontekst("feil ved $steg: $årsak")
    }
}

private val avsluttedeDeltakerstatuser = setOf(
    TiltakDeltakerstatus.Avbrutt,
    TiltakDeltakerstatus.HarSluttet,
    TiltakDeltakerstatus.Fullført,
)

/**
 * Vurderer om systemet har nok informasjon til å fylle ut en automatisk opprettet stans-revurdering selv.
 *
 * Foreløpig støttes kun [HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak], utledet fra at deltakelsen er avbrutt.
 * De andre hjemlene for stans får vi ikke nok informasjon om til å kunne automatisere.
 *
 * Stansen gjelder fra dagen etter deltakelsens sluttdato, eller fra første dag som gir rett dersom sluttdatoen er før det.
 * Vurderingen er bevisst konservativ; alt som ikke er entydig overlates til saksbehandler.
 * Sluttdato og status hentes fra saksopplysningene på revurderingen, slik at det er de samme opplysningene beslutter ser.
 */
fun Sak.utledAutomatiskStans(
    revurdering: Revurdering,
    tiltaksdeltakerId: TiltaksdeltakerId,
    clock: Clock,
): Either<KanIkkeStanseAutomatisk, AutomatiskStans> {
    val automatiskOpprettetGrunn = revurdering.automatiskOpprettetGrunn
    if (revurdering.resultat !is Revurderingsresultat.Stans || automatiskOpprettetGrunn == null) {
        return KanIkkeStanseAutomatisk.ErIkkeAutomatiskOpprettetStans.left()
    }

    if (revurdering.status != Rammebehandlingsstatus.KLAR_TIL_BEHANDLING ||
        revurdering.saksbehandler != null ||
        revurdering.ventestatus.erSattPåVent ||
        revurdering.klagebehandling != null
    ) {
        return KanIkkeStanseAutomatisk.BehandlingenErIkkeKlarTilAutomatiskBehandling(revurdering.status).left()
    }

    val hjemmel = automatiskOpprettetGrunn.endringer.utledHjemmelForAutomatiskStans()
        ?: return KanIkkeStanseAutomatisk.KanIkkeUtledeHjemmel.left()

    if (rammebehandlinger.åpneBehandlinger.any { it.id != revurdering.id }) {
        return KanIkkeStanseAutomatisk.HarAndreÅpneBehandlinger.left()
    }

    val førsteDagSomGirRett = this.førsteDagSomGirRett
    val sisteDagSomGirRett = this.sisteDagSomGirRett
    if (førsteDagSomGirRett == null || sisteDagSomGirRett == null) {
        return KanIkkeStanseAutomatisk.SakenHarIkkeRett.left()
    }

    val deltakelse = revurdering.getTiltaksdeltakelse(tiltaksdeltakerId)
        ?: return KanIkkeStanseAutomatisk.FantIkkeTiltaksdeltakelse.left()

    if (deltakelse.deltakelseStatus !in avsluttedeDeltakerstatuser) {
        return KanIkkeStanseAutomatisk.DeltakelsenErIkkeAvsluttet(deltakelse.deltakelseStatus).left()
    }

    val sluttdato = deltakelse.deltakelseTilOgMed
        ?: return KanIkkeStanseAutomatisk.DeltakelsenManglerSluttdato.left()

    // En sluttdato i fremtiden kan fortsatt endres, og da er det ikke entydig hvilken dag det skal stanses fra.
    if (sluttdato.isAfter(LocalDate.now(clock))) {
        return KanIkkeStanseAutomatisk.SluttdatoErIFremtiden(sluttdato).left()
    }

    val førsteDagUtenDeltakelse = sluttdato.plusDays(1)
    if (førsteDagUtenDeltakelse.isAfter(sisteDagSomGirRett)) {
        return KanIkkeStanseAutomatisk.IngenRettEtterSluttdato(sluttdato).left()
    }

    val stansFraOgMed = maxOf(førsteDagUtenDeltakelse, førsteDagSomGirRett)

    val andreDeltakelserGirRett = rammevedtaksliste.valgteTiltaksdeltakelser.filter {
        it.periode.tilOgMed >= stansFraOgMed && it.verdi.internDeltakelseId != tiltaksdeltakerId
    }.verdier.isNotEmpty()
    if (andreDeltakelserGirRett) {
        return KanIkkeStanseAutomatisk.AndreTiltaksdeltakelserGirRett.left()
    }

    return AutomatiskStans(
        hjemmel = hjemmel,
        stansFraOgMed = if (stansFraOgMed == førsteDagSomGirRett) {
            ValgtStansFraOgMed.StansFraFørsteDagSomGirRett
        } else {
            ValgtStansFraOgMed.StansFraOgMed(stansFraOgMed)
        },
        begrunnelse = Begrunnelse.create(
            "Automatisk behandlet. Tiltaksdeltakelsen er registrert med status ${deltakelse.deltakelseStatus} " +
                "og sluttdato ${sluttdato.format(norskDatoFormatter)}. " +
                "Tiltakspengene stanses fra og med ${stansFraOgMed.format(norskDatoFormatter)} fordi bruker ikke lenger deltar på arbeidsmarkedstiltak.",
        )!!,
    ).right()
}

private fun TiltaksdeltakerEndringer.utledHjemmelForAutomatiskStans(): HjemmelForStans? {
    return when {
        avbrutt != null -> HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak
        else -> null
    }
}
