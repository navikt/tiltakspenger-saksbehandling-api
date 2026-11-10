package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import no.nav.tiltakspenger.saksbehandling.common.Loggfanger
import no.nav.tiltakspenger.saksbehandling.common.Sikkerloggfanger
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.Utbetalingsmålinger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeBehandleUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KvitteringsstatusLagring
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.RåUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskobling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsreferanse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingsreferanseTreff
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.tilUtbetalingsstatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration

class BehandleUtbetalingskvitteringerJobbTest {
    private class Oppsett(sessionFactory: SessionFactory = TestSessionFactory()) {
        val utbetaling = ObjectMother.utbetaling()
        val kvitteringId = UtbetalingskvitteringId.random()
        val innboks = mockk<UtbetalingskvitteringRepo>(relaxed = true)
        val repo = mockk<UtbetalingRepo>()
        val logg = Loggfanger("kvittering")
        val sikkerlogg = Sikkerloggfanger()
        val målinger = Utbetalingsmålinger(SimpleMeterRegistry())
        val jobb = BehandleUtbetalingskvitteringerJobb(
            innboks,
            repo,
            ::tilUtbetalingsstatus,
            fixedClock,
            sessionFactory,
            målinger,
            logg,
            sikkerlogg,
        )

        init {
            every { repo.hent(utbetaling.id, any()) } returns utbetaling
            every { repo.hentOppdragsversjon(utbetaling.sakId) } returns Oppdragsversjon.V3
            every { repo.lagreKvitteringsstatus(any(), any(), any()) } returns KvitteringsstatusLagring.Lagret
        }

        fun kvittering(
            value: String? = """{"status":"OK","detaljer":{"linjer":[]}}""",
            key: String? = utbetaling.id.toString(),
            historikk: Forsøkshistorikk? = null,
            behandlet: Boolean = false,
            sakId: SakId? = null,
            utbetalingId: UtbetalingId? = null,
        ) {
            every { innboks.hent(kvitteringId, any()) } returns RåUtbetalingskvittering(
                id = kvitteringId,
                key = key,
                value = value,
                mottatt = nå(fixedClock),
                behandlet = if (behandlet) nå(fixedClock) else null,
                sakId = sakId,
                utbetalingId = utbetalingId,
                forsøkshistorikk = historikk,
            )
        }
    }

    @Test
    fun `lagrer status og ferdigmarkering i samme transaksjon med full loggkontekst`() {
        val oppsett = Oppsett()
        oppsett.kvittering()
        oppsett.jobb.behandle(oppsett.kvitteringId).getOrFail()
        verify { oppsett.repo.lagreKvitteringsstatus(oppsett.utbetaling.id, Utbetalingsstatus.Ok, TestSessionFactory.transactionContext) }
        verify {
            oppsett.innboks.markerBehandlet(
                oppsett.kvitteringId,
                nå(fixedClock),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
                TestSessionFactory.transactionContext,
            )
        }
        oppsett.logg.logglinjer.single().melding!!.contains(oppsett.utbetaling.enkelLoggKontekst) shouldBe true
        oppsett.sikkerlogg.sikkerlogglinjer.size shouldBe 1
    }

    @Test
    fun `permanente feil beholdes med backoff og rådata lekker ikke i vanlig logg`() {
        listOf(
            null to "ukjent-personverdi",
            """{"status":"NY","hemmelig":"råinnhold"}""" to "ukjent-personverdi",
            "ikke json med råinnhold" to "ukjent-personverdi",
        ).forEach { (value, key) ->
            val oppsett = Oppsett()
            oppsett.kvittering(value, key)
            oppsett.jobb.behandle(oppsett.kvitteringId).leftOrNull()!!.forbigående shouldBe false
            verify(exactly = 0) { oppsett.innboks.markerBehandlet(any(), any(), any(), any()) }
            verify {
                oppsett.innboks.lagreFeil(
                    oppsett.kvitteringId,
                    any(),
                    false,
                    match { it.antallForsøk == 1L && it.nesteForsøk == nå(fixedClock).plusMinutes(1) },
                    null,
                )
            }
            oppsett.logg.logglinjer.single().let {
                it.melding!!.contains("ukjent-personverdi") shouldBe false
                it.melding.contains("råinnhold") shouldBe false
                it.feil shouldBe null
            }
        }
    }

