package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import java.time.LocalDateTime

/**
 * Wrapper rundt hele kontorhistorikken for en ident.
 * Domenelogikk for å plukke ut "riktig" kontor for ulike formål bor her, slik at klienten kan returnere rådata uten å gjøre antakelser.
 */
data class Kontorhistorikk(
    val innslag: List<Kontorhistorikkinnslag>,
) {
    /**
     * Tilnærmet det vi får fra eksisterende veilarboppfolging-tjenesten i dag (Arena med fallback til geografisk tilknytning), men vi tar også med ARBEIDSOPPFOLGING som førstevalg.
     * Når det nye API'et kommer i prod med ARBEIDSOPPFOLGING vil det være det "riktige" kontoret for tiltakspenger; inntil da vil filteret ikke ha noen ARBEIDSOPPFOLGING-innslag og vi faller naturlig tilbake til Arena (og videre til geografisk tilknytning).
     *
     * Brukes i parallellkjøringen for å sammenligne mot gammel tjeneste.
     */
    fun nyesteAktuelleKontor(): Kontorhistorikkinnslag? =
        nyesteAvType(KontorType.ARBEIDSOPPFOLGING)
            ?: nyesteAvType(KontorType.ARENA)
            ?: nyesteAvType(KontorType.GEOGRAFISK_TILKNYTNING)

    private fun nyesteAvType(type: KontorType): Kontorhistorikkinnslag? =
        innslag.filter { it.kontorType == type }.maxByOrNull { it.endretTidspunkt }

    /**
     * Et enkelt innslag i kontorhistorikken til en ident.
     *
     * Siden vi utbetaler for perioder kan forskjellige meldeperioder høre til forskjellige kontorer, og [endretTidspunkt] gir oss det vi trenger for å avgjøre hvilket kontor som gjaldt når.
     */
    data class Kontorhistorikkinnslag(
        val kontorId: String,
        val kontorNavn: String?,
        val kontorType: KontorType,
        val endretTidspunkt: LocalDateTime,
    )

    enum class KontorType {
        ARBEIDSOPPFOLGING,
        ARENA,
        GEOGRAFISK_TILKNYTNING,
    }
}
