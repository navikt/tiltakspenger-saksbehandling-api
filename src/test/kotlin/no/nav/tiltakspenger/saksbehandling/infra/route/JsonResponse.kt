package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.assertions.json.shouldContainJsonKeyValue

infix fun String.harKode(forventetKode: String) {
    this.shouldContainJsonKeyValue("kode", forventetKode)
}
