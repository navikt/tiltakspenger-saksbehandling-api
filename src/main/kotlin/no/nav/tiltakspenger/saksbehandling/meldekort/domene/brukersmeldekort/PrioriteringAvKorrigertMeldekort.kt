package no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus

/**
 * Avgjør om et nytt meldekort fra bruker skal prioriteres i benken.
 * Det gjelder korrigeringer, altså meldekort for en kjede bruker har sendt inn meldekort for tidligere, som kommer inn før oppdragssystemet har iverksatt utbetalingen av en tidligere behandling av kjeden.
 * Da kan korrigeringen fortsatt behandles før pengene er utbetalt, og en eventuell feilutbetaling hindres.
 *
 * Vurderingen gjøres ved mottak og lagres, siden vi ikke tar vare på når oppdragssystemet kvitterte for utbetalingen.
 *
 * @param meldekortId Det nye meldekortet, som ikke regnes som sin egen tidligere innsending.
 * @return null dersom meldekortet ikke er en korrigering, eller alle utbetalinger for kjeden er iverksatt.
 */
fun Sak.utledPrioriteringForBrukersMeldekort(
    meldekortId: MeldekortId,
    kjedeId: MeldeperiodeKjedeId,
): Prioritering? {
    val erKorrigering = brukersMeldekort.any { it.kjedeId == kjedeId && it.id != meldekortId }
    if (!erKorrigering) return null

    val harUtbetalingSomIkkeErIverksatt = meldekortvedtaksliste.any { vedtak ->
        kjedeId in vedtak.meldeperiodebehandlinger.kjedeIder && vedtak.utbetaling.status.erIkkeIverksatt()
    }
    if (!harUtbetalingSomIkkeErIverksatt) return null

    return Prioritering.create(
        "Korrigert meldekort må behandles omgående for å hindre feilutbetaling. " +
            "Korrigeringen kom inn før oppdragssystemet hadde iverksatt utbetalingen av forrige behandling av meldeperioden.",
    )
}

/**
 * Prioriteringen en ny manuell meldekortbehandling arver fra korrigerte meldekort fra bruker, slik at den beholder plassen øverst i benken.
 * Bare meldekort som er mottatt etter alle ikke-avbrutte behandlinger av kjeden, regnes som ubehandlet.
 *
 * @return null dersom ingen av kjedene har et prioritert meldekort fra bruker som venter på behandling.
 */
fun Sak.prioriteringFraUbehandledeBrukersMeldekort(kjedeIder: NonEmptyList<MeldeperiodeKjedeId>): Prioritering? {
    return kjedeIder.firstNotNullOfOrNull { kjedeId ->
        val behandlingerForKjede = meldekortbehandlinger.hentIkkeAvbrutteBehandlingerForKjede(kjedeId)
        brukersMeldekort
            .filter { meldekort -> meldekort.kjedeId == kjedeId && behandlingerForKjede.all { it.opprettet < meldekort.mottatt } }
            .maxByOrNull { it.mottatt }
            ?.prioritering
    }
}

/** Utbetalingen er ikke kvittert ut av oppdragssystemet ennå, og kan derfor fortsatt føre til en feilutbetaling. */
private fun Utbetalingsstatus?.erIkkeIverksatt(): Boolean = when (this) {
    null,
    Utbetalingsstatus.IkkePåbegynt,
    Utbetalingsstatus.SendtTilOppdrag,
    Utbetalingsstatus.FeiletMotOppdrag,
    -> true

    Utbetalingsstatus.Ok,
    Utbetalingsstatus.OkUtenUtbetaling,
    Utbetalingsstatus.Avbrutt,
    -> false
}
