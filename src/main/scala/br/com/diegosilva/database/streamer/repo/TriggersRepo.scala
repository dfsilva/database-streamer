package br.com.diegosilva.database.streamer.repo

import java.sql.Connection
import scala.concurrent.{ExecutionContext, Future}

object TriggersRepo {

  def createFunction(table: String, topic: String, connection: Connection)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      connection.createStatement().execute(getFunctionTemplate(table, topic))
    }
  }

  def createTrigger(schema:String, table: String, connection: Connection)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      connection.createStatement().execute(getTriggerTemplate(schema, table))
    }
  }

  def deleteFunction(table: String, connection: Connection)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      connection.createStatement().execute(getDropFunction(table))
    }
  }

  def deleteTrigger(schema: String, table: String, connection: Connection)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      connection.createStatement().execute(getDropTrigger(schema, table))
    }
  }


  def getFunctionTemplate(table: String, topic: String): String = {
    s"""
       |CREATE OR REPLACE FUNCTION database_streamer.notify_$table()
       |    RETURNS trigger
       |    LANGUAGE plpgsql
       |AS $$function$$
       |DECLARE
       |    eventsRow database_streamer.events%ROWTYPE;
       |    content text;
       |    contentSize int;
       |begin
       |    SELECT nextval('database_streamer.sq_events'::regclass),now(),'$topic','',row_to_json(new) INTO eventsRow;
       |    INSERT INTO database_streamer.events(id,create_time,topic,old_data,new_data) values (eventsRow.*);
       |    content := row_to_json(eventsRow)::text;
       |    contentSize := octet_length(content);
       |    if contentSize <= 8000 then
       |        PERFORM pg_notify('events_notify', content);
       |    end if;
       |    RETURN new;
       |END;
       |$$function$$;
       |""".stripMargin
  }

  def getTriggerTemplate(schema:String, table: String): String = {
    s"""
       |create trigger ${table}_changes after
       |    INSERT or UPDATE
       |    on
       |        ${schema}.${table} for each row execute procedure database_streamer.notify_$table();
       |""".stripMargin
  }

  def getDropFunction(table: String): String = {
    s"drop function database_streamer.notify_$table();"
  }

  def getDropTrigger(schema: String, table: String): String = {
    s"drop trigger ${table}_changes on ${schema}.$table;"
  }
}
