package no.nav.tiltakspenger.saksbehandling.journalnotat

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Et internt notat om et vedtak, som journalføres som NOTAT i Joark når saksbehandler har valgt det.
 * Notatet er kun synlig internt i Nav (Gosys), og sendes ikke til bruker.
 * Innholdet er saksbehandlers begrunnelse, sammen med hvem som behandlet og besluttet vedtaket.
 * Vedtakstype og periode står i vedtaksbrevet, som også er journalført, og gjentas derfor ikke i notatet.
 *
 * @param vedtakstype Styrer tittelen på begrunnelsen i notatet.
 * @param notatsdato Datoen notatet ble generert, som kan være senere enn vedtaksdatoen.
 */
data class Journalnotat(
    val vedtakId: VedtakId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val vedtakstype: Vedtakstype,
    val saksbehandler: String,
    val beslutter: String,
    val notatsdato: LocalDate,
    val begrunnelse: Begrunnelse,
) {
    enum class Vedtakstype {
        SØKNAD_INNVILGELSE,
        SØKNAD_AVSLAG,
        REVURDERING_INNVILGELSE,
        STANS,
        OMGJØRING_INNVILGELSE,
        OMGJØRING_OPPHØR,
        MELDEKORT,
    }
}

/**
 * Forutsetter at saksbehandler har valgt å journalføre notatet.
 * Begrunnelsen for vilkårsvurderingen er da påkrevd, siden behandlingen ellers ikke kunne blitt sendt til beslutning.
 */
fun Rammevedtak.tilJournalnotat(notatsdato: LocalDate): Journalnotat {
    require(skalJournalføreNotat) {
        "Saksbehandler har ikke valgt å journalføre notat for rammevedtaket. sakId: $sakId, saksnummer: $saksnummer, vedtakId: $id"
    }
    return Journalnotat(
        vedtakId = id,
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        vedtakstype = rammebehandlingsresultat.tilVedtakstype(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        notatsdato = notatsdato,
        begrunnelse = requireNotNull(rammebehandling.begrunnelseVilkårsvurdering) {
            "Rammevedtaket mangler begrunnelse for vilkårsvurderingen, men notatet skal journalføres. sakId: $sakId, saksnummer: $saksnummer, vedtakId: $id"
        },
    )
}

/**
 * Forutsetter at saksbehandler har valgt å journalføre notatet.
 * Begrunnelsen er da påkrevd, siden behandlingen ellers ikke kunne blitt sendt til beslutning.
 */
fun Meldekortvedtak.tilJournalnotat(notatsdato: LocalDate): Journalnotat {
    require(skalJournalføreNotat) {
        "Saksbehandler har ikke valgt å journalføre notat for meldekortvedtaket. sakId: $sakId, saksnummer: $saksnummer, vedtakId: $id"
    }
    return Journalnotat(
        vedtakId = id,
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        vedtakstype = Journalnotat.Vedtakstype.MELDEKORT,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        notatsdato = notatsdato,
        begrunnelse = requireNotNull(meldekortbehandling.begrunnelse) {
            "Meldekortvedtaket mangler begrunnelse, men notatet skal journalføres. sakId: $sakId, saksnummer: $saksnummer, vedtakId: $id"
        },
    )
}

private fun Rammebehandlingsresultat.tilVedtakstype(): Journalnotat.Vedtakstype = when (this) {
    is Søknadsbehandlingsresultat.Innvilgelse -> Journalnotat.Vedtakstype.SØKNAD_INNVILGELSE
    is Søknadsbehandlingsresultat.Avslag -> Journalnotat.Vedtakstype.SØKNAD_AVSLAG
    is Revurderingsresultat.Innvilgelse -> Journalnotat.Vedtakstype.REVURDERING_INNVILGELSE
    is Revurderingsresultat.Stans -> Journalnotat.Vedtakstype.STANS
    is Omgjøringsresultat.OmgjøringInnvilgelse -> Journalnotat.Vedtakstype.OMGJØRING_INNVILGELSE
    is Omgjøringsresultat.OmgjøringOpphør -> Journalnotat.Vedtakstype.OMGJØRING_OPPHØR
    is Omgjøringsresultat.OmgjøringIkkeValgt -> throw IllegalStateException("Et rammevedtak kan ikke ha resultatet OmgjøringIkkeValgt")
}
