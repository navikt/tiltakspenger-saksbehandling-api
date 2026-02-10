package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles

import com.fasterxml.jackson.annotation.JsonCreator
import tools.jackson.databind.JsonNode

class Personident(verdi: String) : Ident(verdi) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun deserialize(json: String) = Personident(json)

        @JvmStatic
        @JsonCreator
        fun deserializeObject(json: JsonNode) = Personident(json.get("verdi").asString())
    }
}