    @Test
    fun `manglende utbetaling og lagringsfeil prøves på nytt uten ferdigmarkering`() {
        val mangler = Oppsett()
        mangler.kvittering()
        every { mangler.repo.hent(any(), any()) } returns null
        mangler.jobb.behandle(mangler.kvitteringId).leftOrNull() shouldBe
            KunneIkkeBehandleUtbetalingskvittering.FantIkkeUtbetaling(mangler.utbetaling.id)
        verify { mangler.innboks.lagreFeil(mangler.kvitteringId, "FantIkkeUtbetaling", true, any(), null) }
        verify(exactly = 0) { mangler.innboks.markerBehandlet(any(), any(), any(), any()) }

        val lagringsfeil = Oppsett()
        lagringsfeil.kvittering()
        every { lagringsfeil.innboks.markerBehandlet(any(), any(), any(), any()) } throws IllegalStateException("databasefeil")
        lagringsfeil.jobb.behandle(lagringsfeil.kvitteringId).leftOrNull()!!.forbigående shouldBe true
        verify {
            lagringsfeil.innboks.lagreFeil(
                lagringsfeil.kvitteringId,
                "UventetFeil",
                true,
                any(),
                Utbetalingskobling(lagringsfeil.utbetaling.id, lagringsfeil.utbetaling.sakId),
            )
        }
        lagringsfeil.logg.logglinjer.size shouldBe 1

        lagringsfeil.logg.logglinjer.single().melding!!.contains(lagringsfeil.utbetaling.enkelLoggKontekst) shouldBe true
        lagringsfeil.logg.logglinjer.single().melding!!.contains("Lagret utbetalingsstatus") shouldBe false

        val statuslagringsfeil = Oppsett()
        statuslagringsfeil.kvittering()
        every { statuslagringsfeil.repo.lagreKvitteringsstatus(any(), any(), any()) } returns KvitteringsstatusLagring.FantIkkeUtbetaling
        statuslagringsfeil.jobb.behandle(statuslagringsfeil.kvitteringId).leftOrNull() shouldBe
            KunneIkkeBehandleUtbetalingskvittering.FantIkkeUtbetaling(statuslagringsfeil.utbetaling.id)
        verify {
            statuslagringsfeil.innboks.lagreFeil(
                statuslagringsfeil.kvitteringId,
                "FantIkkeUtbetaling",
                true,
                any(),
                Utbetalingskobling(statuslagringsfeil.utbetaling.id, statuslagringsfeil.utbetaling.sakId),
            )
        }
    }

    @Test
    fun `commitfeil etter transaksjonsblokken lagrer feil med kontekst uten suksesstatuslogg`() {
        val standardSessionFactory = TestSessionFactory()
        val feilendeCommit = object : SessionFactory by standardSessionFactory {
            override fun <T> withTransactionContext(
                disableSessionCounter: Boolean,
                action: (TransactionContext) -> T,
            ): T {
                standardSessionFactory.withTransactionContext(disableSessionCounter, action)
                error("commitfeil")
            }
        }
        val oppsett = Oppsett(feilendeCommit)
        oppsett.kvittering()

        oppsett.jobb.behandle(oppsett.kvitteringId).leftOrNull()!!.forbigående shouldBe true

        verify { oppsett.repo.lagreKvitteringsstatus(oppsett.utbetaling.id, Utbetalingsstatus.Ok, any()) }
        verify {
            oppsett.innboks.markerBehandlet(
                oppsett.kvitteringId,
                any(),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
                any(),
            )
        }
        verify {
            oppsett.innboks.lagreFeil(
                oppsett.kvitteringId,
                "UventetFeil",
                true,
                any(),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
            )
        }
        oppsett.logg.logglinjer.single().melding!!.let {
            it.contains(oppsett.utbetaling.enkelLoggKontekst) shouldBe true
            it.contains("Kunne ikke behandle utbetalingskvittering: UventetFeil") shouldBe true
            it.contains("Lagret utbetalingsstatus") shouldBe false
        }
    }

    @Test
    fun `V2-kvittering med UUID-nøkkel kobles via alle linjereferanser uten statusendring`() {
        val oppsett = Oppsett()
        val referanse = Utbetalingsreferanse(oppsett.utbetaling.id.uuidPart())
        val key = "550e8400-e29b-41d4-a716-446655440000"
        val value = """{"status":"OK","detaljer":{"ytelse":"TILTAKSPENGER","linjer":[{"behandlingId":"${referanse.verdi}"}]}}"""
        oppsett.kvittering(value, key)
        every {
            oppsett.repo.finnUtbetalingerForKvitteringsreferanser(setOf(referanse), TestSessionFactory.transactionContext)
        } returns listOf(UtbetalingsreferanseTreff(referanse, Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId)))
        every { oppsett.repo.hentOppdragsversjon(oppsett.utbetaling.sakId) } returns Oppdragsversjon.V2

