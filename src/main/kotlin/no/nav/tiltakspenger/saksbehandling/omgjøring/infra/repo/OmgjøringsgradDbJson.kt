package no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo

import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad

/**
 * Sier noe om et vedtak er omgjør eller omgjort helt eller delvis.
 * Se også [Omgjøringsgrad] i domenelaget.
 * Skal kun brukes i db-laget.
 */
enum class OmgjøringsgradDbJson {
    HELT,
    DELVIS,
    ;

    fun toDomain(): Omgjøringsgrad {
        return when (this) {
            HELT -> Omgjøringsgrad.HELT
            DELVIS -> Omgjøringsgrad.DELVIS
        }
    }
}

fun Omgjøringsgrad.toDbJson(): OmgjøringsgradDbJson {
    return when (this) {
        Omgjøringsgrad.HELT -> OmgjøringsgradDbJson.HELT
        Omgjøringsgrad.DELVIS -> OmgjøringsgradDbJson.DELVIS
    }
}
