
create table Minos.ProfilePatternElement (
   id                   int                  not null,
   competence_id        int                  not null,
   minLevel_id          int                  not null,
   journal_id           bigint               not null,
   profilePattern_id    int                  null,
   item                 smallint             not null,
   status               tinyint              not null,
   ver					smallint			 not null default 1,
   ancestor				int					 null default null,
   constraint PK_MINOSPROFILEPATTERNELEMENT primary key (id)
)
go


create table Minos.ProfilePattern (
   id                   int                  not null,
   name                 varchar(250)         not null,
   descr                varchar(8000)        null default null,
   catalog_id			int					 not null,
   journal_id           bigint               not null,
   postMask             tinyint              not null,
   filialMask           bigint               not null,
   item                 int                  not null,
   timePoint            datetime             not null,
   status               tinyint              not null,
   ver					smallint			 not null default 1,
   ancestor				int					 null default null,
   constraint PK_MINOSPROFILEPATTERN primary key (id)
)
go

create table Minos.PPE_SA (
   ppe_id               int                  not null,
   stringAttr_id        bigint               not null,
   constraint PK_MINOSPPE_SA primary key (ppe_id, stringAttr_id)
)
go

create table Minos.StringAttr (
   id                   bigint               not null,
   [key]                varchar(1000)        null,
   value                varchar(Max)         null,
   item                 int                  not null,
   variety              tinyint              not null,
   external_id          int                  null default null,
   journal_id           bigint               not null,
   constraint PK_MINOSSTRINGATTR primary key (id)
)



/*
select * from Minos.ProfilePattern
select * from Minos.ProfilePatternElement
select * from MinosProfile
*/
declare @patern table (
	id int identity,
	name varchar(1000) default null,
	d int,
	p int
)

insert into @patern (d, p)
select division_id, position_id from MinosProfile
group by division_id, position_id
order by division_id, position_id

update @patern set name = '[' + tOrgStru.FullName + '] - ' + tStatDolSP.FullTXT
from @patern p
	inner join tOrgStru on ( p.d = tOrgStru.tOrgStruID )
	inner join tStatDolSP on (p.p = tStatDolSP.tStatDolSPId)

insert into Minos.ProfilePattern (id, name, descr, status, journal_id, postMask, filialMask, item, timePoint)
select id, SUBSTRING(name, 1, 249), null, 0, 1, 0,3, id, '7777-07-07 00:00:00.000' from @patern

insert into Minos.ProfilePatternElement (id, competence_id, minLevel_id, journal_id, profilePattern_id, item)
select mp.id, mp.competence_id, mp.minLevel_id, 1, pp.id, mp.item from MinosProfile mp
	inner join @patern pp on pp.d = mp.division_id and pp.p = mp.position_id


select * from @patern

-- truncate table Minos.ProfilePattern
-- truncate table Minos.ProfilePatternElement


--select * from tStatDolSP where tStatDolSPId = 50000327