package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.dokument.TittelOgTekstDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.SLADDET_TEKST
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdet
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt.KlagebehandlingAvbruttDTO

/**
 * Sladding av [KlagebehandlingDTO].
 * Brevtekstene og begrunnelsene er saksbehandlers fritekst om personen.
 * Titlene på brevtekstene er faste ledetekster og beholdes, sammen med hjemler, tidspunkter og ID-er.
 */

fun KlagebehandlingDTO.sladdet(): KlagebehandlingDTO = this.copy(
    fnr = SLADDET_TEKST,
    avbrutt = avbrutt?.sladdet(),
    ventestatus = ventestatus.map { it.sladdet() },
    resultat = resultat?.sladdet(),
)

fun KlagebehandlingAvbruttDTO.sladdet(): KlagebehandlingAvbruttDTO = this.copy(
    begrunnelse = begrunnelse?.let { SLADDET_TEKST },
)

fun KlagebehandlingsresultatDTO.sladdet(): KlagebehandlingsresultatDTO = when (this) {
    is KlagebehandlingsresultatDTO.Avvist -> this.copy(
        brevtekst = brevtekst.sladdet(),
    )

    is KlagebehandlingsresultatDTO.Omgjør -> this.copy(
        begrunnelse = SLADDET_TEKST,
        begrunnelseFerdigstilling = begrunnelseFerdigstilling?.let { SLADDET_TEKST },
    )

    is KlagebehandlingsresultatDTO.Opprettholdt -> this.copy(
        brevtekst = brevtekst.sladdet(),
        begrunnelseFerdigstilling = begrunnelseFerdigstilling?.let { SLADDET_TEKST },
    )
}

private fun List<TittelOgTekstDTO>.sladdet(): List<TittelOgTekstDTO> = this.map { it.copy(tekst = SLADDET_TEKST) }

fun KlagebehandlingDTO.sladdetFor(saksbehandler: Saksbehandler): KlagebehandlingDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
