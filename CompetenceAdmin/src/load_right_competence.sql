select ap.id, ppe.minLevel_id, ppe.competence_id, c.id  from gedr2.minos.ActorsPerformance ap
	inner join gedr2.minos.ProfilePatternElement ppe on ppe.id = ap.ppe_id
	inner join gedr2.minos.Competence c on ( c.id = ppe.competence_id or ISNULL(c.ancestor_id, 0) = ppe.competence_id ) and c.ver = ap.cver
where ap.actors_id = 1
	


/*==============================================================*/
/* Table: Logger                                                */
/*==============================================================*/
create table Minos.Logger (
   id                   bigint               identity(1, 1),
   moment               datetime             not null default getdate(),
   external_id          bigint               not null,
   operationCode        tinyint              not null DEFAULT 0,
   tableCode            int		             not null DEFAULT 0,
   summary              varchar(Max)         null default null,
   constraint PK_LOGGER primary key (id)
)
go

/*==============================================================*/
/* Index: IndexByMoment                                         */
/*==============================================================*/
create index IndexByMoment on Minos.Logger (
moment ASC
)
go

/*==============================================================*/
/* Index: IndexByOperation                                      */
/*==============================================================*/
create index IndexByOperation on Minos.Logger (
operationCode ASC
)
go

/*==============================================================*/
/* Index: IndexByTable                                          */
/*==============================================================*/
create index IndexByTable on Minos.Logger (
tableCode ASC
)
go
