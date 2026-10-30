package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerKlagebehandlingKommando

fun Sak.finnGyldigeKommandoer(
    klagebehandling: Klagebehandling,
    saksbehandler: Saksbehandler,
): List<SaksbehandlerKlagebehandlingKommando> {
    return klagebehandling.finnGyldigeKommandoer(saksbehandler, hentAktivTilknyttetBehandling(klagebehandling))
}

/**
 * Handlinger som en saksbehandler kan utføre på en klagebehandling.
 * Klagebehandlingen har ingen beslutter, så brukere uten saksbehandlerrolle får alltid en tom liste.
 *
 * Har klagen en aktiv tilknyttet ramme- eller meldekortbehandling, delegerer klagens handlinger til den tilknyttede behandlingen.
 * Da utledes kommandoene fra den tilknyttede behandlingens saksbehandlerkommandoer.
 * Er den tilknyttede behandlingen sendt til beslutning, må handlingene gjøres fra den tilknyttede behandlingen, og klagen får ingen kommandoer.
 *
 * @param aktivTilknyttetBehandling se [Sak.hentAktivTilknyttetBehandling].
 */
fun Klagebehandling.finnGyldigeKommandoer(
    saksbehandler: Saksbehandler,
    aktivTilknyttetBehandling: AktivTilknyttetBehandling?,
): List<SaksbehandlerKlagebehandlingKommando> {
    if (!saksbehandler.erSaksbehandler || erAvsluttet) return emptyList()
    if (aktivTilknyttetBehandling != null) {
        return finnGyldigeKommandoerViaTilknyttetBehandling(saksbehandler, aktivTilknyttetBehandling)
    }
    return buildList {
        if (kanSaksbehandle(saksbehandler)) add(SaksbehandlerKlagebehandlingKommando.Saksbehandle)
        if (kanTildele()) add(SaksbehandlerKlagebehandlingKommando.Tildel)
        if (kanOverta(saksbehandler)) add(SaksbehandlerKlagebehandlingKommando.Overta)
        if (kanLeggeTilbake(saksbehandler)) add(SaksbehandlerKlagebehandlingKommando.LeggTilbake)
        if (kanSettePåVent(saksbehandler)) add(SaksbehandlerKlagebehandlingKommando.SettPåVent)
        if (kanOppdaterePåVent()) add(SaksbehandlerKlagebehandlingKommando.OppdaterSettPåVent)
        if (kanGjenoppta()) add(SaksbehandlerKlagebehandlingKommando.Gjenoppta)
        if (kanAvbryte(saksbehandler)) add(SaksbehandlerKlagebehandlingKommando.Avbryt)
    }
}

private fun Klagebehandling.finnGyldigeKommandoerViaTilknyttetBehandling(
    saksbehandler: Saksbehandler,
    aktivTilknyttetBehandling: AktivTilknyttetBehandling,
): List<SaksbehandlerKlagebehandlingKommando> {
    val (tilknyttetStatus, tilknyttedeKommandoer) = when (aktivTilknyttetBehandling) {
        is AktivTilknyttetBehandling.Ramme -> aktivTilknyttetBehandling.rammebehandling.let {
            it.status.tilTilknyttetBehandlingsstatus() to it.finnGyldigeKommandoer(saksbehandler)
        }

        is AktivTilknyttetBehandling.Meldekort -> aktivTilknyttetBehandling.meldekortbehandling.let {
            it.status.tilTilknyttetBehandlingsstatus() to it.finnGyldigeKommandoer(saksbehandler)
        }
    }
    if (tilknyttetStatus != TilknyttetBehandlingsstatus.KLAR_TIL_BEHANDLING && tilknyttetStatus != TilknyttetBehandlingsstatus.UNDER_BEHANDLING) {
        return emptyList()
    }
    return tilknyttedeKommandoer.mapNotNull {
        when (it) {
            SaksbehandlerBehandlingKommando.Saksbehandle -> if (kanSaksbehandle(saksbehandler)) SaksbehandlerKlagebehandlingKommando.Saksbehandle else null

            SaksbehandlerBehandlingKommando.TildelSaksbehandler -> SaksbehandlerKlagebehandlingKommando.Tildel

            SaksbehandlerBehandlingKommando.OvertaSaksbehandler -> SaksbehandlerKlagebehandlingKommando.Overta

            SaksbehandlerBehandlingKommando.LeggTilbakeSaksbehandler -> SaksbehandlerKlagebehandlingKommando.LeggTilbake

            SaksbehandlerBehandlingKommando.SettPåVent -> SaksbehandlerKlagebehandlingKommando.SettPåVent

            SaksbehandlerBehandlingKommando.OppdaterSettPåVent -> SaksbehandlerKlagebehandlingKommando.OppdaterSettPåVent

            SaksbehandlerBehandlingKommando.Gjenoppta -> SaksbehandlerKlagebehandlingKommando.Gjenoppta

            // Klagen kan ikke avbrytes så lenge den er knyttet til en behandling.
            // De øvrige kommandoene gjelder beslutter eller den tilknyttede behandlingen alene.
            SaksbehandlerBehandlingKommando.Avbryt,
            SaksbehandlerBehandlingKommando.Beslutte,
            SaksbehandlerBehandlingKommando.TildelBeslutter,
            SaksbehandlerBehandlingKommando.AngreSendTilBeslutning,
            SaksbehandlerBehandlingKommando.OvertaBeslutter,
            SaksbehandlerBehandlingKommando.LeggTilbakeBeslutter,
            SaksbehandlerBehandlingKommando.HentTilbake,
            SaksbehandlerBehandlingKommando.Gjenåpne,
            -> null
        }
    }
}

