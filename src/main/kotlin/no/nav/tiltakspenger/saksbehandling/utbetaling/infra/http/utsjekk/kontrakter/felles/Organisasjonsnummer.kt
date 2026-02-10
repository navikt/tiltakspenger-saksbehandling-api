package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles

import com.fasterxml.jackson.annotation.JsonCreator
import tools.jackson.databind.JsonNode

class Organisasjonsnummer(verdi: String) : Ident(verdi) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun deserialize(json: String) = Organisasjonsnummer(json)

        @JvmStatic
        @JsonCreator
        fun deserializeObject(json: JsonNode) = Organisasjonsnummer(json.get("verdi").asString())
    }
}