        oppsett.jobb.behandle(oppsett.kvitteringId).getOrFail()

        verify {
            oppsett.repo.finnUtbetalingerForKvitteringsreferanser(setOf(referanse), TestSessionFactory.transactionContext)
        }
        verify(exactly = 0) { oppsett.repo.lagreKvitteringsstatus(any(), any(), any()) }
        verify {
            oppsett.innboks.markerBehandlet(
                oppsett.kvitteringId,
                any(),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
                TestSessionFactory.transactionContext,
            )
        }
        oppsett.logg.logglinjer.size shouldBe 0
    }

    @Test
    fun `manglende og tvetydig V2-kobling prøves på nytt uten ferdigmarkering`() {
        val mangler = Oppsett()
        mangler.kvittering("""{"status":"OK"}""", key = "v2-key")
        mangler.jobb.behandle(mangler.kvitteringId).leftOrNull() shouldBe
            KunneIkkeBehandleUtbetalingskvittering.ManglerUtbetalingskobling
        verify {
            mangler.innboks.lagreFeil(
                mangler.kvitteringId,
                "ManglerUtbetalingskobling",
                true,
                any(),
                null,
            )
        }
        verify(exactly = 0) { mangler.innboks.markerBehandlet(any(), any(), any(), any()) }

        val tvetydig = Oppsett()
        val referanse = Utbetalingsreferanse(tvetydig.utbetaling.id.uuidPart())
        tvetydig.kvittering(
            """{"status":"HOS_OPPDRAG","detaljer":{"linjer":[{"behandlingId":"${referanse.verdi}"}]}}""",
            key = "v2-key",
        )
        every {
            tvetydig.repo.finnUtbetalingerForKvitteringsreferanser(setOf(referanse), any())
        } returns listOf(
            UtbetalingsreferanseTreff(referanse, Utbetalingskobling(tvetydig.utbetaling.id, tvetydig.utbetaling.sakId)),
            UtbetalingsreferanseTreff(referanse, Utbetalingskobling(UtbetalingId.random(), SakId.random())),
        )

        tvetydig.jobb.behandle(tvetydig.kvitteringId).leftOrNull() shouldBe
            KunneIkkeBehandleUtbetalingskvittering.TvetydigUtbetalingskobling
        verify {
            tvetydig.innboks.lagreFeil(
                tvetydig.kvitteringId,
                "TvetydigUtbetalingskobling",
                true,
                any(),
                null,
            )
        }
        verify(exactly = 0) { tvetydig.innboks.markerBehandlet(any(), any(), any(), any()) }
    }

    @Test
    fun `ugyldig V3-nøkkel faller ikke tilbake til linjereferanse`() {
        val oppsett = Oppsett()
        val referanse = Utbetalingsreferanse(oppsett.utbetaling.id.uuidPart())
        oppsett.kvittering(
            """{"status":"OK","detaljer":{"linjer":[{"behandlingId":"${referanse.verdi}"}]}}""",
            key = "utbetaling_ugyldig",
        )

        oppsett.jobb.behandle(oppsett.kvitteringId).leftOrNull() shouldBe
            KunneIkkeBehandleUtbetalingskvittering.UgyldigNøkkel
        verify(exactly = 0) { oppsett.repo.finnUtbetalingerForKvitteringsreferanser(any(), any()) }
        verify(exactly = 0) { oppsett.innboks.markerBehandlet(any(), any(), any(), any()) }
    }

    @Test
    fun `eksakt V3-nøkkel er autoritativ framfor linjereferanser`() {
        val oppsett = Oppsett()
        val eldreReferanse = Utbetalingsreferanse("0123456789ABCDE")
        oppsett.kvittering(
            """{"status":"OK","detaljer":{"linjer":[{"behandlingId":"${eldreReferanse.verdi}"}]}}""",
        )

        oppsett.jobb.behandle(oppsett.kvitteringId).getOrFail()

        verify(exactly = 0) { oppsett.repo.finnUtbetalingerForKvitteringsreferanser(any(), any()) }
        verify {
            oppsett.innboks.markerBehandlet(
                oppsett.kvitteringId,
                any(),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
                any(),
            )
        }
    }

    @Test
    fun `sen status som ignoreres etter terminal status ferdigmarkeres uten suksesstatuslogg`() {
        val oppsett = Oppsett()
        oppsett.kvittering("""{"status":"HOS_OPPDRAG"}""")
        every {
            oppsett.repo.lagreKvitteringsstatus(any(), any(), any())
        } returns KvitteringsstatusLagring.IgnorertFordiUtbetalingenErTerminal

        oppsett.jobb.behandle(oppsett.kvitteringId).getOrFail()

        verify {
            oppsett.innboks.markerBehandlet(
                oppsett.kvitteringId,
                any(),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
                any(),
            )
        }
        oppsett.logg.logglinjer.size shouldBe 0
    }

    @Test
    fun `feilet ferdigmarkering etter V3-statuslagring prøves på nytt med kobling`() {
        val oppsett = Oppsett()
        oppsett.kvittering()
        var antallMarkeringer = 0
        every {
            oppsett.innboks.markerBehandlet(oppsett.kvitteringId, any(), any(), any())
        } answers {
            antallMarkeringer += 1
            if (antallMarkeringer == 1) error("databasefeil")
        }

        oppsett.jobb.behandle(oppsett.kvitteringId).leftOrNull()!!.forbigående shouldBe true
        verify {
            oppsett.innboks.lagreFeil(
                oppsett.kvitteringId,
                "UventetFeil",
                true,
                any(),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
            )
        }

        oppsett.jobb.behandle(oppsett.kvitteringId).getOrFail()
        antallMarkeringer shouldBe 2
        verify(exactly = 1) { oppsett.innboks.lagreFeil(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `simuleringer avsluttes uten statusendring og kjente V2-utbetalinger kobles`() {
        val simulering = Oppsett()
        simulering.kvittering("""{"simulering":true}""", "ikke-utbetalingsnøkkel")
        simulering.jobb.behandle(simulering.kvitteringId).getOrFail()
        verify(exactly = 0) { simulering.repo.hent(any(), any()) }
        verify { simulering.innboks.markerBehandlet(simulering.kvitteringId, any(), null, any()) }
        verify(exactly = 0) { simulering.repo.lagreKvitteringsstatus(any(), any(), any()) }

        val versjon2 = Oppsett()
        val referanse = Utbetalingsreferanse(versjon2.utbetaling.id.uuidPart())
        versjon2.kvittering(
            """{"status":"OK","detaljer":{"linjer":[{"behandlingId":"${referanse.verdi}"}]}}""",
            key = "v2-key",
        )
        every {
            versjon2.repo.finnUtbetalingerForKvitteringsreferanser(setOf(referanse), any())
        } returns listOf(UtbetalingsreferanseTreff(referanse, Utbetalingskobling(versjon2.utbetaling.id, versjon2.utbetaling.sakId)))
        every { versjon2.repo.hentOppdragsversjon(any()) } returns Oppdragsversjon.V2
        versjon2.jobb.behandle(versjon2.kvitteringId).getOrFail()
        verify(exactly = 0) { versjon2.repo.lagreKvitteringsstatus(any(), any(), any()) }
        verify {
            versjon2.innboks.markerBehandlet(
                versjon2.kvitteringId,
                any(),
                Utbetalingskobling(versjon2.utbetaling.id, versjon2.utbetaling.sakId),
                any(),
            )
        }
    }

    @Test
    fun `knyttet V3-utbetaling og kontekst bevares ved ugyldig innhold`() {
        val oppsett = Oppsett()
        oppsett.kvittering("""{"status":"UKJENT"}""")
        oppsett.jobb.behandle(oppsett.kvitteringId).leftOrNull() shouldBe
            KunneIkkeBehandleUtbetalingskvittering.UgyldigInnhold
        verify {
            oppsett.innboks.lagreFeil(
                oppsett.kvitteringId,
                "UgyldigInnhold",
                false,
                any(),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
            )
        }
        oppsett.logg.logglinjer.single().melding!!.contains(oppsett.utbetaling.enkelLoggKontekst) shouldBe true
    }

    @Test
    fun `ukjent V3-utbetaling prøves på nytt uten fallback til V2-referanser`() {
        val oppsett = Oppsett()
        val ukjentUtbetalingId = UtbetalingId.random()
        val referanse = Utbetalingsreferanse(oppsett.utbetaling.id.uuidPart())
        every { oppsett.repo.hent(any(), any()) } returns null
        oppsett.kvittering(
            """{"status":"OK","detaljer":{"linjer":[{"behandlingId":"${referanse.verdi}"}]}}""",
            key = ukjentUtbetalingId.toString(),
        )

        oppsett.jobb.behandle(oppsett.kvitteringId).leftOrNull() shouldBe
            KunneIkkeBehandleUtbetalingskvittering.FantIkkeUtbetaling(ukjentUtbetalingId)
        verify(exactly = 0) { oppsett.repo.finnUtbetalingerForKvitteringsreferanser(any(), any()) }
    }

    @Test
    fun `ferdige kvitteringer og kvitteringer i backoff behandles ikke på nytt`() {
        val ferdig = Oppsett()
        ferdig.kvittering(behandlet = true)
        ferdig.jobb.behandle(ferdig.kvitteringId).getOrFail()
        verify(exactly = 0) { ferdig.repo.hent(any(), any()) }

        val venter = Oppsett()
        venter.kvittering(historikk = Forsøkshistorikk.opprett(clock = fixedClock).inkrementer(clock = fixedClock))
        venter.jobb.behandle(venter.kvitteringId).getOrFail()
        verify(exactly = 0) { venter.repo.hent(any(), any()) }
        verify(exactly = 0) { venter.innboks.lagreFeil(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `nytt forsøk bruker lagret historikk og backoff er begrenset til ett døgn`() {
        var clock: Clock = fixedClock
        var historikk: Forsøkshistorikk? = null
        repeat(15) {
            val kvittering = RåUtbetalingskvittering(
                id = UtbetalingskvitteringId.random(),
                key = null,
                value = null,
                mottatt = nå(fixedClock),
                behandlet = null,
                sakId = null,
                utbetalingId = null,
                forsøkshistorikk = historikk,
            )
            historikk = kvittering.registrerForsøk(clock)
            historikk.antallForsøk shouldBe it + 1L
            clock = Clock.offset(clock, Duration.between(nå(clock), historikk.nesteForsøk))
        }
        Duration.between(historikk!!.forrigeForsøk, historikk.nesteForsøk) shouldBe Duration.ofHours(24)
    }

    @Test
    fun `feilkvittering avsluttes men feilet utbetaling overvåkes videre`() {
        val oppsett = Oppsett()
        oppsett.kvittering("""{"status":"FEILET","error":{"msg":"råinnhold"}}""")
        oppsett.jobb.behandle(oppsett.kvitteringId).getOrFail()
        verify { oppsett.repo.lagreKvitteringsstatus(oppsett.utbetaling.id, Utbetalingsstatus.FeiletMotOppdrag, any()) }
        verify {
            oppsett.innboks.markerBehandlet(
                oppsett.kvitteringId,
                any(),
                Utbetalingskobling(oppsett.utbetaling.id, oppsett.utbetaling.sakId),
                any(),
            )
        }
        oppsett.logg.logglinjer.single().melding!!.contains("råinnhold") shouldBe false
    }

    @Test
    fun `vedvarende avvik mater eksisterende alarmer også etter omstart og uten nye kvitteringer`() {
        val oppsett = Oppsett()
        every { oppsett.repo.antallFeiledeUtbetalingerPåVersjon3() } returns 1
        every { oppsett.innboks.antallPermanenteFeil() } returns 2
        every { oppsett.repo.antallUtbetalingerUtenOkPåVersjon3(nå(fixedClock).minusDays(3)) } returns 3
        every { oppsett.innboks.antallForbigåendeFeil(nå(fixedClock).minusDays(3)) } returns 4
        repeat(2) { oppsett.jobb.følgOppUtbetalinger() }
        oppsett.målinger.feilet.count() shouldBe 6.0
        oppsett.målinger.ikkeOk.count() shouldBe 14.0
        oppsett.logg.logglinjer.size shouldBe 2

        val omstart = BehandleUtbetalingskvitteringerJobb(
            oppsett.innboks,
            oppsett.repo,
            ::tilUtbetalingsstatus,
            fixedClock,
            TestSessionFactory(),
            oppsett.målinger,
            oppsett.logg,
            oppsett.sikkerlogg,
        )
        omstart.følgOppUtbetalinger()
        oppsett.målinger.feilet.count() shouldBe 9.0
        every { oppsett.repo.antallFeiledeUtbetalingerPåVersjon3() } returns 0
        every { oppsett.innboks.antallPermanenteFeil() } returns 0
        every { oppsett.repo.antallUtbetalingerUtenOkPåVersjon3(any()) } returns 0
        every { oppsett.innboks.antallForbigåendeFeil(any()) } returns 0
        omstart.følgOppUtbetalinger()
        oppsett.målinger.feilet.count() shouldBe 9.0
        oppsett.målinger.ikkeOk.count() shouldBe 21.0
    }
}
