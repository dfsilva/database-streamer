package br.com.diegosilva.database.streamer.repo

import java.sql.Connection
import scala.concurrent.{ExecutionContext, Future}

object TriggersRepo {

  def create(schema: String, table: String, topic: String, delete: Boolean, insert: Boolean, update: Boolean, connection: Connection)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      try {
        connection.createStatement().execute(getFunctionTemplate(table, topic))
        connection.createStatement().execute(getTriggerTemplate(schema, table, topic, delete, insert, update))
      } finally {
        connection.close()
      }
    }
  }


  def delete(schema: String, topic: String, table: String, connection: Connection)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      try{
        connection.createStatement().execute(getDropTrigger(schema, topic, table))
        connection.createStatement().execute(getDropFunction(topic, table))
      }finally {
        connection.close()
      }
    }
  }

  def getFunctionTemplate(table: String, topic: String): String = {
    s"""
       |CREATE OR REPLACE FUNCTION database_streamer.notify_${topic}_$table()
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

  //       |    content := row_to_json(eventsRow)::text;
  //       |    contentSize := octet_length(content);
  //       |    if contentSize <= 8000 then
  //       |        PERFORM pg_notify('events_notify', content);
  //       |    end if;

  def getTriggerTemplate(schema: String, table: String, topic: String, delete: Boolean, insert: Boolean, update: Boolean): String = {

    val build = new StringBuilder("")

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
       |create CONSTRAINT trigger ${topic}_${table} after
       |    ${build.toString()}
       |    on
       |        ${schema}.${table} DEFERRABLE INITIALLY DEFERRED for each row execute procedure database_streamer.notify_${topic}_$table();
       |""".stripMargin
  }

  def getDropFunction(topic: String, table: String): String = {
    s"drop function database_streamer.notify_${topic}_$table();"
  }

  def getDropTrigger(schema: String, topic: String, table: String): String = {
    s"drop trigger ${topic}_${table} on ${schema}.$table;"
  }
}
