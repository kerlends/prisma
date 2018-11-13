package com.prisma.native

import java.sql.DriverManager
import java.io.PrintWriter

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.api.server.ApiServer
import com.prisma.deploy.server.ManagementServer
import com.prisma.image.Version
import com.prisma.websocket.WebsocketServer
import com.prisma.workers.WorkerServer

object PrismaNativeMain {
  System.setProperty("org.jooq.no-logo", "true")

  def main(args: Array[String]): Unit = {
    implicit val system       = ActorSystem("single-server", StaticAkkaConfig.config)
    implicit val materializer = ActorMaterializer()
    implicit val dependencies = PrismaNativeDependencies()

    DriverManager.setLogWriter(new PrintWriter(System.out))

    dependencies.initialize()(system.dispatcher)
    dependencies.migrator.initialize

    Version.check()

    ServerExecutor(
      port = dependencies.config.port.getOrElse(4466),
      ManagementServer("management"),
      WebsocketServer(dependencies),
      ApiServer(dependencies.apiSchemaBuilder),
      WorkerServer(dependencies)
    ).startBlocking()
  }
}