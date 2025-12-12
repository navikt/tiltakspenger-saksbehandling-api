package no.nav.tiltakspenger.saksbehandling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

/**
 * Enkelt case der man kun trenger en sak for å opprette en klagebehandling til avvisning (knyttes ikke til et vedtak).
 * Merk at det ikke persisteres søknad eller søknadsbehandling.
 */
internal fun TestDataHelper.persisterOpprettetKlagebehandlingTilAvvisning(
    sakId: SakId = SakId.random(),
    klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
    erKlagerPartISaken: Boolean = true,
    klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
    erKlagefristenOverholdt: Boolean = true,
    erKlagenSignert: Boolean = true,
): Pair<Sak, Klagebehandling> {
    this.persisterSak(sak = sak)
    val klagebehandling = ObjectMother.opprettKlagebehandling(
        id = klagebehandlingId,
        sakId = sak.id,
        fnr = sak.fnr,
        saksnummer = sak.saksnummer,
        erKlagerPartISaken = erKlagerPartISaken,
        klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
        erKlagefristenOverholdt = erKlagefristenOverholdt,
        erKlagenSignert = erKlagenSignert,
    )
    this.sessionFactory.withTransactionContext { tx ->
        this.klagebehandlingRepo.lagreKlagebehandling(
            klagebehandling = klagebehandling,
            transactionContext = tx,
        )
    }
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    oppdatertSak.behandlinger.klagebehandlinger.single() shouldBe klagebehandling
    return Pair(
        oppdatertSak,
        klagebehandling,
    )
}
