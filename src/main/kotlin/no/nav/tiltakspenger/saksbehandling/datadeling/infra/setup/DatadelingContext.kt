package no.nav.tiltakspenger.saksbehandling.datadeling.infra.setup

import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasSystemTokenProvider
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.datadeling.DatadelingClient
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.client.DatadelingHttpClient
import no.nav.tiltakspenger.saksbehandling.datadeling.infra.jobb.SendTilDatadelingJobb
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.MdcKorrelasjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import java.time.Clock

open class DatadelingContext(
    texasClient: TexasClient,
    rammevedtakRepo: RammevedtakRepo,
    rammebehandlingRepo: RammebehandlingRepo,
    sakRepo: SakRepo,
    meldekortbehandlingRepo: MeldekortbehandlingRepo,
    meldekortvedtakRepo: MeldekortvedtakRepo,
    clock: Clock,
) {
    open val datadelingKlient: DatadelingClient by lazy {
        DatadelingHttpClient(
            baseUrl = Configuration.datadelingUrl,
            clock = clock,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.datadelingScope,
            ),
        )
    }

    val sendTilDatadelingJobb by lazy {
        SendTilDatadelingJobb(
            rammevedtakRepo = rammevedtakRepo,
            rammebehandlingRepo = rammebehandlingRepo,
            sakRepo = sakRepo,
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            meldekortvedtakRepo = meldekortvedtakRepo,
            datadelingClient = datadelingKlient,
            clock = clock,
            korrelasjon = MdcKorrelasjon(),
        )
    }
}
