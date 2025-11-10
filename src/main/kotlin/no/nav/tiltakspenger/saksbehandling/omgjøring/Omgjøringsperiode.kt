package no.nav.tiltakspenger.saksbehandling.omgjøring

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode

/**
 * Representerer en periode hvor et vedtak er omgjort eller omgjør et annet vedtak, helt eller delvis.
 * Et vedtak kan bare bli helt omgjort én gang.
 * Dersom et vedtak har blitt delvis omgjort flere ganger, kan ikke periodene overlappe.
 */
data class Omgjøringsperiode(
    val rammevedtakId: VedtakId,
    val periode: Periode,
    val omgjøringsgrad: Omgjøringsgrad,
)
