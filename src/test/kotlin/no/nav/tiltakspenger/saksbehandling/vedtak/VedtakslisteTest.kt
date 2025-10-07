package no.nav.tiltakspenger.saksbehandling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.junit.jupiter.api.Test

internal class VedtakslisteTest {
    @Test
    fun `default antall dager for hele innvilgelsesperioden`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1 til 31.januar(2025)
            val (sak, _, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                virkningsperiode = innvilgelsesperiode,
                resultat = SøknadsbehandlingType.INNVILGELSE,
            )
            sak.rammevedtaksliste.antallDagerPerMeldeperiode shouldBe Periodisering(
                AntallDagerForMeldeperiode.default,
                innvilgelsesperiode,
            )
            sak.rammevedtaksliste.antallDagerForMeldeperiode(16.desember(2024) til 29.desember(2024)) shouldBe null
            sak.rammevedtaksliste.antallDagerForMeldeperiode(30.desember(2024) til 12.januar(2025)) shouldBe AntallDagerForMeldeperiode.default
            sak.rammevedtaksliste.antallDagerForMeldeperiode(13.januar(2025) til 26.januar(2025)) shouldBe AntallDagerForMeldeperiode.default
            sak.rammevedtaksliste.antallDagerForMeldeperiode(27.januar(2025) til 9.februar(2025)) shouldBe AntallDagerForMeldeperiode.default
            sak.rammevedtaksliste.antallDagerForMeldeperiode(10.februar(2025) til 23.februar(2025)) shouldBe null
        }
    }

    @Test
    fun `ren splitt 2 meldeperioder`() {
        withTestApplicationContext { tac ->
            // 2 hele meldeperioder
            val innvilgelsesperiode = 1 til 28.september(2025)
            val antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                PeriodeMedVerdi(AntallDagerForMeldeperiode(9), 1 til 14.september(2025)),
                PeriodeMedVerdi(AntallDagerForMeldeperiode(8), 15 til 28.september(2025)),
            )
            val (sak, _, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                virkningsperiode = innvilgelsesperiode,
                resultat = SøknadsbehandlingType.INNVILGELSE,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            )
            sak.rammevedtaksliste.antallDagerPerMeldeperiode shouldBe antallDagerPerMeldeperiode
            sak.rammevedtaksliste.antallDagerForMeldeperiode(1 til 14.september(2025)) shouldBe AntallDagerForMeldeperiode(9)
            sak.rammevedtaksliste.antallDagerForMeldeperiode(15 til 28.september(2025)) shouldBe AntallDagerForMeldeperiode(8)
        }
    }

    @Test
    fun `kutter andre meldeperiode i 2`() {
        withTestApplicationContext { tac ->
            // 2 hele meldeperioder
            val innvilgelsesperiode = 1 til 28.september(2025)
            val antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                PeriodeMedVerdi(AntallDagerForMeldeperiode(9), 1 til 21.september(2025)),
                PeriodeMedVerdi(AntallDagerForMeldeperiode(8), 22 til 28.september(2025)),
            )
            val (sak, _, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                virkningsperiode = innvilgelsesperiode,
                resultat = SøknadsbehandlingType.INNVILGELSE,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            )
            sak.rammevedtaksliste.antallDagerPerMeldeperiode shouldBe antallDagerPerMeldeperiode
            sak.rammevedtaksliste.antallDagerForMeldeperiode(1 til 14.september(2025)) shouldBe AntallDagerForMeldeperiode(9)
            sak.rammevedtaksliste.antallDagerForMeldeperiode(15 til 28.september(2025)) shouldBe AntallDagerForMeldeperiode(9)
        }
    }

    @Test
    fun `1 dag inn i neste meldeperiode`() {
        withTestApplicationContext { tac ->
            // 2 hele meldeperioder
            val innvilgelsesperiode = 1 til 28.september(2025)
            val antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                PeriodeMedVerdi(AntallDagerForMeldeperiode(9), 1 til 15.september(2025)),
                PeriodeMedVerdi(AntallDagerForMeldeperiode(8), 16 til 28.september(2025)),
            )
            val (sak, _, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                virkningsperiode = innvilgelsesperiode,
                resultat = SøknadsbehandlingType.INNVILGELSE,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            )
            sak.rammevedtaksliste.antallDagerPerMeldeperiode shouldBe antallDagerPerMeldeperiode
            sak.rammevedtaksliste.antallDagerForMeldeperiode(1 til 14.september(2025)) shouldBe AntallDagerForMeldeperiode(9)
            sak.rammevedtaksliste.antallDagerForMeldeperiode(15 til 28.september(2025)) shouldBe AntallDagerForMeldeperiode(9)
        }
    }
}
