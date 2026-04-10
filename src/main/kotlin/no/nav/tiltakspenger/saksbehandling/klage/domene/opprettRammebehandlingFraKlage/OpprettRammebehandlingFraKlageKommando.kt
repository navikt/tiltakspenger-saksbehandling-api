package no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

sealed interface OpprettRammebehandlingFraKlageKommando {
    val sakId: SakId
    val saksbehandler: Saksbehandler
    val klagebehandlingId: KlagebehandlingId
    val correlationId: CorrelationId
}

data class OpprettRevurderingFraKlageKommando(
    override val sakId: SakId,
    override val saksbehandler: Saksbehandler,
    override val klagebehandlingId: KlagebehandlingId,
    val type: Type,
    override val correlationId: CorrelationId,
    val vedtakIdSomOmgjøres: VedtakId?,
) : OpprettRammebehandlingFraKlageKommando {

    init {
        if (type == Type.OMGJØRING && vedtakIdSomOmgjøres == null) {
            throw IllegalArgumentException("vedtakIdSomOmgjøres må være satt for type Omgjøring")
        }
    }

    enum class Type {
        INNVILGELSE,
        OMGJØRING,
    }
}

data class OpprettSøknadsbehandlingFraKlageKommando(
    override val sakId: SakId,
    override val saksbehandler: Saksbehandler,
    override val klagebehandlingId: KlagebehandlingId,
    val søknadId: SøknadId,
    override val correlationId: CorrelationId,
) : OpprettRammebehandlingFraKlageKommando
