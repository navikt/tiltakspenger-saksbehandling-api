package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.kanAvbryte
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.kanGjenoppta
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.leggTilbake.kanLeggeTilbake
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.kanOverta
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.kanSettePåVent
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta.kanTaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando

/**
 * Handlinger som en saksbehandler/beslutter kan utføre på en meldekortbehandling.
 */
fun Meldekortbehandling.finnGyldigeKommandoer(saksbehandler: Saksbehandler): List<SaksbehandlerBehandlingKommando> {
    return buildList {
        if (kanTildeleSaksbehandler(saksbehandler)) add(SaksbehandlerBehandlingKommando.TildelSaksbehandler)
        if (kanTildeleBeslutter(saksbehandler)) add(SaksbehandlerBehandlingKommando.TildelBeslutter)
        if (kanOvertaSaksbehandler(saksbehandler)) add(SaksbehandlerBehandlingKommando.OvertaSaksbehandler)
        if (kanOvertaBeslutter(saksbehandler)) add(SaksbehandlerBehandlingKommando.OvertaBeslutter)
        if (kanLeggeTilbakeSaksbehandler(saksbehandler)) add(SaksbehandlerBehandlingKommando.LeggTilbakeSaksbehandler)
        if (kanLeggeTilbakeBeslutter(saksbehandler)) add(SaksbehandlerBehandlingKommando.LeggTilbakeBeslutter)
        if (kanSettePåVent(saksbehandler).isRight()) add(SaksbehandlerBehandlingKommando.SettPåVent)
        if (kanGjenoppta(saksbehandler).isRight()) add(SaksbehandlerBehandlingKommando.Gjenoppta)
        if (kanAvbryte(saksbehandler).isRight()) add(SaksbehandlerBehandlingKommando.Avbryt)
    }
}

private fun Meldekortbehandling.kanTildeleSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == KLAR_TIL_BEHANDLING && kanTaMeldekortbehandling(saksbehandler).isRight()

private fun Meldekortbehandling.kanOvertaSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BEHANDLING &&
        this.saksbehandler != saksbehandler.navIdent &&
        kanOverta(saksbehandler).isRight()

private fun Meldekortbehandling.kanLeggeTilbakeSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BEHANDLING && kanLeggeTilbake(saksbehandler).isRight()

private fun Meldekortbehandling.kanTildeleBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == KLAR_TIL_BESLUTNING && kanTaMeldekortbehandling(saksbehandler).isRight()

private fun Meldekortbehandling.kanOvertaBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BESLUTNING &&
        this.beslutter != saksbehandler.navIdent &&
        kanOverta(saksbehandler).isRight()

private fun Meldekortbehandling.kanLeggeTilbakeBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BESLUTNING && kanLeggeTilbake(saksbehandler).isRight()
