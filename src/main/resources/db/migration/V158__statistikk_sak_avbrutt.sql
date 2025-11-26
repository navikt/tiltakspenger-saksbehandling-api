update statistikk_sak
set behandlingresultat='AVBRUTT'
where behandlingstatus = 'AVSLUTTET'
  and behandlingresultat is null;