package no.nav.tiltakspenger.saksbehandling.klage.domene.opprett

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
    val vedtakIdSomOmgjøres: VedtakId?,
    override val correlationId: CorrelationId,
) : OpprettRammebehandlingFraKlageKommando {
    enum class Type {
        INNVILGELSE,
        OMGJØRING,
    }
    init {
        when (type) {
            Type.INNVILGELSE -> require(vedtakIdSomOmgjøres == null) {
                "vedtakIdSomOmgjøres må være null for INNVILGELSE"
            }
            Type.OMGJØRING -> require(vedtakIdSomOmgjøres != null) {
                "vedtakIdSomOmgjøres kan ikke være null for OMGJØRING"
            }
        }
    }
}

data class OpprettSøknadsbehandlingFraKlageKommando(
    override val sakId: SakId,
    override val saksbehandler: Saksbehandler,
    override val klagebehandlingId: KlagebehandlingId,
    val søknadId: SøknadId,
    override val correlationId: CorrelationId,
) : OpprettRammebehandlingFraKlageKommando
