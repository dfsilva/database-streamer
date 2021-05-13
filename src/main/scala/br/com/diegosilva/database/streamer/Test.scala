package br.com.diegosilva.database.streamer

object Test extends App {

  val str = "table mandamento.fcm_tokens: UPDATE: user_id[character varying]:'yc2mjGpZqpbPLvpEMUEprVSTzTy1' token[character varying]:'fFxF26ALSOiv4_cg1QGZ8S:APA91bG_NVY_bPEnISAJYm5-th2s2AIS0PyMVTIiwwQSX_g_ZVJLL919mpMN0hVEpN_tQL3zLTfUUrWV80sYMci8qd4S0v_zlYH6lQl93nsxWVPCU8EmWToU676tU-An5T0pDmQ-cLuT' device[character varying]:'MI 6' updated[timestamp without time zone]:'2021-02-01 11:48:49.918'"

  val splited = str.split(":")

  splited.foreach(println)

}
