create table ids
(
    cik          varchar(10) not null,
    ticker       varchar     not null,
    company_name varchar,
    primary key (cik)
);

create index idx_ids_ticker on ids (ticker);
