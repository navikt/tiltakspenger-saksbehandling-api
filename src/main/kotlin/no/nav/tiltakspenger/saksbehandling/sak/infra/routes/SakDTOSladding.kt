package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.sladdet
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.sladdet
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.sladdet

/**
 * Sladding av [SakDTO].
 * Fødselsnummeret på saken erstattes, og hver underliggende liste sladdes av sin egen utvidelse.
 * Saksnummer, ID-er, tidslinjer og utbetalingstidslinjen er ikke identifiserende og beholdes.
 */

fun SakDTO.sladdet(): SakDTO = this.copy(
    fnr = SLADDET_TEKST,
    søknader = søknader.map { it.sladdet() },
    rammebehandlinger = rammebehandlinger.map { it.sladdet() },
    klagebehandlinger = klagebehandlinger.map { it.sladdet() },
    alleRammevedtak = alleRammevedtak.map { it.sladdet() },
    meldekortbehandlinger = meldekortbehandlinger.mapValues { (_, meldekortbehandling) -> meldekortbehandling.sladdet() },
)

fun SakDTO.sladdetFor(saksbehandler: Saksbehandler): SakDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
