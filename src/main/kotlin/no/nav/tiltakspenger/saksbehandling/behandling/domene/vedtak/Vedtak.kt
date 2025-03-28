package no.nav.tiltakspenger.saksbehandling.behandling.domene.vedtak

import no.nav.tiltakspenger.libs.common.VedtakId
import java.time.LocalDateTime

/**
 * Kommentar jah: Denne vil ikke passe for klagevedtak og tilbakekrevingsvedtak, da må vi lage et abstraksjonslag til.
 */
interface Vedtak {
    val id: VedtakId
    val opprettet: LocalDateTime

    // TODO post-mvp jah: Dette er nok en forenkling, siden må kunne anta at antallDagerForMeldeperiode forandrer seg innenfor et vedtak.
    val antallDagerPerMeldeperiode: Int

    fun erStansvedtak(): Boolean
}
