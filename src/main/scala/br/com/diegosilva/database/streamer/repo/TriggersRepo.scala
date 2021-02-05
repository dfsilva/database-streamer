package br.com.diegosilva.database.streamer.repo

import java.sql.Connection
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object TriggersRepo {

  def createTrigger(table: String, topic: String, connection: Connection)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future {
      connection.createStatement().execute(getFunctionTemplate(table, topic))
    }
  }

  def getFunctionTemplate(table: String, topic: String): String = {
    s"""
       |CREATE OR REPLACE FUNCTION database_streamer.notify_$table()
       |    RETURNS trigger
       |    LANGUAGE plpgsql
       |AS $$function$$
       |DECLARE
       |    eventsRow tb_events%ROWTYPE;
       |    content text;
       |    contentSize int;
       |begin
       |    SELECT nextval('database_streamer.sq_events'::regclass),now(),'$topic', row_to_json(new) INTO eventsRow;
       |    INSERT INTO database_streamer.tb_events(id,event_timestamp,topic,body) values (eventsRow.*);
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


}
