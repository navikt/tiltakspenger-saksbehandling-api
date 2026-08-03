package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.avbryt.kanAvbryte
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.kanGjenoppta
import no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake.kanLeggeTilbake
import no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.kanOverta
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.kanSettePåVent
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ta.kanTaBehandling
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando

/**
 * Handlinger som en saksbehandler/beslutter kan utføre på en rammebehandling.
 */
fun Rammebehandling.finnGyldigeKommandoer(saksbehandler: Saksbehandler): List<SaksbehandlerBehandlingKommando> {
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

private fun Rammebehandling.kanTildeleSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == KLAR_TIL_BEHANDLING && kanTaBehandling(saksbehandler).isRight()

private fun Rammebehandling.kanOvertaSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BEHANDLING &&
        this.saksbehandler != saksbehandler.navIdent &&
        kanOverta(saksbehandler).isRight()

private fun Rammebehandling.kanLeggeTilbakeSaksbehandler(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BEHANDLING && kanLeggeTilbake(saksbehandler).isRight()

private fun Rammebehandling.kanTildeleBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == KLAR_TIL_BESLUTNING && kanTaBehandling(saksbehandler).isRight()

private fun Rammebehandling.kanOvertaBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BESLUTNING &&
        this.beslutter != saksbehandler.navIdent &&
        kanOverta(saksbehandler).isRight()

private fun Rammebehandling.kanLeggeTilbakeBeslutter(saksbehandler: Saksbehandler): Boolean =
    status == UNDER_BESLUTNING && kanLeggeTilbake(saksbehandler).isRight()
