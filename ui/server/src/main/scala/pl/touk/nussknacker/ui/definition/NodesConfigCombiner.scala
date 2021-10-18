package pl.touk.nussknacker.ui.definition

import pl.touk.nussknacker.engine.api.process.SingleNodeConfig

object NodesConfigCombiner {
  import cats.instances.map._
  import cats.syntax.semigroup._

  def combine(configs: Map[String, SingleNodeConfig]*): Map[String, SingleNodeConfig] = {
    configs.reduce(_ |+| _)
  }
}