private val statuserSomKanBehandles = setOf(UNDER_BEHANDLING, MOTTATT_FRA_KLAGEINSTANS, OMGJØRING_ETTER_KLAGEINSTANS)

private fun Klagebehandling.kanSaksbehandle(saksbehandler: Saksbehandler): Boolean =
    status in statuserSomKanBehandles &&
        erSaksbehandlerPåBehandlingen(saksbehandler) &&
        !ventestatus.erSattPåVent

/** Speiler [no.nav.tiltakspenger.saksbehandling.klage.domene.ta.ta]. */
private fun Klagebehandling.kanTildele(): Boolean =
    status == KLAR_TIL_BEHANDLING && this.saksbehandler == null

/** Speiler [no.nav.tiltakspenger.saksbehandling.klage.domene.overta.overta]. */
private fun Klagebehandling.kanOverta(saksbehandler: Saksbehandler): Boolean =
    status in statuserSomKanBehandles &&
        !ventestatus.erSattPåVent &&
        this.saksbehandler != null &&
        !erSaksbehandlerPåBehandlingen(saksbehandler)

/** Speiler [no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake.leggTilbake]. */
private fun Klagebehandling.kanLeggeTilbake(saksbehandler: Saksbehandler): Boolean =
    (status == UNDER_BEHANDLING || status == OMGJØRING_ETTER_KLAGEINSTANS) &&
        erSaksbehandlerPåBehandlingen(saksbehandler)

/** Speiler [no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVentOgNullstillSaksbehandler] uten oppdatering. */
private fun Klagebehandling.kanSettePåVent(saksbehandler: Saksbehandler): Boolean =
    status in statuserSomKanBehandles &&
        !ventestatus.erSattPåVent &&
        erSaksbehandlerPåBehandlingen(saksbehandler)

/** Speiler [no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent.settPåVentOgNullstillSaksbehandler] med oppdatering. */
private fun Klagebehandling.kanOppdaterePåVent(): Boolean =
    (status == UNDER_BEHANDLING || status == KLAR_TIL_BEHANDLING) && ventestatus.erSattPåVent

/** Speiler [no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta.gjenopptaKlagebehandling]. */
private fun Klagebehandling.kanGjenoppta(): Boolean =
    (status == UNDER_BEHANDLING || status == KLAR_TIL_BEHANDLING || status == OMGJØRING_ETTER_KLAGEINSTANS) &&
        ventestatus.erSattPåVent

/**
 * Speiler [no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.avbryt].
 * Begrenset til statusene der klagen er under behandling hos oss.
 * En klage som er opprettholdt og sendt til klageinstansen tilbys derfor ikke avbrutt.
 */
private fun Klagebehandling.kanAvbryte(saksbehandler: Saksbehandler): Boolean =
    status in statuserSomKanBehandles &&
        !ventestatus.erSattPåVent &&
        !erKnyttetTilBehandling &&
        erSaksbehandlerPåBehandlingen(saksbehandler)
