package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2SorteringRetning
import org.junit.jupiter.api.Test

/**
 * Pinner strengene som havner i `order by`-leddet.
 * Mappingen er den eneste veien et kolonnenavn kommer inn i sql-en, så en stille omdøping her er en ødelagt spørring.
 */
class BenkV2SorteringDbTest {

    @Test
    fun `søknadskolonner`() {
        BenkSøknaderKolonne.entries.associateWith { it.toDbString() } shouldBe mapOf(
            BenkSøknaderKolonne.FNR to "fnr",
            BenkSøknaderKolonne.SØKNADSTYPE to "søknadstype",
            BenkSøknaderKolonne.STATUS to "status",
            BenkSøknaderKolonne.KRAVTIDSPUNKT to "kravtidspunkt",
            BenkSøknaderKolonne.RESULTAT to "resultat",
            BenkSøknaderKolonne.SIST_ENDRET to "sist_endret",
            BenkSøknaderKolonne.SAKSBEHANDLER to "saksbehandler",
            BenkSøknaderKolonne.BESLUTTER to "beslutter",
            BenkSøknaderKolonne.VENTESTATUS_FRIST to "vente_frist",
        )
    }

    @Test
    fun `revurderingskolonner`() {
        BenkRevurderingerKolonne.entries.associateWith { it.toDbString() } shouldBe mapOf(
            BenkRevurderingerKolonne.FNR to "fnr",
            BenkRevurderingerKolonne.RESULTAT to "resultat",
            BenkRevurderingerKolonne.STATUS to "status",
            BenkRevurderingerKolonne.STARTET to "startet",
            BenkRevurderingerKolonne.SIST_ENDRET to "sist_endret",
            BenkRevurderingerKolonne.SAKSBEHANDLER to "saksbehandler",
            BenkRevurderingerKolonne.BESLUTTER to "beslutter",
            BenkRevurderingerKolonne.VENTESTATUS_FRIST to "vente_frist",
        )
    }

    @Test
    fun `meldekortkolonner`() {
        BenkMeldekortKolonne.entries.associateWith { it.toDbString() } shouldBe mapOf(
            BenkMeldekortKolonne.FNR to "fnr",
            BenkMeldekortKolonne.TYPE to "type",
            BenkMeldekortKolonne.PERIODE to "meldeperioder",
            BenkMeldekortKolonne.BELØP to "beløp",
            BenkMeldekortKolonne.STATUS to "status",
            BenkMeldekortKolonne.MOTTATT to "mottatt_tidspunkt",
            BenkMeldekortKolonne.SAKSBEHANDLER to "saksbehandler",
            BenkMeldekortKolonne.VENTESTATUS_FRIST to "vente_frist",
        )
    }

    @Test
    fun `klagekolonner`() {
        BenkKlageKolonne.entries.associateWith { it.toDbString() } shouldBe mapOf(
            BenkKlageKolonne.FNR to "fnr",
            BenkKlageKolonne.RESULTAT to "resultat",
            BenkKlageKolonne.STATUS to "status",
            BenkKlageKolonne.KRAVTIDSPUNKT to "kravtidspunkt",
            BenkKlageKolonne.SIST_ENDRET to "sist_endret",
            BenkKlageKolonne.SAKSBEHANDLER to "saksbehandler",
            BenkKlageKolonne.BESLUTTER to "beslutter",
            BenkKlageKolonne.VENTESTATUS_FRIST to "vente_frist",
        )
    }

    @Test
    fun `tilbakekrevingskolonner`() {
        BenkTilbakekrevingKolonne.entries.associateWith { it.toDbString() } shouldBe mapOf(
            BenkTilbakekrevingKolonne.FNR to "fnr",
            BenkTilbakekrevingKolonne.BELØP to "beløp",
            BenkTilbakekrevingKolonne.KILDE to "kilde",
            BenkTilbakekrevingKolonne.STATUS to "status",
            BenkTilbakekrevingKolonne.STARTET to "startet",
            BenkTilbakekrevingKolonne.SIST_ENDRET to "sist_endret",
            BenkTilbakekrevingKolonne.SAKSBEHANDLER to "saksbehandler",
            BenkTilbakekrevingKolonne.VENTESTATUS_FRIST to "vente_frist",
            BenkTilbakekrevingKolonne.KRAVGRUNNLAG_PERIODE to "kravgrunnlag_periode",
        )
    }

    @Test
    fun `sorteringsretning`() {
        BenkV2SorteringRetning.entries.associateWith { it.toDbString() } shouldBe mapOf(
            BenkV2SorteringRetning.ASC to "ASC",
            BenkV2SorteringRetning.DESC to "DESC",
        )
    }
}
