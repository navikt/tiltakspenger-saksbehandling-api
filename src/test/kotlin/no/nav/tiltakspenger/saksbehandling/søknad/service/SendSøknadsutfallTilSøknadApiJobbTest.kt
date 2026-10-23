package no.nav.tiltakspenger.saksbehandling.søknad.service

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.søknad.domene.KunneIkkeSendeSøknadsutfall
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfall
import org.junit.jupiter.api.Test

class SendSøknadsutfallTilSøknadApiJobbTest {

    @Test
    @IsolatedDatabaseTest
    fun `sender utfallet av iverksatte søknadsbehandlinger én gang`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, innvilgetSøknad, innvilgelse) = iverksettSøknadsbehandling(tac)
            val (_, avslåttSøknad, avslag) = iverksettSøknadsbehandling(
                tac,
                resultat = SøknadsbehandlingsresultatType.AVSLAG,
            )

            tac.sendSøknadsutfallTilSøknadApiJobb.send()
            tac.sendSøknadsutfallTilSøknadApiJobb.send()

            tac.søknadApiFakeKlient.sendteSøknadsutfall.filter {
                it.søknadId in listOf(innvilgetSøknad.id, avslåttSøknad.id)
            } shouldBe listOf(
                Søknadsutfall(
                    vedtakId = innvilgelse.id,
                    søknadId = innvilgetSøknad.id,
                    resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
                    vedtakstidspunkt = innvilgelse.opprettet,
                ),
                Søknadsutfall(
                    vedtakId = avslag.id,
                    søknadId = avslåttSøknad.id,
                    resultat = SøknadsbehandlingsresultatType.AVSLAG,
                    vedtakstidspunkt = avslag.opprettet,
                ),
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `teknisk feil lar utfallet ligge i køen til neste kjøring`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, søknad, vedtak) = iverksettSøknadsbehandling(tac)
            tac.søknadApiFakeKlient.feilFor(
                søknad.id,
                KunneIkkeSendeSøknadsutfall.Teknisk(ObjectMother.httpKlientUventetStatus()),
            )

            tac.sendSøknadsutfallTilSøknadApiJobb.send()
            tac.søknadApiFakeKlient.sendteSøknadsutfall.filter { it.søknadId == søknad.id }.shouldBeEmpty()

            tac.søknadApiFakeKlient.feilFor(søknad.id, null)
            tac.sendSøknadsutfallTilSøknadApiJobb.send()

            tac.søknadApiFakeKlient.sendteSøknadsutfall.filter { it.søknadId == søknad.id }
                .map { it.vedtakId } shouldBe listOf(vedtak.id)
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `søknad som søknad-api ikke kjenner forsøkes ikke sendt på nytt`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, søknad) = iverksettSøknadsbehandling(tac)
            tac.søknadApiFakeKlient.feilFor(
                søknad.id,
                KunneIkkeSendeSøknadsutfall.SøknadFinnesIkke(ObjectMother.httpKlientUventetStatus(statusCode = 404)),
            )

            tac.sendSøknadsutfallTilSøknadApiJobb.send()
            tac.søknadApiFakeKlient.feilFor(søknad.id, null)
            tac.sendSøknadsutfallTilSøknadApiJobb.send()

            tac.søknadApiFakeKlient.sendteSøknadsutfall.filter { it.søknadId == søknad.id }.shouldBeEmpty()
        }
    }
}
