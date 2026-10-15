package no.nav.tiltakspenger.saksbehandling.journalnotat

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Et internt notat om et vedtak, som journalføres som NOTAT i Joark når saksbehandler har valgt det.
 * Notatet er kun synlig internt i Nav (Gosys), og sendes ikke til bruker.
 * Innholdet er begrunnelsene saksbehandler har valgt å journalføre, sammen med hvem som behandlet og besluttet vedtaket.
 * Vedtakstype og periode står i vedtaksbrevet, som også er journalført, og gjentas derfor ikke i notatet.
 *
 * @param notatsdato Datoen notatet ble generert, som kan være senere enn vedtaksdatoen.
 * @param begrunnelser Vises i notatet i rekkefølgen de har i lista.
 *  Typen avgjør overskriften i PDF-en.
 */
data class Journalnotat(
    val vedtakId: VedtakId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val saksbehandler: String,
    val beslutter: String,
    val notatsdato: LocalDate,
    val begrunnelser: NonEmptyList<Notatbegrunnelse>,
) {
    data class Notatbegrunnelse(
        val type: Begrunnelsestype,
        val begrunnelse: Begrunnelse,
    )

    enum class Begrunnelsestype {
        VILKÅRSVURDERING,
        BARNETILLEGG,
        MELDEKORT,
    }
}

/**
 * Forutsetter at saksbehandler har valgt å journalføre minst én av begrunnelsene.
 * Begrunnelsene som er valgt, er da påkrevd, siden behandlingen ellers ikke kunne blitt sendt til beslutning.
 * Begrunnelsen for vilkårsvurderingen kommer før begrunnelsen for barnetillegg.
 */
fun Rammevedtak.tilJournalnotat(notatsdato: LocalDate): Journalnotat {
    require(skalJournalføreNotat.skalJournalføres) {
        "Saksbehandler har ikke valgt å journalføre notat for rammevedtaket. sakId: $sakId, saksnummer: $saksnummer, vedtakId: $id"
    }
    val begrunnelser = listOfNotNull(
        if (skalJournalføreNotat.vilkårsvurdering) {
            Journalnotat.Notatbegrunnelse(
                type = Journalnotat.Begrunnelsestype.VILKÅRSVURDERING,
                begrunnelse = requireNotNull(rammebehandling.begrunnelseVilkårsvurdering) {
                    "Rammevedtaket mangler begrunnelse for vilkårsvurderingen, men den skal journalføres. sakId: $sakId, saksnummer: $saksnummer, vedtakId: $id"
                },
            )
        } else {
            null
        },
        if (skalJournalføreNotat.barnetillegg) {
            Journalnotat.Notatbegrunnelse(
                type = Journalnotat.Begrunnelsestype.BARNETILLEGG,
                begrunnelse = requireNotNull(rammebehandling.barnetillegg?.begrunnelse) {
                    "Rammevedtaket mangler begrunnelse for barnetillegg, men den skal journalføres. sakId: $sakId, saksnummer: $saksnummer, vedtakId: $id"
                },
            )
        } else {
            null
        },
    ).toNonEmptyListOrNull()!!
    return Journalnotat(
        vedtakId = id,
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        notatsdato = notatsdato,
        begrunnelser = begrunnelser,
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
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        notatsdato = notatsdato,
        begrunnelser = nonEmptyListOf(
            Journalnotat.Notatbegrunnelse(
                type = Journalnotat.Begrunnelsestype.MELDEKORT,
                begrunnelse = requireNotNull(meldekortbehandling.begrunnelse) {
                    "Meldekortvedtaket mangler begrunnelse, men notatet skal journalføres. sakId: $sakId, saksnummer: $saksnummer, vedtakId: $id"
                },
            ),
        ),
    )
}
