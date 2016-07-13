package pl.touk.process.model

import org.slf4j.LoggerFactory
import pl.touk.process.model.model._

object evaluator {

  private val logger = LoggerFactory.getLogger("monitor")

  def evaluate(node: Node, ctx: Ctx): Ctx = {
    ctx.log(s"Processing node ${node.metaData.id}")
    node match {
      case StartNode(_, next) => evaluate(next, ctx)
      case Processor(_, ref, next) => invoke(ref, ctx); evaluate(next, ctx)
      case Enricher(_, ref, output, next) => val out = invoke(ref, ctx); evaluate(next, ctx.copy(data = ctx.data + (output -> out)))
      case Filter(_, expression, next) => val isOk = expression.evaluate(ctx).asInstanceOf[Boolean]; if (isOk) evaluate(next, ctx) else ctx
      case Switch(_, expression, exprVal, nexts) => val output = expression.evaluate(ctx)
        val newCtx = ctx.copy(data = ctx.data + (exprVal -> output))
        nexts.view.find {
          case (expr, _) => expr.evaluate(newCtx).asInstanceOf[Boolean]
        }.map(e => evaluate(e._2, ctx)).getOrElse(ctx)
      case End(_) => ctx
      case _ => ctx

    }

  }

  private def invoke(ref: ProcessorRef, ctx: Ctx): Any = {
    val preparedCtx = ref.parameters.list.map {
      case (name, expr) => val out = expr.evaluate(ctx); name -> out
    }.toMap
    ctx.services(ref.id).invoke(preparedCtx, ctx)
  }

  trait Service {
    def invoke(params: Map[String, Any], ctx: Ctx): Any
  }

  case class Ctx(globals: Map[String, Any], data: Map[String, Any], services: Map[String, Service]) {
    def apply[T](name: String) : T = data.get(name).orElse(globals.get(name))
      .getOrElse(throw new RuntimeException(s"Unknown variable $name")).asInstanceOf[T]

    def log(message: String, args: String*) =
      logger.info(message, args)

  }


}
