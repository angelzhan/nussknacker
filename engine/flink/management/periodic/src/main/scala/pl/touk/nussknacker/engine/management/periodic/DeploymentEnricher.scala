package pl.touk.nussknacker.engine.management.periodic

import scala.concurrent.Future

case class DeploymentEnricherInputData(processJson: String, modelConfig: String)

case class EnrichedDeployment(modelConfig: String)

trait DeploymentEnricher {
  def onInitialSchedule(data: DeploymentEnricherInputData): Future[EnrichedDeployment]
  def onDeploy(data: DeploymentEnricherInputData): Future[EnrichedDeployment]
}



object DeploymentEnricher {
  def noOp: DeploymentEnricher = new DeploymentEnricher {
    override def onInitialSchedule(data: DeploymentEnricherInputData): Future[EnrichedDeployment] = {
      Future.successful(EnrichedDeployment(data.modelConfig))
    }

    override def onDeploy(data: DeploymentEnricherInputData): Future[EnrichedDeployment] = {
      Future.successful(EnrichedDeployment(data.modelConfig))
    }
  }
}
