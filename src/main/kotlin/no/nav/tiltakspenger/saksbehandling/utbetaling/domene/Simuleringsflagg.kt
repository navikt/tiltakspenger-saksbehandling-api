package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

/**
 * Hva simuleringen sier om én meldeperiode, uttrykt som fakta og ikke som dommer.
 *
 * Backend svarer på hva som er sant; frontend bestemmer hvor høyt det skal rope.
 * Da kan fagsiden flytte grensen mellom «advar» og «skjerp» uten at vi rører domenekoden -- og uten migrering, siden [SimulertBeregning] utledes og aldri lagres.
 *
 * Sender vi ut ferdige dommer i stedet, har vi låst fagsidens valg her inne.
 */
data class Simuleringsflagg(
    val harJustering: Boolean,

    /**
     * Justeringene balanserer innenfor sin egen kalendermåned.
     * Det er som regel en omfordeling mellom dager, typisk fordi en korrigering har flyttet en dag.
     */
    val justeringGårOppINull: Boolean,

    /**
     * Justeringen er ikke balansert innenfor denne meldeperioden, altså er beløp motregnet mot andre meldeperioder eller måneder.
     * Sperrer iverksetting -- med mindre justeringene balanserer per kalendermåned på tvers av simuleringen, uten feilutbetaling og uten reversert ytelse i de ubalanserte meldeperiodene; da er det oppdrag som omfordeler trekk, og saksbehandler får advarsel i stedet.
     */
    val justeringPåTversAvMeldeperiodeEllerMåned: Boolean,

    /**
     * En positiv feilutbetaling er et varsel om at det vil komme et kravgrunnlag for tilbakekreving.
     * Negative feilutbetalingsposteringer er reversering av et tidligere krav og flagges ikke -- de gir ikke noe nytt kravgrunnlag.
     */
    val harFeilutbetaling: Boolean,

    /**
     * Trekk er ren tilleggsinformasjon og stopper aldri noe.
     * Feltet er sant også når trekket er negativt, som det er i de aller fleste tilfellene.
     */
    val harTrekk: Boolean,
) {
    companion object {
        val ingenSimulering = Simuleringsflagg(
            harJustering = false,
            justeringGårOppINull = false,
            justeringPåTversAvMeldeperiodeEllerMåned = false,
            harFeilutbetaling = false,
            harTrekk = false,
        )

        fun fraPosteringer(posteringer: List<Postering>, harUbalansertJustering: Boolean): Simuleringsflagg {
            val harJustering = posteringer.any { it.erJustering }
            return Simuleringsflagg(
                harJustering = harJustering,
                justeringGårOppINull = harJustering && !harUbalansertJustering,
                justeringPåTversAvMeldeperiodeEllerMåned = harUbalansertJustering,
                harFeilutbetaling = posteringer.any {
                    it.type == Posteringstype.FEILUTBETALING && it.klassekode == OppsummeringGenerator.KLASSEKODE_FEILUTBETALING && it.beløp > 0
                },
                harTrekk = posteringer.any { it.type == Posteringstype.TREKK },
            )
        }
    }
}
