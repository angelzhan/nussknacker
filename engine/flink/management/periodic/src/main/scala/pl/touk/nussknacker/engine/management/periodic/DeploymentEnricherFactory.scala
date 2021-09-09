package pl.touk.nussknacker.engine.management.periodic

import com.typesafe.config.Config
import sttp.client.{NothingT, SttpBackend}

import scala.concurrent.{ExecutionContext, Future}

trait DeploymentEnricherFactory {
  def apply(config: Config)(implicit backend: SttpBackend[Future, Nothing, NothingT], ec: ExecutionContext): DeploymentEnricher
}

object DeploymentEnricherFactory {
  def noOp: DeploymentEnricherFactory = new DeploymentEnricherFactory {
    override def apply(config: Config)(implicit backend: SttpBackend[Future, Nothing, NothingT], ec: ExecutionContext): DeploymentEnricher = {
      DeploymentEnricher.noOp
    }
  }
}
