package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Vedtakstype
import java.time.LocalDate
import java.time.LocalDateTime

interface RammevedtakMother {
    fun nyRammevedtak(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(),
        sakId: SakId = SakId.random(),
        periode: Periode = ObjectMother.virkningsperiode(),
        fnr: Fnr = Fnr.random(),
        behandling: Behandling = ObjectMother.nyVedtattBehandling(
            sakId = sakId,
            virkningsperiode = periode,
            fnr = fnr,
        ),
        vedtaksdato: LocalDate = 2.januar(2023),
        vedtaksType: Vedtakstype = Vedtakstype.INNVILGELSE,
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        distribusjonId: DistribusjonId? = null,
        distribusjonstidspunkt: LocalDateTime? = null,
        sendtTilDatadeling: LocalDateTime? = null,
        brevJson: String? = null,
    ) = Rammevedtak(
        id = id,
        opprettet = opprettet,
        sakId = sakId,
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        vedtaksType = vedtaksType,
        periode = periode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
    )

    fun nyRammevedtakInnvilgelse(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        periode: Periode = ObjectMother.virkningsperiode(),
        behandling: Behandling = ObjectMother.nyVedtattBehandling(
            sakId = sakId,
            virkningsperiode = periode,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
            fnr = fnr,
        ),
        vedtaksdato: LocalDate = 2.januar(2023),
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        distribusjonId: DistribusjonId? = null,
        distribusjonstidspunkt: LocalDateTime? = null,
        sendtTilDatadeling: LocalDateTime? = null,
        brevJson: String? = null,
    ): Rammevedtak = nyRammevedtak(
        id = id,
        opprettet = opprettet,
        sakId = sakId,
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        vedtaksType = Vedtakstype.INNVILGELSE,
        periode = periode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
    )

    fun nyRammevedtakStans(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        periode: Periode = ObjectMother.virkningsperiode(),
        behandling: Behandling = ObjectMother.nyVedtattBehandling(
            sakId = sakId,
            virkningsperiode = periode,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
            fnr = fnr,
            behandlingstype = Behandlingstype.REVURDERING,
            søknad = null,
        ),
        vedtaksdato: LocalDate = 2.januar(2023),
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        distribusjonId: DistribusjonId? = null,
        distribusjonstidspunkt: LocalDateTime? = null,
        sendtTilDatadeling: LocalDateTime? = null,
        brevJson: String? = null,
    ): Rammevedtak = nyRammevedtak(
        id = id,
        opprettet = opprettet,
        sakId = sakId,
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        vedtaksType = Vedtakstype.STANS,
        periode = periode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
    )
}
