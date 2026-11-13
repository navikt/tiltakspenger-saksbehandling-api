package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdata

data class TiltaksdataDTO(
    val eksternDeltakelseId: String,
    val internDeltakelseId: String?,
    val tittel: SladdbarVerdi<String?>,
)

fun Tiltaksdata.toDTO(): TiltaksdataDTO = TiltaksdataDTO(
    eksternDeltakelseId = eksternDeltakelseId,
    internDeltakelseId = internDeltakelseId?.toString(),
    tittel = tittel.ikkeSladdet(),
)
