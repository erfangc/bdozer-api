create table zacks_model_run_results
(
    ticker            varchar,
    timestamp         timestamp,
    status            int,
    stock_analysis_id varchar,
    message           varchar,
    primary key (ticker)
);
