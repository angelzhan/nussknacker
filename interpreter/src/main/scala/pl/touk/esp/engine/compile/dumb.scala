package pl.touk.esp.engine.compile

import pl.touk.esp.engine.api.{CustomStreamTransformer, FoldingFunction, MetaData}
import pl.touk.esp.engine.definition.{CustomNodeInvokerDeps, CustomNodeInvoker, ProcessObjectFactory, ServiceInvoker}
import pl.touk.esp.engine.graph.param.Parameter

import scala.concurrent.ExecutionContext

object dumb {

  object DumbServiceInvoker extends ServiceInvoker {

    override def invoke(params: Map[String, Any])
                       (implicit ec: ExecutionContext) = throw new IllegalAccessException("Dumb service shouldn't be invoked")

  }

  class DumbProcessObjectFactory[T] extends ProcessObjectFactory[T] {
    override def create(processMetaData: MetaData, params: List[Parameter]) =
      null.asInstanceOf[T]
  }

  class DumbCustomNodeInvoker[T] extends CustomNodeInvoker[T] {
    override def run(lazyDeps: () => CustomNodeInvokerDeps) = null.asInstanceOf[T]
  }

  object DumbFoldingFunction extends FoldingFunction[Any] {

    override def fold(value: AnyRef, acc: Option[Any]) = throw new IllegalAccessException("Dumb folding function shouldn't be invoked")

  }

  object DumbCustomStreamTransformer extends CustomStreamTransformer

}
