package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata

/**
 * Kontoret en person hører til, slik ao-oppfolgingskontor har valgt det.
 * Tjenesten prioriterer arbeidsoppfølgingskontor, deretter Arena-kontor og til slutt geografisk tilknytning.
 * [kontorType] forteller hvilken av disse kildene kontoret kommer fra.
 */
data class KontorTilhørighet(
    val kontorId: String,
    val kontorNavn: String,
    val kontorType: KontorType,
) {
    fun tilNavkontor(): Navkontor = Navkontor(
        kontornummer = kontorId,
        kontornavn = kontorNavn,
    )
}

/**
 * Kontortilhørighet pakket sammen med httpklient sin metadata for kallet.
 * [kontorTilhørighet] er `null` når ao-oppfolgingskontor ikke har noe kontor for personen.
 * [httpKlientMetadata] bærer rå request/response, headere, antall forsøk og timing, slik at vi kan logge eller lagre rådata ved behov.
 */
data class KontorTilhørighetMedMetadata(
    val kontorTilhørighet: KontorTilhørighet?,
    val httpKlientMetadata: HttpKlientMetadata,
)
