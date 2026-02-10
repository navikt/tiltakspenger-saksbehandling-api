package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.OmgjørKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

sealed interface Klagebehandlingsresultat {

    val brevtekst: Brevtekster?
    val kanIverksette: Boolean?
    val kanIkkeIverksetteGrunner: List<String>

    val erKnyttetTilRammebehandling: Boolean

    /**
     * Merk at en avvisning ikke er det samme som et avslag.
     * Det er et vedtak som kan klages på.
     */
    data class Avvist(
        override val brevtekst: Brevtekster?,
    ) : Klagebehandlingsresultat {
        override val kanIverksette: Boolean = !brevtekst.isNullOrEmpty()
        override val kanIkkeIverksetteGrunner: List<String> by lazy {
            val grunner = mutableListOf<String>()
            if (brevtekst.isNullOrEmpty()) grunner.add("Må ha minst et element i brevtekst")
            grunner
        }
        override val erKnyttetTilRammebehandling = false

        fun oppdaterBrevtekst(
            brevtekst: Brevtekster,
        ): Avvist {
            return this.copy(
                brevtekst = brevtekst,
            )
        }

        companion object {
            val empty = Avvist(null)
        }
    }

    /**
     * Grunnen til at dette er et imperativ verb, er at selve klagebehandlingen utfører ikke omgjøringen; det er den følgende rammebehandlingen som gjør.
     * @param rammebehandlingId Genereres av systemet når klagen omgjøres til en rammebehandling. Vil være null ved f.eks. medhold på klage om tilbakekreving.
     */
    data class Omgjør(
        val årsak: KlageOmgjøringsårsak,
        val begrunnelse: Begrunnelse,
        val rammebehandlingId: BehandlingId?,
    ) : Klagebehandlingsresultat {
        override val brevtekst = null

        /** Denne tar ikke stilling til rammebehandlingens tilstand, så helheten kan kun avgjøres av iverksettelse av rammebehandlingen. */
        override val kanIverksette: Boolean = rammebehandlingId != null
        override val kanIkkeIverksetteGrunner: List<String> = if (rammebehandlingId == null) {
            listOf("Må ha en rammebehandlingId for å kunne iverksette omgjøring")
        } else {
            emptyList()
        }

        override val erKnyttetTilRammebehandling = rammebehandlingId != null

        /** Kan oppdatere frem til rammebehandlingen er KLAR_TIL_BESLUTNING */
        fun oppdater(kommando: OmgjørKlagebehandlingKommando): Omgjør {
            return this.copy(
                årsak = kommando.årsak,
                begrunnelse = kommando.begrunnelse,
            )
        }

        fun oppdaterRammebehandlingId(
            rammebehandlingId: BehandlingId,
        ): Omgjør = this.copy(rammebehandlingId = rammebehandlingId)

        fun fjernRammebehandlingId(rammmebehandlingId: BehandlingId): Omgjør {
            require(this.rammebehandlingId == rammmebehandlingId) {
                "Kan kun fjerne rammebehandlingId hvis den matcher eksisterende verdi"
            }
            return this.copy(rammebehandlingId = null)
        }
    }

    data class Opprettholdt(
        val hjemler: NonEmptySet<Klagehjemmel>,
        override val brevtekst: Brevtekster?,
    ) : Klagebehandlingsresultat {
        override val kanIverksette: Boolean = !brevtekst.isNullOrEmpty()
        override val kanIkkeIverksetteGrunner: List<String> by lazy {
            val grunner = mutableListOf<String>()
            if (brevtekst.isNullOrEmpty()) grunner.add("Må ha minst et element i brevtekst")
            if (hjemler.isEmpty()) grunner.add("Må ha minst en hjemmel")
            grunner
        }
        override val erKnyttetTilRammebehandling = false

        fun oppdaterBrevtekst(brevtekst: Brevtekster): Opprettholdt = this.copy(brevtekst = brevtekst)
    }
}
