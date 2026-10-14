package no.nav.tiltakspenger.saksbehandling.behandling.domene

/**
 * Saksbehandlers valg om hvilke begrunnelser i en rammebehandling som skal journalføres som et internt notat i Joark etter iverksettelse.
 * Begrunnelsene som er valgt, samles i ett notat, som journalføres når minst én er valgt.
 * Notatet er kun synlig internt i Nav (Gosys), og deles ikke med bruker.
 *
 * @param vilkårsvurdering Begrunnelsen for vilkårsvurderingen.
 * @param barnetillegg Begrunnelsen for barnetillegget.
 */
data class SkalJournalføreNotat(
    val vilkårsvurdering: Boolean,
    val barnetillegg: Boolean,
) {
    val skalJournalføres: Boolean get() = vilkårsvurdering || barnetillegg

    companion object {
        val INGEN = SkalJournalføreNotat(vilkårsvurdering = false, barnetillegg = false)
    }
}
