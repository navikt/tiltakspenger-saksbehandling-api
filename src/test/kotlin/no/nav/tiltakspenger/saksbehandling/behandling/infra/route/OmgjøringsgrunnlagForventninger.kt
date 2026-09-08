package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons

/**
 * Feilresponsen saksbehandler får når behandlingen sendes til beslutning og et annet vedtak har endret omgjøringsgrunnlaget.
 */
val omgjøringsgrunnlagetErEndretForSaksbehandler: ForventetRespons = ForventetRespons.json(
    400,
    """
    {
      "melding": "Et annet vedtak har endret grunnlaget for omgjøringen. Vedtaksperioden må oppdateres før behandlingen kan sendes til beslutning.",
      "kode": "omgjøringsgrunnlaget_er_endret"
    }
    """.trimIndent(),
    "application/json; charset=UTF-8",
)

/**
 * Samme feil sett fra beslutter ved iverksetting.
 */
val omgjøringsgrunnlagetErEndretForBeslutter: ForventetRespons = ForventetRespons.json(
    400,
    """
    {
      "melding": "Et annet vedtak har endret grunnlaget for omgjøringen. Behandlingen må sendes tilbake til saksbehandler for oppdatering av vedtaksperioden.",
      "kode": "omgjøringsgrunnlaget_er_endret"
    }
    """.trimIndent(),
    "application/json; charset=UTF-8",
)
