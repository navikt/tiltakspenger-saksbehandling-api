package no.nav.tiltakspenger.saksbehandling.benk.domene

/**
 * Pagineringen benken ber om, som et 0-basert sidetall.
 * Sidestørrelsen er fast og kan ikke endres av kaller — benken blar gjennom fanen side for side, i stedet for å velge hvor store stegene er.
 * Kall [fra] med sidetallet fra requesten: et negativt eller absurd stort tall fra en url brukeren kan redigere gir da en gyldig side, framfor en overflow i offset-regnestykket.
 */
data class BenkPaginering(
    val side: Int = 0,
) {
    init {
        require(side >= 0) { "side må være >= 0, var $side" }
    }

    fun limit(): Int = SIDEANTALL

    fun offset(): Int = side * SIDEANTALL

    companion object {
        // TODO: øk denne før prodsetting
        const val SIDEANTALL = 10

        /**
         * Øverste sidetall som kan uttrykkes uten at `side * SIDEANTALL` overflower Int.
         * Benken har ingen reell øvre grense på antall sider; dette er kun overflow-guard, rundt 10 millioner sider.
         */
        private val MAKS_SIDE = Int.MAX_VALUE / SIDEANTALL

        fun fra(side: Int?): BenkPaginering = BenkPaginering((side ?: 0).coerceIn(0, MAKS_SIDE))
    }
}
