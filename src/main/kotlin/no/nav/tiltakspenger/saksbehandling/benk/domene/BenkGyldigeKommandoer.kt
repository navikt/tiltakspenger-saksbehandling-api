package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingStatus.OPPRETTET
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingStatus.TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingStatus.TIL_FORHÅNDSVARSEL
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingStatus.TIL_GODKJENNING
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingStatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingStatus.UNDER_FORHÅNDSVARSLING
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingStatus.UNDER_GODKJENNING
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.AngreSendTilBeslutning
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.Avbryt
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.Gjenoppta
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.LeggTilbakeBeslutter
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.LeggTilbakeSaksbehandler
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.OvertaBeslutter
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.OvertaSaksbehandler
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.SettPåVent
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.TildelBeslutter
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando.TildelSaksbehandler

/**
 * Kommandoene en rad i benken tilbyr den innloggede saksbehandleren.
 *
 * Reglene speiler `Rammebehandling.finnGyldigeKommandoer`, `Meldekortbehandling.finnGyldigeKommandoer` og `TilbakekrevingBehandling.gyldigeKommandoer`, men regnes ut fra radens felter — benken laster ikke de fulle behandlingene.
 * Speilingen er pinnet mot de ekte reglene i `BenkAggregatTest`; endres reglene der, skal testene si ifra.
 *
 * Rader uten behandling — innsendte eller korrigerte meldekort som venter på at noen starter en behandling — har ingen kommandoer, fordi kommandoene alle er handlinger på en behandling.
 */

fun BenkSøknadsbehandling.finnGyldigeKommandoer(saksbehandler: Saksbehandler): List<SaksbehandlerBehandlingKommando> =
    finnGyldigeRammebehandlingKommandoer(felles, status, saksbehandler)

fun BenkRevurdering.finnGyldigeKommandoer(saksbehandler: Saksbehandler): List<SaksbehandlerBehandlingKommando> =
    finnGyldigeRammebehandlingKommandoer(felles, status, saksbehandler)

fun BenkMeldekort.finnGyldigeKommandoer(saksbehandler: Saksbehandler): List<SaksbehandlerBehandlingKommando> =
    if (type == BenkMeldekortType.MELDEKORTBEHANDLING) {
        finnGyldigeMeldekortKommandoer(felles, status, saksbehandler)
    } else {
        emptyList()
    }

fun BenkTilbakekreving.finnGyldigeKommandoer(saksbehandler: Saksbehandler): List<SaksbehandlerBehandlingKommando> {
    val navIdent = saksbehandler.navIdent
    return buildList {
        when (status) {
            TIL_FORHÅNDSVARSEL, TIL_BEHANDLING ->
                if (saksbehandler.erSaksbehandler()) add(TildelSaksbehandler)

            UNDER_FORHÅNDSVARSLING, UNDER_BEHANDLING ->
                if (felles.saksbehandler == navIdent) {
                    add(LeggTilbakeSaksbehandler)
                } else if (saksbehandler.erSaksbehandler()) {
                    add(OvertaSaksbehandler)
                }

            TIL_GODKJENNING ->
                if (saksbehandler.erBeslutter() && navIdent != felles.saksbehandler) add(TildelBeslutter)

            UNDER_GODKJENNING ->
                if (felles.beslutter == navIdent) {
                    add(LeggTilbakeBeslutter)
                } else if (saksbehandler.erBeslutter() && navIdent != felles.saksbehandler) {
                    add(OvertaBeslutter)
                }

            OPPRETTET -> {}
        }
    }
}

/**
 * Speiler `Rammebehandling.finnGyldigeKommandoer` og `kan*`-funksjonene den bygger på.
 */
