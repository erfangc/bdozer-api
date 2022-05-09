create table mktv
(
    ticker             varchar,
    m_ticker           varchar,
    comp_name          varchar,
    fye                int,
    per_end_date       date,
    per_type           char,
    active_ticker_flag varchar,
    mkt_val            numeric,
    ep_val             numeric,
    primary key (m_ticker, per_end_date)
);