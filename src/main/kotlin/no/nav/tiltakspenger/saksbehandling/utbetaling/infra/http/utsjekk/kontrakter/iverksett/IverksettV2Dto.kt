package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.iverksett

import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.Personident
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.Satstype
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Representerer en iverksetting. Se funksjonell dokumentasjon på [utsjekk-docs](https://navikt.github.io/utsjekk-docs/for_konsumenter/utbetaling/iverksetting)
 * @property sakId Id på saken i saksbehandlingsløsningen.
 * @property behandlingId Id på behandlingen i saksbehandlingsløsningen.
 * @property iverksettingId Id som unikt identifiserer iverksettingen. Brukes når konsument iverksetter flere ganger for samme behandling.
 * @property personident Naturlig ident for personen iverksettingen gjelder.
 * @property vedtak Data knyttet til vedtaket. Se no.nav.utsjekk.kontrakter.iverksett.VedtaksdetaljerV2Dto.
 * @property forrigeIverksetting Id på forrige iverksetting på saken. Settes kun når saken har eksisterende iverksettinger.
 */
data class IverksettV2Dto(
    val sakId: String,
    val behandlingId: String,
    val iverksettingId: String? = null,
    val personident: Personident,
    val vedtak: VedtaksdetaljerV2Dto =
        VedtaksdetaljerV2Dto(
            vedtakstidspunkt = LocalDateTime.now(),
            saksbehandlerId = "",
            beslutterId = "",
        ),
    val forrigeIverksetting: ForrigeIverksettingV2Dto? = null,
)

/**
 * @property vedtakstidspunkt Tidspunktet vedtaket ble fattet.
 * @property saksbehandlerId NAV-ident til saksbehandler som har behandlet vedtaket – evt. navn på system/servicebruker dersom vedtaket ble behandlet fullautomatisk.
 * @property beslutterId NAV-ident til beslutter som har godkjent vedtaket – evt. navn på system/servicebruker dersom vedtaket ble behandlet fullautomatisk.
 * @property utbetalinger Gjeldende totalbilde av utbetalingene på saken.
 * Se [funksjonell dokumentasjon](https://navikt.github.io/utsjekk-docs/for_konsumenter/utbetaling/iverksetting#gjeldende-totalbilde-av-utbetalinger-p%C3%A5-saken)
 */
data class VedtaksdetaljerV2Dto(
    val vedtakstidspunkt: LocalDateTime,
    val saksbehandlerId: String,
    val beslutterId: String,
    val utbetalinger: List<UtbetalingV2Dto> = emptyList(),
)

/**
 * Data for en enkelt utbetalingsperiode.
 * @property beløp Beløpet som skal utbetales iht. satstypen.
 * @property satstype Benevningen som avgjør hva totalbeløpet for perioden blir. Denne varierer fra ytelse til ytelse iht.
 * det konkrete ytelsesregelverket. Eksempelvis er alle meldepliktsytelser dagytelser og må sende satstype DAGLIG.
 * @property fraOgMedDato Fra og med-dato for utbetalingsperioden.
 * @property tilOgMedDato Til og med-dato for utbetalingsperioden.
 * @property stønadsdata Ekstra data om utbetalingsperioden. Varierer fra ytelse til ytelse, se de konkrete klassene for detaljer.
 */
data class UtbetalingV2Dto(
    val beløp: UInt,
    val satstype: Satstype,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val stønadsdata: StønadsdataTiltakspengerV2Dto,
)

/**
 * Identifiserer forrige iverksetting, brukes når nåværende iverksetting ikke er første iverksetting på saken.
 * Dersom nåværende iverksetting har både behandlingsid og iverksettingsid satt, må også forrige iverksetting ha begge felter satt.
 * @property behandlingId behandlingsid for forrige iverksetting
 * @property iverksettingId iverksettingsid for forrige iverksetting. Brukes når konsument iverksetter flere ganger for samme behandling.
 */
data class ForrigeIverksettingV2Dto(
    val behandlingId: String,
    val iverksettingId: String? = null,
)