private fun finnGyldigeRammebehandlingKommandoer(
    felles: BenkBehandlingsfelles,
    status: BenkBehandlingsstatus,
    saksbehandler: Saksbehandler,
): List<SaksbehandlerBehandlingKommando> {
    val navIdent = saksbehandler.navIdent
    val erSaksbehandler = saksbehandler.erSaksbehandler()
    val erBeslutter = saksbehandler.erBeslutter()
    val erSattPåVent = felles.ventestatus.erSattPåVent

    return buildList {
        if (status == KLAR_TIL_BEHANDLING && erSaksbehandler && felles.saksbehandler == null) {
            add(TildelSaksbehandler)
        }
        if (status == KLAR_TIL_BESLUTNING && felles.saksbehandler == navIdent) {
            add(AngreSendTilBeslutning)
        }
        if (status == KLAR_TIL_BESLUTNING && erBeslutter && felles.beslutter == null && felles.saksbehandler != navIdent) {
            add(TildelBeslutter)
        }
        if (status == BenkBehandlingsstatus.UNDER_BEHANDLING && felles.saksbehandler != navIdent && erSaksbehandler && felles.saksbehandler != null) {
            add(OvertaSaksbehandler)
        }
        if (status == UNDER_BESLUTNING && felles.beslutter != navIdent && erBeslutter && felles.beslutter != null && felles.saksbehandler != navIdent) {
            add(OvertaBeslutter)
        }
        if (status == BenkBehandlingsstatus.UNDER_BEHANDLING && erSaksbehandler && felles.saksbehandler == navIdent) {
            add(LeggTilbakeSaksbehandler)
        }
        if (status == UNDER_BESLUTNING && erBeslutter && felles.beslutter == navIdent) {
            add(LeggTilbakeBeslutter)
        }
        if (!erSattPåVent && when (status) {
                UNDER_AUTOMATISK_BEHANDLING -> true
                BenkBehandlingsstatus.UNDER_BEHANDLING -> erSaksbehandler && felles.saksbehandler == navIdent
                UNDER_BESLUTNING -> erBeslutter && felles.beslutter == navIdent
                else -> false
            }
        ) {
            add(SettPåVent)
        }
        if (erSattPåVent && when (status) {
                KLAR_TIL_BEHANDLING, BenkBehandlingsstatus.UNDER_BEHANDLING, UNDER_AUTOMATISK_BEHANDLING -> erSaksbehandler
                KLAR_TIL_BESLUTNING, UNDER_BESLUTNING -> erBeslutter
                else -> false
            }
        ) {
            add(Gjenoppta)
        }
        if (when (status) {
                UNDER_AUTOMATISK_BEHANDLING, KLAR_TIL_BEHANDLING, BenkBehandlingsstatus.UNDER_BEHANDLING -> true
                KLAR_TIL_BESLUTNING -> felles.saksbehandler == navIdent
                UNDER_BESLUTNING -> felles.beslutter == navIdent
                else -> false
            }
        ) {
            add(Avbryt)
        }
    }
}

/**
 * Speiler `Meldekortbehandling.finnGyldigeKommandoer` og `kan*`-funksjonene den bygger på.
 * Reglene er ikke de samme som for rammebehandlinger — en meldekortbehandling som er klar til behandling kan for eksempel ikke avbrytes.
 */
private fun finnGyldigeMeldekortKommandoer(
    felles: BenkBehandlingsfelles,
    status: BenkBehandlingsstatus,
    saksbehandler: Saksbehandler,
): List<SaksbehandlerBehandlingKommando> {
    val navIdent = saksbehandler.navIdent
    val erSaksbehandler = saksbehandler.erSaksbehandler()
    val erBeslutter = saksbehandler.erBeslutter()
    val erSattPåVent = felles.ventestatus.erSattPåVent

    return buildList {
        if (status == KLAR_TIL_BEHANDLING && felles.saksbehandler == null && erSaksbehandler) {
            add(TildelSaksbehandler)
        }
        if (status == KLAR_TIL_BESLUTNING && felles.beslutter == null && felles.saksbehandler != navIdent && erBeslutter) {
            add(TildelBeslutter)
        }
        if (status == BenkBehandlingsstatus.UNDER_BEHANDLING && felles.saksbehandler != navIdent && erSaksbehandler && felles.saksbehandler != null) {
            add(OvertaSaksbehandler)
        }
        if (status == UNDER_BESLUTNING && felles.beslutter != navIdent && erBeslutter && felles.beslutter != null && felles.saksbehandler != navIdent) {
            add(OvertaBeslutter)
        }
        if (status == BenkBehandlingsstatus.UNDER_BEHANDLING && erSaksbehandler && felles.saksbehandler == navIdent) {
            add(LeggTilbakeSaksbehandler)
        }
        if (status == UNDER_BESLUTNING && erBeslutter && felles.beslutter == navIdent) {
            add(LeggTilbakeBeslutter)
        }
        if (!erSattPåVent && when (status) {
                BenkBehandlingsstatus.UNDER_BEHANDLING -> erSaksbehandler && felles.saksbehandler == navIdent
                UNDER_BESLUTNING -> erBeslutter && felles.beslutter == navIdent
                else -> false
            }
        ) {
            add(SettPåVent)
        }
        if (erSattPåVent && when (status) {
                KLAR_TIL_BEHANDLING -> erSaksbehandler
                BenkBehandlingsstatus.UNDER_BEHANDLING -> erSaksbehandler && felles.saksbehandler == navIdent
                KLAR_TIL_BESLUTNING -> erBeslutter && felles.saksbehandler != navIdent
                else -> false
            }
        ) {
            add(Gjenoppta)
        }
        if (when (status) {
                BenkBehandlingsstatus.UNDER_BEHANDLING, KLAR_TIL_BESLUTNING -> felles.saksbehandler == navIdent
                UNDER_BESLUTNING -> felles.beslutter == navIdent
                else -> false
            }
        ) {
            add(Avbryt)
        }
    }
}
