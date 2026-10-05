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
     * Kontoret vi bruker som navkontor (oppfølgingsenhet) for personen.
     * ARBEIDSOPPFOLGING er førstevalg, med fallback til Arena og videre til geografisk tilknytning (tilsvarende det den tidligere veilarboppfolging-tjenesten ga).
     * Når APIet leverer ARBEIDSOPPFOLGING-innslag i prod vil det være det "riktige" kontoret for tiltakspenger; inntil da faller vi naturlig tilbake til Arena.
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
    ) {
        fun tilNavkontor(): Navkontor = Navkontor(
            kontornummer = kontorId,
            kontornavn = kontorNavn,
        )
    }

    enum class KontorType {
        ARBEIDSOPPFOLGING,
        ARENA,
        GEOGRAFISK_TILKNYTNING,
    }
}
