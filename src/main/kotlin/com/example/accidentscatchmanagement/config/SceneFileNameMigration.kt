package com.example.accidentscatchmanagement.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Copies old location file names once, without overwriting existing scene file names. */
@Component
@Order(0)
class SceneFileNameMigration(private val jdbc: JdbcTemplate) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        jdbc.execute("""create table if not exists roadwatch_schema_migration
            (version varchar(100) primary key)""")
        val version = "scene-file-name-v1"
        if (jdbc.queryForObject("select count(*) from roadwatch_schema_migration where version = ?",
                Long::class.java, version)!! > 0) return
        val columns = jdbc.query("select * from location where 1 = 0",
            org.springframework.jdbc.core.ResultSetExtractor { rs ->
                (1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it).lowercase() }.toSet()
            }).orEmpty()
        if ("file_name" in columns) {
            jdbc.update("""
                update accident_scene set file_name = (
                    select nullif(trim(l.file_name), '') from location l
                    where l.id = accident_scene.location_id
                )
                where nullif(trim(file_name), '') is null
                and exists (select 1 from location l where l.id = accident_scene.location_id
                            and nullif(trim(l.file_name), '') is not null)
            """)
        }
        // Leave the retired location column as a backup. A marker prevents restoring cleared names.
        jdbc.update("insert into roadwatch_schema_migration (version) values (?)", version)
    }
}
