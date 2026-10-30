package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/** Tiltaksdeltakelsen på siste innvilgede dag i vedtaket forlengelsen [behandlingId] forlenger. */
fun Sak.tiltaksdeltakelsenSomForlenges(behandlingId: RammebehandlingId): TiltaksdeltakerId {
    val forlenger = (hentRammebehandling(behandlingId)!!.resultat as Revurderingsresultat.Forlengelse).forlenger
    return hentRammevedtakForId(forlenger.rammevedtakId).valgteTiltaksdeltakelser!!
        .hentVerdiForDag(forlenger.sisteInnvilgedeDag)!!
        .internDeltakelseId
}
