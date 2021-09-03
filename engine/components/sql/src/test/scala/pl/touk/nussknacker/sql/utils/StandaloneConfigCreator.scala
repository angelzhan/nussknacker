package pl.touk.nussknacker.sql.utils

import io.circe.Decoder
import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import pl.touk.nussknacker.engine.api._
import pl.touk.nussknacker.engine.api.component.ComponentProvider
import pl.touk.nussknacker.engine.api.process._
import pl.touk.nussknacker.engine.standalone.api.StandaloneSinkWithParameters
import pl.touk.nussknacker.engine.standalone.utils.JsonStandaloneSourceFactory
import pl.touk.nussknacker.engine.util.loader.ScalaServiceLoader
import pl.touk.nussknacker.engine.util.process.EmptyProcessConfigCreator
import pl.touk.nussknacker.sql.DatabaseEnricherComponentProvider
import pl.touk.nussknacker.sql.db.pool.DBPoolConfig
import pl.touk.nussknacker.sql.service.DatabaseLookupEnricher

import scala.collection.immutable

//TODO: extract to separate, standalone tests module
class StandaloneConfigCreator extends EmptyProcessConfigCreator {

  private val Category = "Test"

  override def sourceFactories(processObjectDependencies: ProcessObjectDependencies): Map[String, WithCategories[SourceFactory[_]]] = {
    Map(
      "request" -> WithCategories(new JsonStandaloneSourceFactory[StandaloneRequest], Category))
  }

  override def sinkFactories(processObjectDependencies: ProcessObjectDependencies): Map[String, WithCategories[SinkFactory]] = {
    Map(
      "response" -> WithCategories(ResponseSinkFactory, Category))
  }
}

@JsonCodec case class StandaloneRequest(id: Int)

@JsonCodec case class StandaloneResponse(name: String) extends DisplayJsonWithEncoder[StandaloneResponse]

object ResponseSinkFactory extends SinkFactory {
  override def requiresOutput: Boolean = false

  @MethodToInvoke
  def invoke(@ParamName("name") name: LazyParameter[String]): Sink = new ResponseSink(name)
}

class ResponseSink(nameParam: LazyParameter[String]) extends StandaloneSinkWithParameters {
  override def prepareResponse(implicit evaluateLazyParameter: LazyParameterInterpreter): LazyParameter[AnyRef] =
    nameParam.map(name => StandaloneResponse(name))

  override def testDataOutput: Option[Any => String] = Some({ case response: StandaloneResponse => response.asJson.spaces2 })
}
