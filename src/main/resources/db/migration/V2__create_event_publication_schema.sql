create table if not exists event_publication (
    id uuid not null primary key,
    listener_id varchar(512) not null,
    event_type varchar(512) not null,
    serialized_event text not null,
    publication_date timestamp with time zone not null,
    completion_date timestamp with time zone,
    status varchar(20),
    completion_attempts integer default 0,
    last_resubmission_date timestamp with time zone
);

create index if not exists ix_event_publication_completion_date
    on event_publication (completion_date);

create index if not exists ix_event_publication_listener_serialized
    on event_publication (listener_id, serialized_event);
