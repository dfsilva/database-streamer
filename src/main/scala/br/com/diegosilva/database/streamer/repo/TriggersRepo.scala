package br.com.diegosilva.database.streamer.repo

import br.com.diegosilva.database.streamer.repo.PostgresProfile.api._
import org.slf4j.LoggerFactory
import slick.dbio.DBIO

object TriggersRepo {

  private val log = LoggerFactory.getLogger(TriggersRepo.getClass)


  def createFunction(table: String, topic: String): DBIO[Int] = {
    sqlu"#${functionCreateTemplate(table, topic)}"
  }

  def createTrigger(schema: String, table: String, topic: String, delete: Boolean, insert: Boolean, update: Boolean): DBIO[Int] = {
    sqlu"#${triggerCreateTemplate(schema, table, topic, delete, insert, update)}"
  }

  def dropTrigger(schema: String, topic: String, table: String): DBIO[Int] = {
    sqlu"#${dropTriggerTemplate(schema, topic, table)}"
  }

  def dropFunction(topic: String, table: String): DBIO[Int] = {
    sqlu"#${dropFunctionTemplate(topic, table)}"
  }

  private def functionCreateTemplate(table: String, topic: String): String = {
    val functionName = s"${topic}_$table"
    s"""
       |CREATE OR REPLACE FUNCTION database_streamer.notify_$functionName()
       |    RETURNS trigger
       |    LANGUAGE plpgsql
       |AS $$function$$
       |DECLARE
       |    eventsRow database_streamer.events%ROWTYPE;
       |    content text;
       |    contentSize int;
       |begin
       |    SELECT nextval('database_streamer.sq_events'::regclass),now(),'$topic',row_to_json(old),row_to_json(new) INTO eventsRow;
       |    INSERT INTO database_streamer.events(id,create_time,topic,old,current) values (eventsRow.*);
       |    RETURN new;
       |END;
       |$$function$$;
       |""".stripMargin
  }

  private def triggerCreateTemplate(schema: String, table: String, topic: String, delete: Boolean, insert: Boolean, update: Boolean): String = {

    val build = new StringBuilder("")
    val triggerName = s"${topic}_$table"

    if (insert) {
      build.addAll("INSERT")
    }

    if (update) {
      if (insert)
        build.addAll(" or UPDATE")
      else
        build.addAll(" UPDATE")
    }

    if (delete) {
      if (insert || update)
        build.addAll(" or DELETE")
      else
        build.addAll(" DELETE")
    }

    s"""
       |create CONSTRAINT trigger $triggerName after
       |    ${build.toString()}
       |    on
       |        ${schema}.${table} DEFERRABLE INITIALLY DEFERRED for each row execute procedure database_streamer.notify_$triggerName();
       |""".stripMargin
  }

  def dropFunctionTemplate(topic: String, table: String): String = {
    val name = s"${topic}_$table"
    s"drop function database_streamer.notify_$name();"
  }

  def dropTriggerTemplate(schema: String, topic: String, table: String): String = {
    val name = s"${topic}_$table"
    s"drop trigger $name on ${schema}.$table;"
  }
}
