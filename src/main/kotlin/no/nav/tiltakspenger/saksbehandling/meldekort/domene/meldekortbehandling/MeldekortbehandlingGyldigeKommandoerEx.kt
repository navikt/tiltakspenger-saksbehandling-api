package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.kanGjenoppta
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando

/**
 * Handlinger som en saksbehandler/beslutter kan utføre på en meldekortbehandling.
 *
 * Hver kommando evalueres for seg selv via egne `kan...`-funksjoner.
 */
fun Meldekortbehandling.gyldigeKommandoer(saksbehandler: Saksbehandler): List<SaksbehandlerBehandlingKommando> {
    return buildList {
        if (kanTildelSaksbehandler(saksbehandler)) add(SaksbehandlerBehandlingKommando.TildelSaksbehandler)
        if (kanOvertaSaksbehandler(saksbehandler)) add(SaksbehandlerBehandlingKommando.OvertaSaksbehandler)
        if (kanLeggeTilbakeSaksbehandler(saksbehandler)) add(SaksbehandlerBehandlingKommando.LeggTilbakeSaksbehandler)
        if (kanTildelBeslutter(saksbehandler)) add(SaksbehandlerBehandlingKommando.TildelBeslutter)
        if (kanOvertaBeslutter(saksbehandler)) add(SaksbehandlerBehandlingKommando.OvertaBeslutter)
        if (kanLeggeTilbakeBeslutter(saksbehandler)) add(SaksbehandlerBehandlingKommando.LeggTilbakeBeslutter)
        if (kanGjenoppta(saksbehandler).isRight()) add(SaksbehandlerBehandlingKommando.Gjenoppta)
    }
}

private fun Meldekortbehandling.kanTildelSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == KLAR_TIL_BEHANDLING && saksbehandler.erSaksbehandler()

private fun Meldekortbehandling.kanOvertaSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BEHANDLING &&
        this.saksbehandler != saksbehandler.navIdent &&
        saksbehandler.erSaksbehandler()

private fun Meldekortbehandling.kanLeggeTilbakeSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BEHANDLING && this.saksbehandler == saksbehandler.navIdent

private fun Meldekortbehandling.kanTildelBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == KLAR_TIL_BESLUTNING &&
        saksbehandler.erBeslutter() &&
        this.saksbehandler != saksbehandler.navIdent

private fun Meldekortbehandling.kanOvertaBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BESLUTNING &&
        this.beslutter != saksbehandler.navIdent &&
        saksbehandler.erBeslutter() &&
        this.saksbehandler != saksbehandler.navIdent

private fun Meldekortbehandling.kanLeggeTilbakeBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BESLUTNING && this.beslutter == saksbehandler.navIdent
