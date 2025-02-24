package no.nav.tiltakspenger.saksbehandling.domene.vilkår

import no.nav.tiltakspenger.libs.periodisering.Periodisering

/**
 * Utfall for en sammenhengende periode.
 * Dersom det gjelder utfall for en periodisering, skal [SamletUtfall] brukes.
 */
enum class UtfallForPeriode {
    OPPFYLT,
    IKKE_OPPFYLT,
    UAVKLART, ;

    /**
     * Kombinerer to utfall for en periode til ett utfall.
     * UAVKLART > IKKE_OPPFYLT > OPPFYLT
     */
    fun kombiner(other: UtfallForPeriode): UtfallForPeriode =
        when (this) {
            OPPFYLT ->
                when (other) {
                    OPPFYLT -> OPPFYLT
                    IKKE_OPPFYLT -> IKKE_OPPFYLT
                    UAVKLART -> UAVKLART
                }
            IKKE_OPPFYLT ->
                when (other) {
                    OPPFYLT -> IKKE_OPPFYLT
                    IKKE_OPPFYLT -> IKKE_OPPFYLT
                    UAVKLART -> UAVKLART
                }
            UAVKLART -> UAVKLART
        }

    /**
     * @throws IllegalStateException dersom [UtfallForPeriode] er [UAVKLART]
     */
    fun toAvklartUtfallForPeriode(): AvklartUtfallForPeriode =
        when (this) {
            OPPFYLT -> AvklartUtfallForPeriode.OPPFYLT
            IKKE_OPPFYLT -> AvklartUtfallForPeriode.IKKE_OPPFYLT
            UAVKLART -> throw IllegalStateException("Kan ikke konvertere UAVKLART til AvklartUtfallForPeriode")
        }
}

/**
 * @throws IllegalStateException dersom [SamletUtfall] er [UAVKLART]
 */
fun Periodisering<UtfallForPeriode>.toAvklartUtfallForPeriode(): Periodisering<AvklartUtfallForPeriode> =
    map {
        it.toAvklartUtfallForPeriode()
    }
