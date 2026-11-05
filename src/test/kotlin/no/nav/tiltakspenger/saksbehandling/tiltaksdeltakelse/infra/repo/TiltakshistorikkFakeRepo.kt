package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag
import java.util.concurrent.CopyOnWriteArrayList

class TiltakshistorikkFakeRepo : TiltakshistorikkRepo {
    private val data = CopyOnWriteArrayList<Tiltakshistorikkinnslag>()

    override fun lagre(innslag: Tiltakshistorikkinnslag, sessionContext: SessionContext) {
        data.add(innslag)
    }

    override fun erLikSisteVerdi(
        tiltaksdeltakerId: TiltaksdeltakerId,
        verdi: Tiltaksdeltakelse.GirRett,
        sessionContext: SessionContext,
    ): Boolean {
        val siste = data.filter { it.tiltaksdeltakerId == tiltaksdeltakerId }
            .maxWithOrNull(compareBy<Tiltakshistorikkinnslag> { it.innhentet }.thenBy { it.id.toString() })
        return siste?.verdi?.tilNåtilstandDbJson() == verdi.tilNåtilstandDbJson()
    }
}
