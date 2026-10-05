package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

/**
 * Kontoret en person hører til, slik ao-oppfolgingskontor har valgt det.
 * Tjenesten prioriterer arbeidsoppfølgingskontor, deretter Arena-kontor og til slutt geografisk tilknytning.
 * [kontorType] forteller hvilken av disse kildene kontoret kommer fra.
 */
data class KontorTilhørighet(
    val kontorId: String,
    val kontorNavn: String,
    val kontorType: KontorType,
) {
    fun tilNavkontor(): Navkontor = Navkontor(
        kontornummer = kontorId,
        kontornavn = kontorNavn,
    )

    enum class KontorType {
        ARBEIDSOPPFOLGING,
        ARENA,
        GEOGRAFISK_TILKNYTNING,
    }
}
