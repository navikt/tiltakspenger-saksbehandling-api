package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * JSON-payloaden vi lagrer i kolonnen `tilbakekreving_hendelse.behandling`.
 */
data class TilbakekrevingHendelseBehandlingDb(
    val behandlingId: String,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatusDb,
    val forrigeBehandlingsstatus: TilbakekrevingBehandlingsstatusDb?,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: PeriodeDbJson,
    val venter: TilbakekrevingVenterDb?,
)

fun TilbakekrevingBehandlingEndretHendelse.tilDbBehandlingJson(): String =
    TilbakekrevingHendelseBehandlingDb(
        behandlingId = tilbakeBehandlingId,
        sakOpprettet = sakOpprettet,
        varselSendt = varselSendt,
        behandlingsstatus = behandlingsstatus.tilDb(),
        forrigeBehandlingsstatus = forrigeBehandlingsstatus?.tilDb(),
        totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
        saksbehandlingURL = url,
        fullstendigPeriode = fullstendigPeriode.toDbJson(),
        venter = venter?.tilDb(),
    ).let { serialize(it) }
