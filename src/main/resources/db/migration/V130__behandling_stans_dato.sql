 ALTER TABLE behandling
 ADD COLUMN har_valgt_stans_fra_første_dag_som_gir_rett BOOLEAN default false;

 ALTER TABLE behandling
 ADD COLUMN har_valgt_stans_til_siste_dag_som_gir_rett BOOLEAN default true;