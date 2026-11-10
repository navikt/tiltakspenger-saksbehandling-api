package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import java.time.Clock
import java.time.LocalDateTime

@JvmInline
value class Utbetalingsreferanse(val verdi: String) {
    init {
        require(verdi.length == 15)
    }
}

data class Utbetalingskobling(
    val utbetalingId: UtbetalingId,
    val sakId: SakId,
)

data class UtbetalingsreferanseTreff(
    val referanse: Utbetalingsreferanse,
    val kobling: Utbetalingskobling,
)

sealed interface Utbetalingskvittering {
    val referanser: Set<Utbetalingsreferanse>

    data class Status(
        val status: Utbetalingsstatus,
        override val referanser: Set<Utbetalingsreferanse>,
    ) : Utbetalingskvittering

    data class Simulering(
        override val referanser: Set<Utbetalingsreferanse>,
    ) : Utbetalingskvittering
}

data class RåUtbetalingskvittering(
    val id: UtbetalingskvitteringId,
    val key: String?,
    val value: String?,
    val mottatt: LocalDateTime,
    val behandlet: LocalDateTime?,
    val sakId: SakId?,
    val utbetalingId: UtbetalingId?,
    val forsøkshistorikk: Forsøkshistorikk?,
) {
    val utbetalingskobling: Utbetalingskobling?
        get() = if (utbetalingId != null && sakId != null) Utbetalingskobling(utbetalingId, sakId) else null

    fun skalBehandles(clock: Clock): Boolean =
        behandlet == null && (forsøkshistorikk?.nesteForsøk?.let { it <= nå(clock) } ?: true)

    fun markerBehandlet(tidspunkt: LocalDateTime): RåUtbetalingskvittering = copy(behandlet = tidspunkt)

    fun registrerForsøk(clock: Clock): Forsøkshistorikk =
        (forsøkshistorikk ?: Forsøkshistorikk.opprett(clock = clock)).inkrementer(clock = clock)

    fun knyttTil(kobling: Utbetalingskobling): RåUtbetalingskvittering = copy(
        sakId = sakId ?: kobling.sakId,
        utbetalingId = utbetalingId ?: kobling.utbetalingId,
    )

    override fun toString(): String =
        "RåUtbetalingskvittering(id=$id, mottatt=$mottatt, behandlet=$behandlet, sakId=$sakId, utbetalingId=$utbetalingId, forsøkshistorikk=$forsøkshistorikk)"
}
