create table shrs
(
    ticker             varchar,
    m_ticker           varchar,
    comp_name          varchar,
    fye                int,
    per_end_date       date,
    per_type           char,
    active_ticker_flag varchar,
    shares_out         numeric,
    avg_d_shares       numeric,
    primary key (m_ticker, per_end_date)
);
