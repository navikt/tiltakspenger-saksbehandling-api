package no.nav.tiltakspenger.saksbehandling.domene.vedtak

import no.nav.tiltakspenger.libs.common.VedtakId

/**
 * TODO post-mvp jah: Denne vil ikke passe for klagevedtak og tilbakekrevingsvedtak, da må lage et abstraksjonslag til.
 */
interface Vedtak {
    val id: VedtakId

    // TODO post-mvp jah: Dette er nok en forenkling, siden må kunne anta at antallDagerForMeldeperiode forandrer seg innenfor et vedtak.
    val antallDagerPerMeldeperiode: Int
}
