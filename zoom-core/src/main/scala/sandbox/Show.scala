package sandbox

import magnolia.{CaseClass, Magnolia, SealedTrait}

import scala.language.experimental.macros

trait Show[Out, T] {
  def show(value: T): Out
}

trait GenericShow[Out] {
  type Typeclass[T] = Show[Out, T]

  // "simplifying" combine in the context of Show[Out,T]
  def join(typeName: String, params: Seq[(String, Out)]): Out

  // Default implementation for tuple go back to Case class
  def joinTuple(size: Int, params: Seq[Out]): Out =
    join(s"Tuple$size", params.zipWithIndex.map {
      case (o, i) ⇒
        val n = i + 1

        s"_$n" → o
    })

  def combine[T](ctx: CaseClass[Typeclass, T]): Show[Out, T] =
    value ⇒
      if (ctx.typeName.owner == "scala" && ctx.typeName.short.startsWith("Tuple")) {
        joinTuple(ctx.parameters.size, ctx.parameters.map(param ⇒ param.typeclass.show(param.dereference(value))))
      } else if (ctx.isValueClass) {
        val param = ctx.parameters.head
        param.typeclass.show(param.dereference(value))
      } else {
        val params: Seq[(String, Out)] =
          ctx.parameters.map { param ⇒
            param.label → param.typeclass.show(param.dereference(value))
          }
        join(ctx.typeName.short, params)
    }

  def dispatch[T](ctx: SealedTrait[Typeclass, T]): Show[Out, T] =
    value ⇒
      ctx.dispatch(value) { sub ⇒
        sub.typeclass.show(sub.cast(value))
    }

  implicit def gen[T]: Show[Out, T] = macro Magnolia.gen[T]
}
