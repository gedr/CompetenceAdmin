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

insert into Minos.ProfilePattern (id, name, descr, status, journal_id, postMask, filialMask, item, timePoint, ver, ancestor, catalog_id)
select id, SUBSTRING(name, 1, 249), null, 0, 1, 0,3, id, '7777-07-07 00:00:00.000', 1, null, 1 from @patern

insert into Minos.ProfilePatternElement (id, competence_id, minLevel_id, journal_id, profilePattern_id, item, status, ver, ancestor)
select mp.id, mp.competence_id, mp.minLevel_id, 1, pp.id, mp.item, 0, 1, null from MinosProfile mp
	inner join @patern pp on pp.d = mp.division_id and pp.p = mp.position_id


select * from @patern

-- truncate table Minos.ProfilePattern
-- truncate table Minos.ProfilePatternElement


--select * from tStatDolSP where tStatDolSPId = 50000327