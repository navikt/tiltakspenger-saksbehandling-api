package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt.KlagebehandlingAvbruttDTO

/**
 * Sladding av [KlagebehandlingDTO].
 * Brevtekstene og begrunnelsene er saksbehandlers fritekst om personen.
 * Titlene på brevtekstene er faste ledetekster og beholdes, sammen med hjemler, tidspunkter og ID-er.
 */

fun KlagebehandlingDTO.sladdet(): KlagebehandlingDTO = this.copy(
    fnr = SladdetVerdi,
    avbrutt = avbrutt?.sladdet(),
    ventestatus = ventestatus.map { it.sladdet() },
    resultat = resultat?.sladdet(),
)

fun KlagebehandlingAvbruttDTO.sladdet(): KlagebehandlingAvbruttDTO = this.copy(
    begrunnelse = SladdetVerdi,
)

fun KlagebehandlingsresultatDTO.sladdet(): KlagebehandlingsresultatDTO = when (this) {
    is KlagebehandlingsresultatDTO.Avvist -> this.copy(
        brevtekst = brevtekst.sladdet(),
    )

    is KlagebehandlingsresultatDTO.Omgjør -> this.copy(
        begrunnelse = SladdetVerdi,
        begrunnelseFerdigstilling = SladdetVerdi,
    )

    is KlagebehandlingsresultatDTO.Opprettholdt -> this.copy(
        brevtekst = brevtekst.sladdet(),
        begrunnelseFerdigstilling = SladdetVerdi,
    )
}

private fun List<TittelOgSladdbarTekstDTO>.sladdet(): List<TittelOgSladdbarTekstDTO> =
    this.map { it.copy(tekst = SladdetVerdi) }

fun KlagebehandlingDTO.sladdetFor(saksbehandler: Saksbehandler): KlagebehandlingDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
