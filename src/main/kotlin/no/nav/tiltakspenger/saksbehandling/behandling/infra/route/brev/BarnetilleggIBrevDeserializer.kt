package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.OppdaterBarnetilleggDTO
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

/**
 * Eldre versjoner av frontenden sender barnetillegget til forhåndsvisning av brev som en liste med antall barn per periode, mens nyere versjoner sender [OppdaterBarnetilleggDTO].
 * Listen gjøres om til [OppdaterBarnetilleggDTO.perioder].
 *
 * TODO: Fjernes når alle saksbehandlere har frontenden som sender barna.
 */
class BarnetilleggIBrevDeserializer : ValueDeserializer<OppdaterBarnetilleggDTO>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OppdaterBarnetilleggDTO {
        if (!p.isExpectedStartArrayToken) {
            return ctxt.readValue(p, OppdaterBarnetilleggDTO::class.java)
        }

        val listetype = ctxt.typeFactory.constructCollectionType(List::class.java, BarnetilleggPeriodeDTO::class.java)
        return OppdaterBarnetilleggDTO(
            perioder = ctxt.readValue<List<BarnetilleggPeriodeDTO>>(p, listetype),
            begrunnelse = null,
        )
    }
}
