package pl.touk.nussknacker.ui.process.repository

import cats.Monad
import pl.touk.nussknacker.engine.api.process.ProcessName
import pl.touk.nussknacker.engine.api.process.ProcessId
import pl.touk.nussknacker.restmodel.processdetails.{BaseProcessDetails, ProcessAction, ProcessShapeFetchStrategy}
import pl.touk.nussknacker.ui.EspError.XError
import pl.touk.nussknacker.ui.db.entity.{ProcessEntityData, ProcessVersionEntityData}
import pl.touk.nussknacker.ui.process.repository.ProcessDBQueryRepository.ProcessNotFoundError
import pl.touk.nussknacker.ui.security.api.LoggedUser

import scala.concurrent.ExecutionContext
import scala.language.higherKinds
import cats.syntax.functor._
import pl.touk.nussknacker.restmodel.process.ProcessingType

abstract class FetchingProcessRepository[F[_]: Monad] extends ProcessDBQueryRepository[F] {

  def fetchLatestProcessDetailsForProcessId[PS: ProcessShapeFetchStrategy](id: ProcessId)
                                                                          (implicit loggedUser: LoggedUser, ec: ExecutionContext): F[Option[BaseProcessDetails[PS]]]

  def fetchLatestProcessDetailsForProcessIdEither[PS: ProcessShapeFetchStrategy](id: ProcessId)
                                                                                (implicit loggedUser: LoggedUser, ec: ExecutionContext): F[XError[BaseProcessDetails[PS]]] = {
    fetchLatestProcessDetailsForProcessId(id).map[XError[BaseProcessDetails[PS]]] {
      case None => Left(ProcessNotFoundError(id.value.toString))
      case Some(p) => Right(p)
    }
  }

  def fetchProcessDetailsForId[PS: ProcessShapeFetchStrategy](processId: ProcessId, versionId: Long)
                                                             (implicit loggedUser: LoggedUser, ec: ExecutionContext): F[Option[BaseProcessDetails[PS]]]

  def fetchLatestProcessVersion[PS: ProcessShapeFetchStrategy](processId: ProcessId)(implicit loggedUser: LoggedUser): F[Option[ProcessVersionEntityData]]

  def fetchProcesses[PS: ProcessShapeFetchStrategy]()(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchProcesses[PS: ProcessShapeFetchStrategy](isSubprocess: Option[Boolean],
                                                    isArchived: Option[Boolean],
                                                    isDeployed: Option[Boolean],
                                                    categories: Option[Seq[String]],
                                                    processingTypes: Option[Seq[String]])
                                                   (implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchCustomProcesses[PS: ProcessShapeFetchStrategy]()(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchProcessesDetails[PS: ProcessShapeFetchStrategy]()(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchDeployedProcessesDetails[PS: ProcessShapeFetchStrategy]()(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchProcessesDetails[PS: ProcessShapeFetchStrategy](processNames: List[ProcessName])(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchSubProcessesDetails[PS: ProcessShapeFetchStrategy]()(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchAllProcessesDetails[PS: ProcessShapeFetchStrategy]()(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchArchivedProcesses[PS: ProcessShapeFetchStrategy]()(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[List[BaseProcessDetails[PS]]]

  def fetchProcessId(processName: ProcessName)(implicit ec: ExecutionContext): F[Option[ProcessId]]

  def fetchProcessName(processId: ProcessId)(implicit ec: ExecutionContext): F[Option[ProcessName]]

  def fetchProcessActions(processId: ProcessId)(implicit ec: ExecutionContext): F[List[ProcessAction]]

  //TODO: It should return F[Option[ProcessingType]]
  def fetchProcessingType(processId: ProcessId)(implicit loggedUser: LoggedUser, ec: ExecutionContext): F[ProcessingType]

  def fetchProcessDetails(processName: ProcessName)(implicit ec: ExecutionContext): F[Option[ProcessEntityData]]

}
