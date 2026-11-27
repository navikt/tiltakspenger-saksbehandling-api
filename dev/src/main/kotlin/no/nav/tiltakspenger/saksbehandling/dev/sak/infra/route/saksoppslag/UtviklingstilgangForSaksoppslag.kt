package no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.saksoppslag

import no.nav.tiltakspenger.saksbehandling.auditlog.AuditLogEvent
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollService
import no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.maskinoppslag.erVerifisertMaskin
import no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.maskinoppslag.loggMaskinoppslag
import no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.maskinoppslag.testaktør
import no.nav.tiltakspenger.saksbehandling.infra.route.correlationId
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.saksoppslag.Saksoppslagstilgang
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.saksoppslag.saksbehandlertilgangForSaksoppslag
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.tilUsladdetSakDTO
import java.time.Clock

fun utviklingstilgangForSaksoppslag(
    tilgangskontrollService: TilgangskontrollService,
    auditService: AuditService,
    clock: Clock,
): Saksoppslagstilgang {
    val saksbehandlertilgang = saksbehandlertilgangForSaksoppslag(
        tilgangskontrollService = tilgangskontrollService,
        auditService = auditService,
        clock = clock,
    )
    return tilgang@{
        if (!erVerifisertMaskin()) return@tilgang saksbehandlertilgang()
        val testaktør = testaktør() ?: return@tilgang null

        { sak ->
            auditService.logMedBrukerId(
                brukerId = sak.fnr,
                navIdent = testaktør.klientId,
                action = AuditLogEvent.Action.ACCESS,
                correlationId = correlationId(),
                contextMessage = "Maskin henter hele saken til brukeren",
            )
            val svar = sak.tilUsladdetSakDTO(testaktør, clock)
            testaktør.loggMaskinoppslag("saksoppslag")
            svar
        }
    }
}
