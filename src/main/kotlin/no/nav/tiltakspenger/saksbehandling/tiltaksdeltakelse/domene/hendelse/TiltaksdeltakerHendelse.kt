package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 *  En endring i en tiltaksdeltakelse, slik vi mottok eller hentet den fra en kilde.
 *  Lagres kun for sporbarhet, og verdien fra kilden tolkes ikke.
 *  [id] Vår interne id for endringen
 *  [internDeltakerId] Vår interne id for deltakelsen
 *  [eksternDeltakerId] Id for deltakelsen fra arena/tiltak/komet
 * */
data class TiltaksdeltakerHendelse(
    val id: TiltaksdeltakerHendelseId,
    val internDeltakerId: TiltaksdeltakerId,
    val eksternDeltakerId: String,
    val sakId: SakId,
)
