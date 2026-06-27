package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

internal class TilbakekrevingBehandlingEndretHendelseTest {

    @Test
    fun `oppdaterer ikke behandling når url er lik verdi men ulik referanse`() {
        val tilbakeBehandlingId = "tilbake-behandling-123"
        val urlVerdi = "https://tilbakekreving.nav.no/behandling/$tilbakeBehandlingId"
        // Tving fram to String-instanser med lik verdi, men ulik referanse, slik at referansesammenligning (!==) gir et annet svar enn verdisammenligning (!=).
        val urlPaaBehandling = buildString { append(urlVerdi) }
        val urlIHendelse = buildString { append(urlVerdi) }
        check(urlPaaBehandling == urlIHendelse) { "Verdiene skal vere like" }
        check(urlPaaBehandling !== urlIHendelse) { "Referansene skal vere ulike" }

        val periode = Periode(fraOgMed = LocalDate.of(2025, 1, 1), tilOgMed = LocalDate.of(2025, 1, 31))
        val sistEndret = LocalDateTime.of(2025, 2, 1, 12, 0)
        val hendelseOpprettet = sistEndret.plusSeconds(10)

        val behandling = TilbakekrevingBehandling(
            id = TilbakekrevingId.random(),
            sakId = SakId.random(),
            utbetalingId = UtbetalingId.random(),
            tilbakeBehandlingId = tilbakeBehandlingId,
            opprettet = LocalDateTime.of(2025, 1, 15, 12, 0),
            sistEndret = sistEndret,
            status = TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
            url = urlPaaBehandling,
            kravgrunnlagTotalPeriode = periode,
            totaltFeilutbetaltBeløp = BigDecimal("1000.00"),
            varselSendt = LocalDate.of(2025, 1, 20),
            saksbehandler = null,
            beslutter = null,
            venter = null,
        )

        val hendelse = TilbakekrevingBehandlingEndretHendelse(
            id = TilbakekrevinghendelseId.random(),
            opprettet = hendelseOpprettet,
            behandlet = null,
            sakId = behandling.sakId,
            eksternFagsakId = "fagsak-1",
            feil = null,
            eksternBehandlingId = "ekstern-1",
            tilbakeBehandlingId = tilbakeBehandlingId,
            sakOpprettet = LocalDateTime.of(2025, 1, 1, 12, 0),
            varselSendt = behandling.varselSendt,
            behandlingsstatus = behandling.status,
            forrigeBehandlingsstatus = null,
            totaltFeilutbetaltBeløp = behandling.totaltFeilutbetaltBeløp,
            url = urlIHendelse,
            fullstendigPeriode = periode,
            venter = null,
        )

        // Ingen felter er endret (kun url-referansen er ulik), så behandlingen skal ikke oppdateres.
        hendelse.oppdaterBehandlingHvisEndret(behandling) shouldBe null
    }
}
